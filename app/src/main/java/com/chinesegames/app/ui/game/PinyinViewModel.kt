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

data class PinyinUiState(
    val loading: Boolean = true,
    val notEnoughWords: Boolean = false,
    val deckIds: List<Long> = emptyList(),
    val questions: Int = 12,
    val secondsPerQuestion: Int = 0,
    val srsFirst: Boolean = true,
    val words: List<Word> = emptyList(),
    val index: Int = 0,
    val input: String = "",
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
    val word: Word? get() = words.getOrNull(index)

    val progress: Float
        get() = if (words.isEmpty()) 0f else (index + if (checked) 1 else 0).toFloat() / words.size
}

/**
 * Игра «Ввод пиньиня»: показываем иероглиф, пользователь печатает пиньинь.
 * Тоны можно не писать — сравнение идёт без них, поэтому «nihao» и «nǐ hǎo»
 * считаются одинаково верными.
 */
class PinyinViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: DeckRepository = (app as ChineseGamesApplication).repository
    private val sounds = (app as ChineseGamesApplication).sounds
    private val speaker = (app as ChineseGamesApplication).speaker

    private val _state = MutableStateFlow(PinyinUiState())
    val state: StateFlow<PinyinUiState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var advanceJob: Job? = null

    private val wordQuality = HashMap<Long, Float>()
    private val wordMistakes = HashMap<Long, Int>()
    private var playedWordIds: List<Long> = emptyList()

    fun start(deckIds: List<Long>, questions: Int, secondsPerQuestion: Int, srsFirst: Boolean) {
        val current = _state.value
        if (!current.loading && current.words.isNotEmpty() && !current.finished) return
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
            val pool = repo.studyWords(deckIds, srsFirst)
            if (pool.size < MIN_WORDS) {
                _state.update {
                    it.copy(loading = false, notEnoughWords = true, words = emptyList())
                }
                return@launch
            }
            val words = wordsOverride ?: pool.take(questions.coerceAtLeast(1))
            deal(deckIds, questions, secondsPerQuestion, srsFirst, words)
        }
    }

    private fun deal(
        deckIds: List<Long>,
        questions: Int,
        secondsPerQuestion: Int,
        srsFirst: Boolean,
        words: List<Word>
    ) {
        tickJob?.cancel()
        advanceJob?.cancel()
        wordQuality.clear()
        wordMistakes.clear()
        playedWordIds = words.map { it.id }.distinct()

        _state.value = PinyinUiState(
            loading = false,
            deckIds = deckIds,
            questions = questions,
            secondsPerQuestion = secondsPerQuestion,
            srsFirst = srsFirst,
            words = words,
            timeLeft = secondsPerQuestion
        )
        sounds.whoosh()
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
                    reveal(miss = true, message = null)
                } else {
                    _state.update { it.copy(timeLeft = left) }
                }
            }
        }
    }

    fun updateInput(value: String) {
        val s = _state.value
        if (s.checked || s.finished) return
        _state.update { it.copy(input = value.take(40)) }
    }

    /** Проверка введённого пиньиня. */
    fun submit() {
        val s = _state.value
        if (s.checked || s.finished || s.loading) return
        val word = s.word ?: return
        if (s.input.isBlank()) return
        val expected = PinyinText.normalize(word.pinyin)
        if (expected.isEmpty()) {
            reveal(miss = true, message = null)
            return
        }
        val entered = PinyinText.normalize(s.input)
        if (entered == expected) {
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
                    score = it.score + 100 + comboBonus
                )
            }
            scheduleNext(900)
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
            scheduleNext(2_200)
        }
    }

    /** «Не знаю» — сразу показываем правильный ответ. */
    fun reveal() {
        val s = _state.value
        if (s.checked || s.finished) return
        reveal(miss = true, message = null)
    }

    private fun reveal(miss: Boolean, message: String?) {
        val word = _state.value.word ?: return
        if (miss) {
            sounds.error()
            wordMistakes[word.id] = (wordMistakes[word.id] ?: 0) + 1
            wordQuality[word.id] = 0.15f
            _state.update {
                it.copy(
                    checked = true,
                    isCorrect = false,
                    wrong = it.wrong + 1,
                    combo = 0
                )
            }
            scheduleNext(2_200)
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
        if (nextIndex >= s.words.size) {
            finish()
            return
        }
        _state.update {
            it.copy(
                index = nextIndex,
                input = "",
                checked = false,
                isCorrect = false,
                timeLeft = it.secondsPerQuestion
            )
        }
    }

    fun speakCurrent() {
        _state.value.word?.let { speaker.speak(it.hanzi) }
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
                    game = GameKind.PINYIN.id,
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
        _state.value = PinyinUiState(loading = true)
        if (newWords || playedWordIds.size < MIN_WORDS) {
            loadAndDeal(deckIds, questions, seconds, srs, null)
        } else {
            viewModelScope.launch {
                val words = repo.wordsByIds(playedWordIds)
                if (words.size < MIN_WORDS) {
                    loadAndDeal(deckIds, questions, seconds, srs, null)
                } else {
                    deal(deckIds, questions, seconds, srs, words.shuffled())
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
        advanceJob?.cancel()
    }

    companion object {
        const val MIN_WORDS = 3
    }
}

/** Приведение пиньиня к «сравнимому» виду: без тонов, без пробелов, ü = u = v. */
object PinyinText {

    private val toneMap: Map<Char, Char> = mapOf(
        'ā' to 'a', 'á' to 'a', 'ǎ' to 'a', 'à' to 'a',
        'ē' to 'e', 'é' to 'e', 'ě' to 'e', 'è' to 'e',
        'ī' to 'i', 'í' to 'i', 'ǐ' to 'i', 'ì' to 'i',
        'ō' to 'o', 'ó' to 'o', 'ǒ' to 'o', 'ò' to 'o',
        'ū' to 'u', 'ú' to 'u', 'ǔ' to 'u', 'ù' to 'u',
        'ǖ' to 'u', 'ǘ' to 'u', 'ǚ' to 'u', 'ǜ' to 'u', 'ü' to 'u', 'v' to 'u',
        'ń' to 'n', 'ň' to 'n', 'ḿ' to 'm', 'ê' to 'e'
    )

    fun normalize(raw: String): String = buildString {
        raw.lowercase().forEach { char ->
            val mapped = toneMap[char] ?: char
            if (mapped.isLetter()) append(if (mapped == 'ü') 'u' else mapped)
        }
    }

    /** Подсказка для пустого пиньиня: сколько слогов ожидается. */
    fun syllableCount(raw: String): Int =
        raw.trim().split(' ', '\'', '-').
            count { it.isNotBlank() }
}
