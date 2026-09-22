package com.chinesegames.app.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chinesegames.app.ChineseGamesApplication
import com.chinesegames.app.data.DeckRepository
import com.chinesegames.app.data.GameResult
import com.chinesegames.app.data.Word
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Что показываем в вопросе. */
enum class PromptKind { HANZI, TRANSLATION, AUDIO }

/** Что нужно выбрать в ответе. */
enum class AnswerKind { TRANSLATION, HANZI }

/** Фаза партии. */
enum class QuizPhase { LOADING, MEMORIZE, QUESTION, FINISHED }

/** Настройки одной партии викторины. */
data class QuizConfig(
    val kind: GameKind,
    val deckIds: List<Long>,
    /** Сколько слов спросить. */
    val questions: Int = 15,
    /** Сколько вариантов ответа показывать. */
    val options: Int = 4,
    /** Секунд на один вопрос (0 — без лимита). */
    val secondsPerQuestion: Int = 0,
    /** Общий лимит времени на партию (0 — без лимита). */
    val totalSeconds: Int = 0,
    val prompt: PromptKind = PromptKind.HANZI,
    val answer: AnswerKind = AnswerKind.TRANSLATION,
    /** Memory-цепочка: сколько слов показывать перед вопросами (0 — не показывать). */
    val memorizeCount: Int = 0,
    val memorizeSeconds: Int = 5,
    /** «Сначала слова с низкой точностью». */
    val srsFirst: Boolean = true
)

/** Один вопрос: слово и варианты ответа. */
data class QuizQuestion(
    val word: Word,
    val options: List<Word>,
    val correctIndex: Int
) {
    val correctWord: Word get() = options[correctIndex]
}

data class QuizUiState(
    val loading: Boolean = true,
    val notEnoughWords: Boolean = false,
    val config: QuizConfig = QuizConfig(GameKind.SPRINT, emptyList()),
    val phase: QuizPhase = QuizPhase.LOADING,
    val memorize: List<Word> = emptyList(),
    val memorizeLeft: Int = 0,
    val questions: List<QuizQuestion> = emptyList(),
    val index: Int = 0,
    val chosen: Int? = null,
    val revealed: Boolean = false,
    val score: Int = 0,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val correct: Int = 0,
    val wrong: Int = 0,
    val timeLeft: Int = 0,
    val totalLeft: Int = 0,
    val seconds: Int = 0,
    val finished: Boolean = false,
    val stars: Int = 0,
    val accuracy: Float = 1f,
    val paused: Boolean = false,
    val message: String? = null,
    val comboMessage: String? = null,
    val shakeKey: Int = 0,
    val sparkKey: Int = 0,
    val ttsAvailable: Boolean = true
) {
    val question: QuizQuestion? get() = questions.getOrNull(index)

    /** Ошибки этой партии (для экрана итогов). */
    val mistakes: Int get() = wrong

    val asked: Int get() = correct + wrong

    val progress: Float
        get() = if (questions.isEmpty()) 0f else (index + if (revealed) 1 else 0).toFloat() / questions.size

    val timeProgress: Float
        get() = when {
            config.secondsPerQuestion > 0 -> timeLeft.toFloat() / config.secondsPerQuestion
            config.totalSeconds > 0 -> totalLeft.toFloat() / config.totalSeconds
            else -> 1f
        }
}

/**
 * Движок всех «вопросных» игр: Пусзыри, Падающие слова, Спринт, Аудио-квиз
 * и Memory-цепочка. Отвечает за раздачу слов (с учётом точности), проверку
 * ответов, очки, комбо, таймеры и запись статистики.
 */
class QuizViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: DeckRepository = (app as ChineseGamesApplication).repository
    private val sounds = (app as ChineseGamesApplication).sounds
    private val speaker = (app as ChineseGamesApplication).speaker
    private val settings = (app as ChineseGamesApplication).settings

    private val _state = MutableStateFlow(QuizUiState())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var advanceJob: Job? = null
    private var speechJob: Job? = null

    /** Качество ответа по каждому слову — уходит в статистику в конце. */
    private val wordQuality = HashMap<Long, Float>()
    private val wordMistakes = HashMap<Long, Int>()
    private var playedWordIds: List<Long> = emptyList()

    /* ------------------------------ Запуск ------------------------------ */

    fun start(config: QuizConfig) {
        val current = _state.value
        if (!current.loading && current.questions.isNotEmpty() && !current.finished) return
        loadAndDeal(config, wordsOverride = null)
    }

    private fun loadAndDeal(config: QuizConfig, wordsOverride: List<Word>?) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, notEnoughWords = false) }
            val pool = repo.studyWords(config.deckIds, config.srsFirst)
            if (pool.size < MIN_WORDS) {
                _state.update {
                    it.copy(
                        loading = false,
                        notEnoughWords = true,
                        questions = emptyList(),
                        config = config
                    )
                }
                return@launch
            }
            val words = wordsOverride ?: pickWords(pool, config.questions)
            deal(config, pool, words)
        }
    }

    /** Берём нужное число слов, при нехватке — идём по кругу. */
    private fun pickWords(pool: List<Word>, count: Int): List<Word> {
        if (pool.size >= count) return pool.take(count)
        val result = ArrayList<Word>(count)
        while (result.size < count) {
            result.addAll(pool.shuffled())
        }
        return result.take(count)
    }

    private fun deal(config: QuizConfig, pool: List<Word>, words: List<Word>) {
        tickJob?.cancel()
        advanceJob?.cancel()
        wordQuality.clear()
        wordMistakes.clear()
        playedWordIds = words.map { it.id }.distinct()

        val questions = buildQuestions(config, pool, words)

        _state.value = QuizUiState(
            loading = false,
            config = config,
            phase = if (config.memorizeCount > 0 && questions.isNotEmpty()) {
                QuizPhase.MEMORIZE
            } else {
                QuizPhase.QUESTION
            },
            memorize = QuestionsHelper.chunk(questions, config.memorizeCount),
            memorizeLeft = if (config.memorizeCount > 0) config.memorizeSeconds else 0,
            questions = questions,
            timeLeft = config.secondsPerQuestion,
            totalLeft = config.totalSeconds,
            ttsAvailable = if (config.prompt == PromptKind.AUDIO) speaker.isAvailable else true
        )

        sounds.whoosh()
        startTicking()
        speakCurrentIfAudio(force = true)
    }

    private fun buildQuestions(
        config: QuizConfig,
        pool: List<Word>,
        words: List<Word>
    ): List<QuizQuestion> {
        val optionsCount = config.options.coerceIn(2, 6)
        return words.mapIndexed { index, word ->
            val candidates = when {
                // Memory-цепочка: варианты — из того же «кусочка», что показывали
                config.memorizeCount > 0 -> {
                    val start = (index / config.memorizeCount) * config.memorizeCount
                    val chunk = words.drop(start).take(config.memorizeCount)
                    val others = pool.filter { it.id != word.id && !chunk.any { c -> c.id == it.id } }
                    chunk.filter { it.id != word.id } + others.shuffled()
                }

                else -> pool.filter { it.id != word.id }.shuffled()
            }
            val options = buildOptions(word, candidates, optionsCount, config.answer)
            QuizQuestion(
                word = word,
                options = options,
                correctIndex = options.indexOfFirst { it.id == word.id }.coerceAtLeast(0)
            )
        }
    }

    /**
     * Варианты ответа: правильный + похожие по длине слова (чтобы нельзя было
     * угадать по форме), без одинаковых ответов.
     */
    private fun buildOptions(
        word: Word,
        candidates: List<Word>,
        count: Int,
        answerKind: AnswerKind
    ): List<Word> {
        val correctText = answerText(word, answerKind)
        val targetLength = correctText.length
        val usable = candidates
            .filter { candidate ->
                candidate.id != word.id &&
                    answerText(candidate, answerKind).isNotBlank() &&
                    answerText(candidate, answerKind) != correctText
            }
            .sortedBy { candidate ->
                kotlin.math.abs(answerText(candidate, answerKind).length - targetLength)
            }

        val picked = LinkedHashMap<String, Word>()
        for (candidate in usable) {
            val text = answerText(candidate, answerKind)
            if (picked.containsKey(text)) continue
            picked[text] = candidate
            if (picked.size >= count - 1) break
        }

        val options = ArrayList<Word>(picked.size + 1)
        options.add(word)
        options.addAll(picked.values)
        // Перемешиваем, чтобы правильный ответ оказался в случайной позиции.
        return options.shuffled()
    }

    private fun answerText(word: Word, kind: AnswerKind): String = when (kind) {
        AnswerKind.TRANSLATION -> word.translation.trim()
        AnswerKind.HANZI -> word.hanzi.trim()
    }

    /* ------------------------------ Таймеры ----------------------------- */

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                val s = _state.value
                if (s.paused || s.finished || s.loading) continue

                when (s.phase) {
                    QuizPhase.MEMORIZE -> {
                        val left = s.memorizeLeft - 1
                        if (left <= 0) {
                            _state.update { it.copy(phase = QuizPhase.QUESTION, memorizeLeft = 0) }
                            speakCurrentIfAudio(force = true)
                        } else {
                            _state.update { it.copy(memorizeLeft = left) }
                        }
                    }

                    QuizPhase.QUESTION -> {
                        val nextTime = if (s.config.secondsPerQuestion > 0) s.timeLeft - 1 else s.timeLeft
                        val nextTotal = if (s.config.totalSeconds > 0) s.totalLeft - 1 else s.totalLeft
                        val totalOut = s.config.totalSeconds > 0 && nextTotal <= 0
                        val questionOut = s.config.secondsPerQuestion > 0 && nextTime <= 0
                        _state.update {
                            it.copy(timeLeft = nextTime.coerceAtLeast(0), totalLeft = nextTotal.coerceAtLeast(0))
                        }
                        when {
                            totalOut -> finish()
                            questionOut && !s.revealed -> timeout()
                            else -> Unit
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    /* ------------------------------ Ответы ------------------------------ */

    fun answer(optionIndex: Int) {
        val s = _state.value
        val question = s.question ?: return
        if (s.paused || s.finished || s.revealed || s.phase != QuizPhase.QUESTION) return
        if (optionIndex !in question.options.indices) return

        val isCorrect = optionIndex == question.correctIndex
        val word = question.word

        if (isCorrect) {
            val combo = s.combo + 1
            val speedBonus = if (s.config.secondsPerQuestion > 0) {
                (s.timeLeft * 8).coerceAtMost(60)
            } else {
                15
            }
            val comboBonus = if (combo >= 2) (combo - 1) * 15 else 0
            val gained = BASE_POINTS + speedBonus + comboBonus

            if (s.config.kind == GameKind.BUBBLE) sounds.pop() else sounds.match()
            if (combo >= 2) sounds.combo(combo)
            wordQuality[word.id] = if (s.config.secondsPerQuestion > 0 &&
                s.timeLeft <= (s.config.secondsPerQuestion + 1) / 3
            ) {
                0.7f
            } else {
                1f
            }

            _state.update {
                it.copy(
                    chosen = optionIndex,
                    revealed = true,
                    correct = it.correct + 1,
                    combo = combo,
                    bestCombo = maxOf(it.bestCombo, combo),
                    score = it.score + gained,
                    message = "Верно!",
                    comboMessage = if (combo >= 2) "×$combo  +$gained" else "+$gained",
                    sparkKey = it.sparkKey + 1
                )
            }
            // После верного ответа слово произносится вслух — так оно запоминается.
            speakAnswer(word.hanzi)
            scheduleNext(if (s.config.kind == GameKind.SPRINT) 380L else 700L)
        } else {
            sounds.error()
            wordMistakes[word.id] = (wordMistakes[word.id] ?: 0) + 1
            wordQuality[word.id] = 0.15f
            _state.update {
                it.copy(
                    chosen = optionIndex,
                    revealed = true,
                    wrong = it.wrong + 1,
                    combo = 0,
                    score = (it.score - 25).coerceAtLeast(0),
                    message = "Мимо!",
                    comboMessage = null,
                    shakeKey = it.shakeKey + 1
                )
            }
            // Ошиблись — всё равно проговариваем слово: слышим, что искали.
            speakAnswer(word.hanzi)
            scheduleNext(if (s.config.kind == GameKind.SPRINT) 900L else 1_400L)
        }
    }

    /** Время вышло — считаем как ошибку и показываем правильный ответ. */
    fun timeout() {
        val s = _state.value
        if (s.revealed || s.finished) return
        val word = s.question?.word ?: return
        sounds.error()
        wordMistakes[word.id] = (wordMistakes[word.id] ?: 0) + 1
        wordQuality[word.id] = 0.1f
        speakAnswer(word.hanzi)
        _state.update {
            it.copy(
                revealed = true,
                chosen = null,
                wrong = it.wrong + 1,
                combo = 0,
                message = "Время!",
                comboMessage = null,
                shakeKey = it.shakeKey + 1
            )
        }
        scheduleNext(1_300L)
    }

    /**
     * Озвучка слова после ответа (настройка «Озвучка слов»). Небольшая пауза —
     * чтобы звук «верно/мимо» не обрезал начало слова.
     */
    private fun speakAnswer(hanzi: String) {
        if (!settings.settings.speakWords || hanzi.isBlank()) return
        speechJob?.cancel()
        speechJob = viewModelScope.launch {
            delay(320)
            if (isActive) speaker.speak(hanzi)
        }
    }

    /** «Не знаю» — та же ошибка, но без ожидания таймера. */
    fun skip() {
        val s = _state.value
        if (s.phase != QuizPhase.QUESTION || s.revealed) return
        timeout()
    }

    private fun scheduleNext(delayMillis: Long) {
        advanceJob?.cancel()
        advanceJob = viewModelScope.launch {
            delay(delayMillis)
            next()
        }
    }

    private fun next() {
        val s = _state.value
        if (s.finished) return
        val nextIndex = s.index + 1
        if (nextIndex >= s.questions.size) {
            finish()
            return
        }
        val memorizeChunk = s.config.memorizeCount > 0 && nextIndex % s.config.memorizeCount == 0
        if (memorizeChunk) {
            _state.update {
                it.copy(
                    index = nextIndex,
                    chosen = null,
                    revealed = false,
                    message = null,
                    comboMessage = null,
                    phase = QuizPhase.MEMORIZE,
                    memorize = QuestionsHelper.chunk(it.questions, it.config.memorizeCount, nextIndex),
                    memorizeLeft = it.config.memorizeSeconds,
                    timeLeft = it.config.secondsPerQuestion
                )
            }
        } else {
            _state.update {
                it.copy(
                    index = nextIndex,
                    chosen = null,
                    revealed = false,
                    message = null,
                    comboMessage = null,
                    phase = QuizPhase.QUESTION,
                    timeLeft = it.config.secondsPerQuestion
                )
            }
            speakCurrentIfAudio()
        }
    }

    /* --------------------------- Озвучка и пауза --------------------------- */

    private fun speakCurrentIfAudio(force: Boolean = false) {
        val s = _state.value
        if (s.config.prompt != PromptKind.AUDIO) return
        if (!force && s.phase != QuizPhase.QUESTION) return
        val word = s.question?.word ?: return
        speaker.speak(word.hanzi)
    }

    /** Повторить озвучку текущего слова (кнопка «ещё раз»). */
    fun repeatAudio() {
        val word = _state.value.question?.word ?: return
        if (_state.value.config.prompt == PromptKind.AUDIO) speaker.speak(word.hanzi)
    }

    fun pause() {
        _state.update { it.copy(paused = true) }
    }

    fun resume() {
        _state.update { it.copy(paused = false) }
    }

    /* ------------------------------ Финал ------------------------------- */

    private fun finish() {
        if (_state.value.finished) return
        tickJob?.cancel()
        advanceJob?.cancel()
        val s = _state.value
        val attempts = s.correct + s.wrong
        val accuracy = if (attempts == 0) 0f else s.correct.toFloat() / attempts.toFloat()
        val stars = when {
            accuracy >= 0.85f && s.correct >= 5 -> 3
            accuracy >= 0.6f -> 2
            else -> 1
        }

        sounds.win()
        _state.update {
            it.copy(
                finished = true,
                phase = QuizPhase.FINISHED,
                stars = stars,
                accuracy = accuracy,
                message = null,
                comboMessage = null
            )
        }

        viewModelScope.launch {
            repo.saveGameResult(
                GameResult(
                    game = s.config.kind.id,
                    asked = attempts,
                    correct = s.correct,
                    mistakes = s.wrong,
                    accuracy = accuracy,
                    durationSeconds = s.seconds.coerceAtLeast(playedSeconds(s)),
                    bestCombo = s.bestCombo,
                    score = s.score,
                    stars = stars,
                    deckIds = s.config.deckIds.joinToString(",")
                )
            )
            repo.registerWordResults(wordQuality, wordMistakes)
        }
    }

    private fun playedSeconds(s: QuizUiState): Int = when {
        s.config.totalSeconds > 0 -> s.config.totalSeconds - s.totalLeft
        else -> 0
    }

    /* ----------------------------- Рестарт ------------------------------ */

    fun restart(newWords: Boolean) {
        val s = _state.value
        if (s.config.deckIds.isEmpty() && playedWordIds.isEmpty()) return
        val config = s.config
        _state.value = QuizUiState(config = config, loading = true)
        if (newWords || playedWordIds.size < MIN_WORDS) {
            loadAndDeal(config, wordsOverride = null)
        } else {
            viewModelScope.launch {
                val words = repo.wordsByIds(playedWordIds)
                if (words.size < MIN_WORDS) {
                    loadAndDeal(config, wordsOverride = null)
                } else {
                    val pool = repo.wordsFor(config.deckIds).ifEmpty { words }
                    deal(config, pool, words.shuffled())
                }
            }
        }
    }

    /** Тик «секунд в партии» — нужен для отчёта о длительности. */
    fun tickSeconds() {
        _state.update { if (it.finished) it else it.copy(seconds = it.seconds + 1) }
    }

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
        advanceJob?.cancel()
        speechJob?.cancel()
    }

    companion object {
        const val MIN_WORDS = 3
        const val BASE_POINTS = 100
    }
}

/** Мелкие помощники для вопросов. */
object QuestionsHelper {
    /** Кусочек вопросов, который показываем в фазе запоминания. */
    fun chunk(questions: List<QuizQuestion>, size: Int, from: Int = 0): List<Word> {
        if (size <= 0 || questions.isEmpty()) return emptyList()
        val start = (from / size) * size
        return questions.drop(start).take(size).map { it.word }
    }
}
