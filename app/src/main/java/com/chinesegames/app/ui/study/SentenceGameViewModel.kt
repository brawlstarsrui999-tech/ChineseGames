package com.chinesegames.app.ui.study

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chinesegames.app.ChineseGamesApplication
import com.chinesegames.app.data.HskCourse
import com.chinesegames.app.data.HskSentenceData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Четыре игры на предложения: сборка китайской фразы, сборка перевода,
 * перевод на слух и выбор правильной озвучки.
 */
enum class SentenceGameKind(
    val id: String,
    val title: String,
    val description: String,
    val bit: Int
) {
    RU_TO_CN(
        id = "ru_to_cn",
        title = "Русский → китайский",
        description = "Собрать китайскую фразу из слов",
        bit = 1
    ),
    CN_TO_RU(
        id = "cn_to_ru",
        title = "Китайский → русский",
        description = "Собрать русский перевод из слов",
        bit = 2
    ),
    AUDIO_TO_RU(
        id = "audio_to_ru",
        title = "Звук → русский",
        description = "Услышать фразу и собрать её перевод",
        bit = 4
    ),
    RU_TO_AUDIO(
        id = "ru_to_audio",
        title = "Русский → звук",
        description = "Найти озвучку, которая подходит к переводу",
        bit = 8
    );

    companion object {
        fun fromId(id: String?): SentenceGameKind? = entries.firstOrNull { it.id == id }
    }
}

data class SentenceGameState(
    val loading: Boolean = true,
    val notReady: Boolean = false,
    val title: String = "",
    val subtitle: String = "",
    val game: SentenceGameKind = SentenceGameKind.RU_TO_CN,
    val level: Int = 1,
    val topicId: String = "",
    val sentences: List<HskSentenceData> = emptyList(),
    val index: Int = 0,
    /** Плитки со словами (целевые + лишние), перемешанные. */
    val tiles: List<String> = emptyList(),
    /** Индексы выбранных плиток — в порядке выбора. */
    val picked: List<Int> = emptyList(),
    /** Варианты для игры «Русский → звук». */
    val options: List<HskSentenceData> = emptyList(),
    val correctOption: Int = -1,
    val chosen: Int? = null,
    val revealed: Boolean = false,
    val correct: Int = 0,
    val mistakes: Int = 0,
    val finished: Boolean = false,
    val passed: Boolean = false,
    val ttsAvailable: Boolean = true
) {
    val sentence: HskSentenceData? get() = sentences.getOrNull(index)

    val total: Int get() = sentences.size

    val progress: Float
        get() = if (sentences.isEmpty()) {
            0f
        } else {
            (index + if (revealed) 1 else 0).toFloat() / sentences.size
        }

    /** Игра со сборкой из плиток (все, кроме выбора озвучки). */
    val isTileGame: Boolean get() = game != SentenceGameKind.RU_TO_AUDIO

    /** Слова, которые нужно собрать в этой игре. */
    val target: List<String>
        get() = when (game) {
            SentenceGameKind.RU_TO_CN -> sentence?.tokens.orEmpty()
            else -> sentence?.ruTokens.orEmpty()
        }

    val pickedWords: List<String> get() = picked.mapNotNull { tiles.getOrNull(it) }

    val pickedText: String get() = pickedWords.joinToString(" ")

    val isPickCorrect: Boolean
        get() = pickedWords.joinToString("") == target.joinToString("")
}

