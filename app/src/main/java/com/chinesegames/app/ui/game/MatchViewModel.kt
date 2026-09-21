package com.chinesegames.app.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chinesegames.app.ChineseGamesApplication
import com.chinesegames.app.data.DeckRepository
import com.chinesegames.app.data.MatchResult
import com.chinesegames.app.data.Word
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Режимы игры «Найди пару». */
enum class GameMode(
    val title: String,
    val hint: String,
    val emoji: String
) {
    CLASSIC(
        title = "Классика",
        hint = "Все карточки закрыты — ищем пару по памяти",
        emoji = "🃏"
    ),
    RUSSIAN_TO_HANZI(
        title = "Русский → 汉字",
        hint = "Иероглифы открыты: подбираем к русскому слову его 汉字",
        emoji = "🈶"
    ),
    AUDIO_TO_HANZI(
        title = "🔊 Аудио → 汉字",
        hint = "Слушаем карточку-звук и находим её иероглиф",
        emoji = "🎧"
    )
}

enum class CardKind { HANZI, RUSSIAN, AUDIO }

data class MatchCard(
    val id: Int,
    val pairId: Int,
    val wordId: Long,
    val kind: CardKind,
    val main: String,
    val sub: String? = null,
    /** Иероглиф этого слова — для озвучки. */
    val hanzi: String = ""
)

data class MatchUiState(
    val loading: Boolean = true,
    val notEnoughWords: Boolean = false,
    val cards: List<MatchCard> = emptyList(),
    val faceUpByDefault: Set<Int> = emptySet(),
    val selected: List<Int> = emptyList(),
    val revealed: Set<Int> = emptySet(),
    val matched: Set<Int> = emptySet(),
    val wrongPair: Set<Int> = emptySet(),
    val pairsTotal: Int = 0,
    val pairsFound: Int = 0,
    val mistakes: Int = 0,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val seconds: Int = 0,
    val score: Int = 0,
    val hintsLeft: Int = MAX_HINTS,
    val hintActive: Boolean = false,
    val locked: Boolean = false,
    val finished: Boolean = false,
    val stars: Int = 0,
    val accuracy: Float = 0f,
    val mode: GameMode = GameMode.CLASSIC,
    val deckIds: List<Long> = emptyList(),
    val pairsRequested: Int = 0,
    val message: String? = null,
    val comboMessage: String? = null,
    val shakeKey: Int = 0,
    val paused: Boolean = false
) {
    val progress: Float
        get() = if (pairsTotal == 0) 0f else pairsFound.toFloat() / pairsTotal

    companion object {
        const val MAX_HINTS = 3
    }
}

/**
 * Движок игры «Найди пару»: раздача карточек, проверка пар,
 * комбо, очки, таймер и статистика по каждому слову.
 */
class MatchViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: DeckRepository = (app as ChineseGamesApplication).repository
    private val sounds = (app as ChineseGamesApplication).sounds
    private val speaker = (app as ChineseGamesApplication).speaker

    private val _state = MutableStateFlow(MatchUiState())
    val state: StateFlow<MatchUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var messageJob: Job? = null

    /** Ошибки по каждому слову в текущей партии. */
    private val wordMistakes = HashMap<Long, Int>()
    private val wordsInGame = LinkedHashSet<Long>()

    /* --------------------------- Старт партии --------------------------- */

    fun start(deckIds: List<Long>, pairsRequested: Int, mode: GameMode) {
        val current = _state.value
        if (!current.loading && current.cards.isNotEmpty() && !current.finished) return

        viewModelScope.launch {
            _state.update {
                it.copy(loading = true, notEnoughWords = false)
            }
            val words = repo.wordsOfDecks(deckIds)
            if (words.size < MIN_WORDS) {
                _state.update { it.copy(loading = false, notEnoughWords = true, cards = emptyList()) }
                return@launch
            }
            deal(deckIds, pairsRequested, mode, words)
        }
    }

    private fun deal(
        deckIds: List<Long>,
        pairsRequested: Int,
        mode: GameMode,
        source: List<Word>,
        exactWords: Boolean = false
    ) {
        timerJob?.cancel()
        messageJob?.cancel()
        wordMistakes.clear()
        wordsInGame.clear()

        val pairs = pairsRequested.coerceIn(MIN_PAIRS, minOf(MAX_PAIRS, source.size))
        val chosen = if (exactWords) source.take(pairs) else source.shuffled().take(pairs)

        val cards = ArrayList<MatchCard>(pairs * 2)
        val faceUp = HashSet<Int>()
        var id = 0

        chosen.forEachIndexed { index, word ->
            wordsInGame.add(word.id)
            val hanziCard = MatchCard(
                id = id++,
                pairId = index,
                wordId = word.id,
                kind = CardKind.HANZI,
                main = word.hanzi,
                sub = word.pinyin.ifBlank { null },
                hanzi = word.hanzi
            )
            val partnerCard = when (mode) {
                GameMode.AUDIO_TO_HANZI -> MatchCard(
                    id = id++,
                    pairId = index,
                    wordId = word.id,
                    kind = CardKind.AUDIO,
                    main = "🔊",
                    sub = null,
                    hanzi = word.hanzi
                )

                else -> MatchCard(
                    id = id++,
                    pairId = index,
                    wordId = word.id,
                    kind = CardKind.RUSSIAN,
                    main = word.translation,
                    sub = null,
                    hanzi = word.hanzi
                )
            }
            if (mode != GameMode.CLASSIC) faceUp.add(hanziCard.id)
            cards.add(hanziCard)
            cards.add(partnerCard)
        }

        val shuffled = cards.shuffled()

        _state.value = MatchUiState(
            loading = false,
            cards = shuffled,
            faceUpByDefault = faceUp,
            revealed = faceUp,
            pairsTotal = pairs,
            mode = mode,
            deckIds = deckIds,
            pairsRequested = pairsRequested,
            hintsLeft = MatchUiState.MAX_HINTS
        )

        sounds.whoosh()
        startTimer()
    }

    /* ---------------------------- Ходы игрока ---------------------------- */

    fun onCardTap(cardId: Int) {
        val s = _state.value
        if (s.loading || s.locked || s.finished || s.hintActive) return
        if (s.matched.contains(cardId) || s.selected.contains(cardId)) return

        val card = s.cards.firstOrNull { it.id == cardId } ?: return

        if (card.kind == CardKind.AUDIO) {
            speaker.speak(card.hanzi)
        }

        if (s.selected.isEmpty()) {
            sounds.flip()
            _state.update {
                it.copy(selected = listOf(cardId), revealed = it.revealed + cardId)
            }
            return
        }

        val first = s.cards.firstOrNull { it.id == s.selected.first() } ?: return
        sounds.flip()

        val isMatch = first.pairId == card.pairId && first.kind != card.kind
        if (isMatch) {
            onMatch(first, card)
        } else {
            onMiss(first, card)
        }
    }

    private fun onMatch(first: MatchCard, second: MatchCard) {
        sounds.match()

        val combo = _state.value.combo + 1
        val base = 100
        val comboBonus = if (combo >= 2) (combo - 1) * 25 else 0
        val gained = base + comboBonus

        if (combo >= 2) sounds.combo(combo)
        first.hanzi.takeIf { it.isNotBlank() }?.let { speaker.speak(it) }

        _state.update { s ->
            val found = s.pairsFound + 1
            s.copy(
                selected = emptyList(),
                revealed = s.revealed + first.id + second.id,
                matched = s.matched + first.id + second.id,
                pairsFound = found,
                combo = combo,
                bestCombo = maxOf(s.bestCombo, combo),
                score = s.score + gained,
                message = "Верно!",
                comboMessage = if (combo >= 2) "Комбо ×$combo  +$gained" else "+$gained",
                finished = found == s.pairsTotal
            )
        }

        showMessageTemporarily()
        if (_state.value.finished) completeGame()
    }

    private fun onMiss(first: MatchCard, second: MatchCard) {
        sounds.error()

        wordMistakes[first.wordId] = (wordMistakes[first.wordId] ?: 0) + 1
        if (second.wordId != first.wordId) {
            wordMistakes[second.wordId] = (wordMistakes[second.wordId] ?: 0) + 1
        }

        val wrong = setOf(first.id, second.id)
        _state.update { s ->
            s.copy(
                mistakes = s.mistakes + 1,
                combo = 0,
                locked = true,
                wrongPair = wrong,
                shakeKey = s.shakeKey + 1,
                score = (s.score - 30).coerceAtLeast(0),
                message = "Мимо!",
                comboMessage = null
            )
        }

        viewModelScope.launch {
            delay(900)
            _state.update { s ->
                val revealed = s.revealed.toMutableSet()
                wrong.forEach { id -> if (!s.faceUpByDefault.contains(id)) revealed.remove(id) }
                s.copy(
                    selected = emptyList(),
                    wrongPair = emptySet(),
                    locked = false,
                    message = null,
                    revealed = revealed
                )
            }
        }
    }

    /* ------------------------------ Подсказка ---------------------------- */

    fun useHint() {
        val s = _state.value
        if (s.hintsLeft <= 0 || s.finished || s.hintActive || s.locked || s.loading) return
        sounds.whoosh()
        _state.update {
            it.copy(
                hintsLeft = it.hintsLeft - 1,
                hintActive = true,
                locked = true,
                score = (it.score - 75).coerceAtLeast(0)
            )
        }
        viewModelScope.launch {
            delay(1600)
            _state.update { it.copy(hintActive = false, locked = false) }
        }
    }

    /* ------------------------------- Таймер ------------------------------ */

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                _state.update { s ->
                    if (s.finished || s.paused || s.loading) s else s.copy(seconds = s.seconds + 1)
                }
            }
        }
    }

    fun pause() {
        _state.update { it.copy(paused = true) }
    }

    fun resume() {
        _state.update { it.copy(paused = false) }
    }

    /* ---------------------------- Окончание ------------------------------ */

    private fun completeGame() {
        timerJob?.cancel()
        val s = _state.value
        val tries = s.pairsTotal + s.mistakes
        val accuracy = if (tries == 0) 1f else s.pairsTotal.toFloat() / tries.toFloat()
        val timeBonus = (s.pairsTotal * 12 - s.seconds / 2).coerceAtLeast(0)
        val score = (s.score + timeBonus).coerceAtLeast(0)
        val stars = when {
            accuracy >= 0.85f -> 3
            accuracy >= 0.6f -> 2
            else -> 1
        }

        sounds.win()
        _state.update {
            it.copy(finished = true, stars = stars, accuracy = accuracy, score = score, comboMessage = null, message = null)
        }

        viewModelScope.launch {
            repo.saveMatchResult(
                MatchResult(
                    pairs = s.pairsTotal,
                    cards = s.cards.size,
                    mistakes = s.mistakes,
                    hints = MatchUiState.MAX_HINTS - s.hintsLeft,
                    accuracy = accuracy,
                    durationSeconds = s.seconds,
                    bestCombo = s.bestCombo,
                    score = score,
                    stars = stars,
                    deckIds = s.deckIds.joinToString(",")
                )
            )
            val quality = wordsInGame.associateWith { wordId ->
                when (wordMistakes[wordId] ?: 0) {
                    0 -> 1f
                    1 -> 0.6f
                    2 -> 0.35f
                    else -> 0.15f
                }
            }
            repo.registerWordResults(quality, wordMistakes)
        }
    }

    /* ------------------------------ Рестарт ------------------------------ */

    /**
     * [newWords] = true  — берём другие случайные слова из тех же папок.
     * [newWords] = false — повторяем ровно те же слова, что были в партии.
     */
    fun restart(newWords: Boolean) {
        val s = _state.value
        if (s.deckIds.isEmpty()) return
        val playedWordIds = s.cards.map { it.wordId }.distinct()

        _state.value = MatchUiState(loading = true)
        viewModelScope.launch {
            if (newWords || playedWordIds.size < MIN_WORDS) {
                val words = repo.wordsOfDecks(s.deckIds)
                if (words.size < MIN_WORDS) {
                    _state.update { it.copy(loading = false, notEnoughWords = true) }
                } else {
                    deal(s.deckIds, s.pairsRequested, s.mode, words)
                }
            } else {
                val same = repo.wordsByIds(playedWordIds).shuffled()
                deal(s.deckIds, s.pairsRequested, s.mode, same, exactWords = true)
            }
        }
    }

    private fun showMessageTemporarily() {
        messageJob?.cancel()
        messageJob = viewModelScope.launch {
            delay(1_400)
            _state.update { if (it.finished) it else it.copy(message = null, comboMessage = null) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        messageJob?.cancel()
    }

    companion object {
        const val MIN_PAIRS = 3
        const val MAX_PAIRS = 30
        const val MIN_WORDS = 3
    }
}
