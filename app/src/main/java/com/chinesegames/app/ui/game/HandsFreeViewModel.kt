package com.chinesegames.app.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chinesegames.app.ChineseGamesApplication
import com.chinesegames.app.data.DeckRepository
import com.chinesegames.app.data.Word
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HandsFreeUiState(
    val loading: Boolean = true,
    val notEnoughWords: Boolean = false,
    val deckIds: List<Long> = emptyList(),
    val srsFirst: Boolean = true,
    /** Сколько секунд живёт одна карточка (лицо + оборот). */
    val intervalSeconds: Int = 6,
    val words: List<Word> = emptyList(),
    val index: Int = 0,
    /** Карточка перевёрнута: показываем перевод. */
    val flipped: Boolean = false,
    val playing: Boolean = true,
    /** Доля времени текущей карточки, 0…1 — для полоски под карточкой. */
    val phaseProgress: Float = 0f,
    /** Сколько карточек показано с начала сеанса (по кругу). */
    val shown: Int = 0,
    val rounds: Int = 0,
    val russianVoice: Boolean = true
) {
    val word: Word? get() = words.getOrNull(index)
}

/**
 * Режим «Без рук»: карточки листаются сами. Каждое слово живёт
 * [HandsFreeUiState.intervalSeconds] секунд: сначала лицевая сторона
 * (иероглиф, китайская озвучка), затем оборот (перевод, русская озвучка).
 * Слова идут по кругу, пока пользователь не остановит режим.
 */
class HandsFreeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: DeckRepository = (app as ChineseGamesApplication).repository
    private val sounds = (app as ChineseGamesApplication).sounds
    private val speaker = (app as ChineseGamesApplication).speaker

    private val _state = MutableStateFlow(HandsFreeUiState())
    val state: StateFlow<HandsFreeUiState> = _state.asStateFlow()

    private var loopJob: Job? = null

    /** Сигнал циклу «начать карточку заново» (после «Дальше» или смены интервала). */
    @Volatile
    private var restartCard = false

    fun start(deckIds: List<Long>, intervalSeconds: Int, srsFirst: Boolean) {
        val current = _state.value
        if (!current.loading && current.words.isNotEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, notEnoughWords = false) }
            val pool = repo.studyWords(deckIds, srsFirst)
            if (pool.size < MIN_WORDS) {
                _state.update { it.copy(loading = false, notEnoughWords = true, words = emptyList()) }
                return@launch
            }
            _state.value = HandsFreeUiState(
                loading = false,
                deckIds = deckIds,
                srsFirst = srsFirst,
                intervalSeconds = intervalSeconds.coerceIn(MIN_INTERVAL, MAX_INTERVAL),
                words = if (srsFirst) pool else pool.shuffled(),
                russianVoice = speaker.isRussianAvailable
            )
            sounds.whoosh()
            runLoop()
        }
    }

    private fun runLoop() {
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            while (isActive) {
                val s = _state.value
                val word = s.word
                if (word == null) {
                    delay(200)
                    continue
                }
                restartCard = false
                // лицевая сторона: иероглиф + китайская озвучка
                _state.update { it.copy(flipped = false, phaseProgress = 0f, shown = it.shown + 1) }
                speaker.speak(word.hanzi)
                val total = _state.value.intervalSeconds * 1_000L
                val frontMillis = (total * FRONT_SHARE).toLong()
                if (!waitPhase(frontMillis, from = 0f, to = FRONT_SHARE)) continue

                // оборот: перевод + русская озвучка
                _state.update { it.copy(flipped = true) }
                speaker.speakRussian(word.translation)
                if (!waitPhase(total - frontMillis, from = FRONT_SHARE, to = 1f)) continue

                advance()
            }
        }
    }

    /**
     * Ждём фазу карточки, обновляя полоску прогресса и уважая паузу.
     * Возвращает `false`, если карточку попросили начать заново (пропуск/смена интервала).
     */
    private suspend fun waitPhase(millis: Long, from: Float, to: Float): Boolean {
        var elapsed = 0L
        while (elapsed < millis) {
            if (restartCard) return false
            if (!_state.value.playing) {
                delay(TICK)
                continue
            }
            delay(TICK)
            elapsed += TICK
            val fraction = (elapsed.toFloat() / millis).coerceIn(0f, 1f)
            _state.update { it.copy(phaseProgress = from + (to - from) * fraction) }
        }
        return true
    }

    private fun advance() {
        _state.update { s ->
            val nextIndex = s.index + 1
            if (nextIndex >= s.words.size) {
                // круг пройден — перемешиваем и идём заново
                s.copy(words = s.words.shuffled(), index = 0, rounds = s.rounds + 1)
            } else {
                s.copy(index = nextIndex)
            }
        }
    }

    fun togglePlaying() {
        val playing = !_state.value.playing
        _state.update { it.copy(playing = playing) }
        if (!playing) speaker.stop()
        sounds.click()
    }

    fun pause() {
        _state.update { it.copy(playing = false) }
        speaker.stop()
    }

    fun resume() {
        _state.update { it.copy(playing = true) }
    }

    /** Следующее слово прямо сейчас. */
    fun skip() {
        if (_state.value.words.isEmpty()) return
        sounds.tick()
        advance()
        _state.update { it.copy(playing = true) }
        restartCard = true
    }

    /** Повторить озвучку текущей стороны карточки. */
    fun repeatSpeech() {
        val s = _state.value
        val word = s.word ?: return
        if (s.flipped) speaker.speakRussian(word.translation) else speaker.speak(word.hanzi)
    }

    /** Сменить интервал на лету: текущая карточка начинается заново. */
    fun setInterval(seconds: Int) {
        val value = seconds.coerceIn(MIN_INTERVAL, MAX_INTERVAL)
        if (value == _state.value.intervalSeconds) return
        sounds.click()
        _state.update { it.copy(intervalSeconds = value, shown = (it.shown - 1).coerceAtLeast(0)) }
        restartCard = true
    }

    override fun onCleared() {
        super.onCleared()
        loopJob?.cancel()
        speaker.stop()
    }

    companion object {
        const val MIN_WORDS = 1
        const val MIN_INTERVAL = 3
        const val MAX_INTERVAL = 30

        /** Доля интервала, которую карточка лежит лицом (иероглиф). */
        private const val FRONT_SHARE = 0.55f
        private const val TICK = 100L
    }
}