/** Движок игр с предложениями. */
class SentenceGameViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as ChineseGamesApplication).repository
    private val sounds = (app as ChineseGamesApplication).sounds
    private val speaker = (app as ChineseGamesApplication).speaker
    private val settings = (app as ChineseGamesApplication).settings

    private val _state = MutableStateFlow(SentenceGameState())
    val state: StateFlow<SentenceGameState> = _state.asStateFlow()

    private var advanceJob: Job? = null
    private var speechJob: Job? = null

    private var topicSentences: List<HskSentenceData> = emptyList()

    fun start(level: Int, topicId: String, gameId: String) {
        val kind = SentenceGameKind.fromId(gameId) ?: SentenceGameKind.RU_TO_CN
        _state.value = SentenceGameState(loading = true, game = kind, level = level, topicId = topicId)
        viewModelScope.launch {
            val topic = HskCourse.sentenceTopics(getApplication(), level).firstOrNull { it.id == topicId }
            if (topic == null || topic.sentences.size < MIN_SENTENCES) {
                _state.update { it.copy(loading = false, notReady = true) }
                return@launch
            }
            topicSentences = topic.sentences
            val sentences = topic.sentences.shuffled()
            val first = sentences.first()
            val options = buildOptions(first, sentences)
            _state.value = SentenceGameState(
                loading = false,
                title = kind.title,
                subtitle = topic.title,
                game = kind,
                level = level,
                topicId = topicId,
                sentences = sentences,
                tiles = buildTiles(first, kind, sentences),
                options = options,
                correctOption = options.indexOfFirst { it.hanzi == first.hanzi }.coerceAtLeast(0),
                ttsAvailable = speaker.isAvailable
            )
            sounds.whoosh()
            speakCurrent(force = true)
        }
    }

    /** Плитки: слова цели + три лишних слова из других предложений раздела. */
    private fun buildTiles(
        sentence: HskSentenceData,
        kind: SentenceGameKind,
        sentences: List<HskSentenceData>
    ): List<String> {
        val target = if (kind == SentenceGameKind.RU_TO_CN) sentence.tokens else sentence.ruTokens
        val extras = sentences
            .filter { it.hanzi != sentence.hanzi }
            .flatMap { if (kind == SentenceGameKind.RU_TO_CN) it.tokens else it.ruTokens }
            .filter { !target.contains(it) }
            .distinct()
            .shuffled()
            .take(EXTRA_TILES)
        return (target + extras).shuffled()
    }

    /** Варианты озвучки: правильное предложение первым, дальше перемешиваем. */
    private fun buildOptions(
        sentence: HskSentenceData,
        sentences: List<HskSentenceData>
    ): List<HskSentenceData> {
        val others = sentences.filter { it.hanzi != sentence.hanzi }.shuffled().take(OPTIONS - 1)
        val options = ArrayList<HskSentenceData>(OPTIONS)
        options.add(sentence)
        options.addAll(others)
        options.shuffle()
        return options
    }

    /* ------------------------------- Ответы ------------------------------- */

    /** Тап по плитке: добавляем в ответ или убираем обратно. */
    fun tapTile(tileIndex: Int) {
        val s = _state.value
        if (s.revealed || s.finished || !s.isTileGame) return
        if (tileIndex !in s.tiles.indices) return
        _state.update {
            val picked = it.picked.toMutableList()
            if (picked.contains(tileIndex)) picked.remove(tileIndex) else picked.add(tileIndex)
            it.copy(picked = picked)
        }
        sounds.click()
    }

    fun clearTiles() {
        val s = _state.value
        if (s.revealed || s.finished) return
        _state.update { it.copy(picked = emptyList()) }
    }

    /** Проверка собранной фразы. */
    fun checkTiles() {
        val s = _state.value
        if (s.revealed || s.finished || !s.isTileGame) return
        if (s.picked.isEmpty()) return
        reveal(isCorrect = s.isPickCorrect, chosen = null)
    }

    /** Выбор озвучки в игре «Русский → звук». */
    fun chooseAudio(optionIndex: Int) {
        val s = _state.value
        if (s.revealed || s.finished || s.game != SentenceGameKind.RU_TO_AUDIO) return
        if (optionIndex !in s.options.indices) return
        val option = s.options[optionIndex]
        val isCorrect = option.hanzi == s.sentence?.hanzi
        reveal(isCorrect = isCorrect, chosen = optionIndex)
    }

    /** «Не знаю». */
    fun skip() {
        val s = _state.value
        if (s.revealed || s.finished) return
        reveal(isCorrect = false, chosen = null)
    }

    private fun reveal(isCorrect: Boolean, chosen: Int?) {
        val s = _state.value
        val sentence = s.sentence
        if (isCorrect) sounds.match() else sounds.error()
        _state.update {
            it.copy(
                chosen = chosen,
                revealed = true,
                correct = it.correct + if (isCorrect) 1 else 0,
                mistakes = it.mistakes + if (isCorrect) 0 else 1
            )
        }
        // После ответа всегда звучит китайская фраза целиком.
        if (settings.settings.speakWords && sentence != null) {
            speakSentence(sentence.hanzi)
        }
        scheduleNext(if (isCorrect) 1_400L else 2_400L)
    }

    fun next() {
        advanceJob?.cancel()
        val s = _state.value
        if (s.finished) return
        val nextIndex = s.index + 1
        if (nextIndex >= s.sentences.size) {
            finish()
            return
        }
        val nextSentence = s.sentences[nextIndex]
        val options = buildOptions(nextSentence, s.sentences)
        _state.update {
            it.copy(
                index = nextIndex,
                picked = emptyList(),
                chosen = null,
                revealed = false,
                tiles = buildTiles(nextSentence, it.game, s.sentences),
                options = options,
                correctOption = options.indexOfFirst { option ->
                    option.hanzi == nextSentence.hanzi
                }.coerceAtLeast(0)
            )
        }
        speakCurrent()
    }

    /* ------------------------------ Озвучка ------------------------------ */

    fun repeatAudio() {
        _state.value.sentence?.let { speaker.speak(it.hanzi, rate = 0.9f) }
    }

    /** Прослушать вариант озвучки (игра «Русский → звук»). */
    fun playOption(optionIndex: Int) {
        val option = _state.value.options.getOrNull(optionIndex) ?: return
        speaker.speak(option.hanzi)
    }

    private fun speakCurrent(force: Boolean = false) {
        val s = _state.value
        if (s.revealed && !force) return
        if (s.game != SentenceGameKind.AUDIO_TO_RU) return
        val sentence = s.sentence ?: return
        speaker.speak(sentence.hanzi)
    }

    private fun speakSentence(hanzi: String) {
        speechJob?.cancel()
        speechJob = viewModelScope.launch {
            delay(350)
            if (isActive) speaker.speak(hanzi)
        }
    }

    private fun scheduleNext(delayMillis: Long) {
        advanceJob?.cancel()
        advanceJob = viewModelScope.launch {
            delay(delayMillis)
            next()
        }
    }

    /* -------------------------------- Финал -------------------------------- */

    private fun finish() {
        val s = _state.value
        if (s.finished) return
        advanceJob?.cancel()
        val passed = s.mistakes == 0
        sounds.win()
        _state.update { it.copy(finished = true, passed = passed) }

        val key = topicKey(s.level, s.topicId)
        viewModelScope.launch {
            repo.registerSentenceGame(
                key = key,
                level = s.level,
                topicId = s.topicId,
                gameBit = s.game.bit,
                passed = passed
            )
        }
    }

    private fun topicKey(level: Int, topicId: String) = "$level|$topicId"

    override fun onCleared() {
        super.onCleared()
        advanceJob?.cancel()
        speechJob?.cancel()
    }

    companion object {
        const val MIN_SENTENCES = 2
        const val OPTIONS = 4
        const val EXTRA_TILES = 3
    }
}
