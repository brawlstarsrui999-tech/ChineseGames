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

/** Слово тренажёра: иероглиф + разобранные слоги с правильными тонами. */
data class ToneTask(
    val word: Word,
    val syllables: List<ToneSyllable>
) {
    val answer: List<Int> get() = syllables.map { it.tone }
}

data class ToneUiState(
    val loading: Boolean = true,
    val notEnoughWords: Boolean = false,
    val deckIds: List<Long> = emptyList(),
    val questions: Int = 12,
    val secondsPerQuestion: Int = 0,
    val srsFirst: Boolean = true,
    val tasks: List<ToneTask> = emptyList(),
    val index: Int = 0,
    /** Тоны, которые уже нажал игрок (по одному на слог). */
    val entered: List<Int> = emptyList(),
    val checked: Boolean = false,
    val isCorrect: Boolean = false,
    val score: Int = 0,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val correct: Int = 0,
    val wrong: Int = 0,
    val timeLeft: Int = 0,
    val seconds: Int = 0,
    val finished: Boolean = false,
    val stars: Int = 0,
    val accuracy: Float = 1f,
    val paused: Boolean = false
) {
    val task: ToneTask? get() = tasks.getOrNull(index)

    val progress: Float
        get() = if (tasks.isEmpty()) 0f else (index + if (checked) 1 else 0).toFloat() / tasks.size
}

/**
 * «Тренажёр тонов»: показываем иероглиф и озвучиваем слово, пиньинь скрыт.
 * Игрок нажимает тон каждого слога по порядку (nǐ hǎo → 3, 3; zǎo ān → 3, 1);
 * когда набрано столько тонов, сколько слогов, ответ проверяется сам.
 */
class ToneViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: DeckRepository = (app as ChineseGamesApplication).repository
    private val sounds = (app as ChineseGamesApplication).sounds
    private val speaker = (app as ChineseGamesApplication).speaker

    private val _state = MutableStateFlow(ToneUiState())
    val state: StateFlow<ToneUiState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var advanceJob: Job? = null
    private var speechJob: Job? = null

    private val wordQuality = HashMap<Long, Float>()
    private val wordMistakes = HashMap<Long, Int>()
    private var playedWordIds: List<Long> = emptyList()

    fun start(deckIds: List<Long>, questions: Int, secondsPerQuestion: Int, srsFirst: Boolean) {
        val current = _state.value
        if (!current.loading && current.tasks.isNotEmpty() && !current.finished) return
        loadAndDeal(deckIds, questions, secondsPerQuestion, srsFirst, wordsOverride = null)
    }

    private fun loadAndDeal(
        deckIds: List<Long>,
        questions: Int,
        secondsPerQuestion: Int,
        srsFirst: Boolean,
        wordsOverride: List<Word>?
    ) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, notEnoughWords = false) }
            val pool = (wordsOverride ?: repo.studyWords(deckIds, srsFirst))
                .mapNotNull { word -> toTask(word) }
            if (pool.size < MIN_WORDS) {
                _state.update {
                    it.copy(loading = false, notEnoughWords = true, tasks = emptyList())
                }
                return@launch
            }
            val tasks = if (wordsOverride != null) pool else pool.take(questions.coerceAtLeast(1))
            deal(deckIds, questions, secondsPerQuestion, srsFirst, tasks)
        }
    }

    /** Слова без тоновых знаков или с неразборчивым пиньинем тренажёр пропускает. */
    private fun toTask(word: Word): ToneTask? {
        val syllables = PinyinTones.analyze(word.hanzi, word.pinyin) ?: return null
        if (syllables.isEmpty() || syllables.all { it.tone == 0 }) return null
        return ToneTask(word, syllables)
    }

    private fun deal(
        deckIds: List<Long>,
        questions: Int,
        secondsPerQuestion: Int,
        srsFirst: Boolean,
        tasks: List<ToneTask>
    ) {
        tickJob?.cancel()
        advanceJob?.cancel()
        wordQuality.clear()
        wordMistakes.clear()
        playedWordIds = tasks.map { it.word.id }.distinct()

        _state.value = ToneUiState(
            loading = false,
            deckIds = deckIds,
            questions = questions,
            secondsPerQuestion = secondsPerQuestion,
            srsFirst = srsFirst,
            tasks = tasks,
            timeLeft = secondsPerQuestion
        )
        sounds.whoosh()
        speakCurrent(delayMillis = 450)
        startTicking()
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                val s = _state.value
                if (s.paused || s.finished || s.loading) continue
                _state.update { it.copy(seconds = it.seconds + 1) }
                if (s.secondsPerQuestion <= 0 || s.checked) continue
                val left = s.timeLeft - 1
                if (left <= 0) {
                    miss()
                } else {
                    _state.update { it.copy(timeLeft = left) }
                }
            }
        }
    }

    /** Нажата кнопка тона (1…4, 0 — лёгкий тон). */
    fun tapTone(tone: Int) {
        val s = _state.value
        if (s.checked || s.finished || s.loading) return
        val task = s.task ?: return
        if (s.entered.size >= task.syllables.size) return
        val entered = s.entered + tone
        _state.update { it.copy(entered = entered) }
        sounds.tick()
        if (entered.size == task.syllables.size) check(entered)
    }

    /** Убрать последний введённый тон. */
    fun backspace() {
        val s = _state.value
        if (s.checked || s.finished || s.entered.isEmpty()) return
        _state.update { it.copy(entered = it.entered.dropLast(1)) }
    }

    private fun check(entered: List<Int>) {
        val s = _state.value
        val task = s.task ?: return
        val word = task.word
        if (entered == task.answer) {
            val combo = s.combo + 1
            val comboBonus = if (combo >= 2) (combo - 1) * 20 else 0
            sounds.match()
            if (combo >= 2) sounds.combo(combo)
            wordQuality[word.id] = 1f
            _state.update {
                it.copy(
                    checked = true,
                    isCorrect = true,
                    correct = it.correct + 1,
                    combo = combo,
                    bestCombo = maxOf(it.bestCombo, combo),
                    score = it.score + 100 + comboBonus + task.syllables.size * 10
                )
            }
            speakCurrent(delayMillis = 250)
            scheduleNext(1_100)
        } else {
            sounds.error()
            wordMistakes[word.id] = (wordMistakes[word.id] ?: 0) + 1
            wordQuality[word.id] = 0.2f
            _state.update {
                it.copy(
                    checked = true,
                    isCorrect = false,
                    wrong = it.wrong + 1,
                    combo = 0,
                    score = (it.score - 20).coerceAtLeast(0)
                )
            }
            speakCurrent(delayMillis = 250)
            scheduleNext(2_600)
        }
    }

    /** «Не знаю» или вышло время — показываем правильные тоны. */
    fun reveal() {
        val s = _state.value
        if (s.checked || s.finished) return
        miss()
    }

    private fun miss() {
        val word = _state.value.task?.word ?: return
        sounds.error()
        wordMistakes[word.id] = (wordMistakes[word.id] ?: 0) + 1
        wordQuality[word.id] = 0.15f
        _state.update {
            it.copy(checked = true, isCorrect = false, wrong = it.wrong + 1, combo = 0)
        }
        speakCurrent(delayMillis = 250)
        scheduleNext(2_600)
    }

    /** Озвучка слова — главный источник информации о тонах. */
    fun speakCurrent(delayMillis: Long = 0L) {
        val hanzi = _state.value.task?.word?.hanzi ?: return
        speechJob?.cancel()
        speechJob = viewModelScope.launch {
            if (delayMillis > 0) delay(delayMillis)
            if (isActive) speaker.speak(hanzi, rate = 0.85f)
        }
    }

    private fun scheduleNext(delayMillis: Long) {
        advanceJob?.cancel()
        advanceJob = viewModelScope.launch {
            delay(delayMillis)
            next()
        }
    }

    /** Перейти к следующему слову (кнопка «Далее»). */
    fun next() {
        advanceJob?.cancel()
        val s = _state.value
        if (s.finished) return
        val nextIndex = s.index + 1
        if (nextIndex >= s.tasks.size) {
            finish()
            return
        }
        _state.update {
            it.copy(
                index = nextIndex,
                entered = emptyList(),
                checked = false,
                isCorrect = false,
                timeLeft = it.secondsPerQuestion
            )
        }
        speakCurrent(delayMillis = 350)
    }

    fun pause() {
        _state.update { it.copy(paused = true) }
    }

    fun resume() {
        _state.update { it.copy(paused = false) }
    }

    private fun finish() {
        if (_state.value.finished) return
        tickJob?.cancel()
        advanceJob?.cancel()
        val s = _state.value
        val attempts = s.correct + s.wrong
        val accuracy = if (attempts == 0) 0f else s.correct.toFloat() / attempts
        val stars = when {
            accuracy >= 0.85f && s.correct >= 5 -> 3
            accuracy >= 0.6f -> 2
            else -> 1
        }
        sounds.win()
        _state.update { it.copy(finished = true, stars = stars, accuracy = accuracy) }
        viewModelScope.launch {
            repo.saveGameResult(
                GameResult(
                    game = GameKind.TONES.id,
                    asked = attempts,
                    correct = s.correct,
                    mistakes = s.wrong,
                    accuracy = accuracy,
                    durationSeconds = s.seconds,
                    bestCombo = s.bestCombo,
                    score = s.score,
                    stars = stars,
                    deckIds = s.deckIds.joinToString(",")
                )
            )
            repo.registerWordResults(wordQuality, wordMistakes)
        }
    }

    fun restart(newWords: Boolean) {
        val s = _state.value
        val deckIds = s.deckIds
        val questions = s.questions
        val seconds = s.secondsPerQuestion
        val srs = s.srsFirst
        if (deckIds.isEmpty() && playedWordIds.isEmpty()) return
        _state.value = ToneUiState(loading = true)
        if (newWords || playedWordIds.size < MIN_WORDS) {
            loadAndDeal(deckIds, questions, seconds, srs, null)
        } else {
            viewModelScope.launch {
                val words = repo.wordsByIds(playedWordIds)
                if (words.size < MIN_WORDS) {
                    loadAndDeal(deckIds, questions, seconds, srs, null)
                } else {
                    loadAndDeal(deckIds, questions, seconds, srs, words.shuffled())
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
        advanceJob?.cancel()
        speechJob?.cancel()
    }

    companion object {
        const val MIN_WORDS = 3
    }
}
