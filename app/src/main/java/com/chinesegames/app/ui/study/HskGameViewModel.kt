package com.chinesegames.app.ui.study

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chinesegames.app.ChineseGamesApplication
import com.chinesegames.app.data.HskCourse
import com.chinesegames.app.data.HskGroupData
import com.chinesegames.app.data.HskWordData
import com.chinesegames.app.ui.game.PinyinText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Шесть игр на одну группу из 5 слов. Каждая игра проходится отдельно и
 * засчитывается только без ошибок — тогда группа растёт на 1/6, а при 6/6
 * слова уезжают в папку «Выученное».
 */
enum class HskGameKind(
    val id: String,
    val title: String,
    val description: String,
    /** Бит в маске прогресса группы. */
    val bit: Int,
    /** Что показывается в вопросе. */
    val promptLabel: String
) {
    PINYIN_TO_HANZI(
        id = "pinyin_to_hanzi",
        title = "Пиньинь → иероглиф",
        description = "По пиньиню выбрать нужный иероглиф",
        bit = 1,
        promptLabel = "Пиньинь"
    ),
    HANZI_TO_PINYIN(
        id = "hanzi_to_pinyin",
        title = "Иероглиф → пиньинь",
        description = "Выбрать, как читается иероглиф",
        bit = 2,
        promptLabel = "Иероглиф"
    ),
    AUDIO_TO_HANZI(
        id = "audio_to_hanzi",
        title = "Звук → иероглиф",
        description = "Слушаем слово и выбираем иероглиф",
        bit = 4,
        promptLabel = "Аудио"
    ),
    TRANSLATION_TO_HANZI(
        id = "translation_to_hanzi",
        title = "Перевод → иероглиф",
        description = "По русскому слову найти иероглиф",
        bit = 8,
        promptLabel = "Перевод"
    ),
    HANZI_TO_TRANSLATION(
        id = "hanzi_to_translation",
        title = "Иероглиф → перевод",
        description = "Выбрать верный перевод иероглифа",
        bit = 16,
        promptLabel = "Иероглиф"
    ),
    TYPE_PINYIN(
        id = "type_pinyin",
        title = "Напиши пиньинь",
        description = "Набрать пиньинь с клавиатуры (тоны можно не писать)",
        bit = 32,
        promptLabel = "Иероглиф"
    );

    companion object {
        fun fromId(id: String?): HskGameKind? = entries.firstOrNull { it.id == id }
    }
}

/** Один вопрос игры курса. */
data class HskQuestionData(
    val word: HskWordData,
    val prompt: String,
    val promptSub: String?,
    val options: List<String>,
    val correctIndex: Int,
    /** Верный ответ строкой — нужен для игры с вводом пиньиня. */
    val answer: String,
    /** Вопрос с вводом с клавиатуры (без вариантов). */
    val typing: Boolean = false
)

data class HskGameState(
    val loading: Boolean = true,
    val notReady: Boolean = false,
    val title: String = "",
    val subtitle: String = "",
    val game: HskGameKind = HskGameKind.PINYIN_TO_HANZI,
    val isExam: Boolean = false,
    val level: Int = 1,
    val topicId: String = "",
    val groupIndex: Int = 0,
    val words: List<HskWordData> = emptyList(),
    val questions: List<HskQuestionData> = emptyList(),
    val index: Int = 0,
    val input: String = "",
    val chosen: Int? = null,
    val revealed: Boolean = false,
    /** Верным ли был последний ответ. */
    val lastCorrect: Boolean = false,
    val correct: Int = 0,
    val mistakes: Int = 0,
    val score: Int = 0,
    val finished: Boolean = false,
    val passed: Boolean = false,
    val wordsSaved: Int = 0,
    val ttsAvailable: Boolean = true
) {
    val question: HskQuestionData? get() = questions.getOrNull(index)

    val total: Int get() = questions.size

    val progress: Float
        get() = if (questions.isEmpty()) {
            0f
        } else {
            (index + if (revealed) 1 else 0).toFloat() / questions.size
        }
}

/**
 * Движок игр курса и экзамена по уровню HSK.
 * Вопросы собираются из слов группы (для экзамена — из всех слов уровня),
 * результат пишется в прогресс курса.
 */
class HskGameViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as ChineseGamesApplication).repository
    private val sounds = (app as ChineseGamesApplication).sounds
    private val speaker = (app as ChineseGamesApplication).speaker
    private val settings = (app as ChineseGamesApplication).settings

    private val _state = MutableStateFlow(HskGameState())
    val state: StateFlow<HskGameState> = _state.asStateFlow()

    private var advanceJob: Job? = null
    private var speechJob: Job? = null

    /* ------------------------------- Запуск ------------------------------- */

    /** Игра на группу из 5 слов. */
    fun startGroup(level: Int, topicId: String, groupIndex: Int, gameId: String) {
        val kind = HskGameKind.fromId(gameId) ?: HskGameKind.PINYIN_TO_HANZI
        _state.value = HskGameState(loading = true, game = kind, level = level, topicId = topicId, groupIndex = groupIndex)
        viewModelScope.launch {
            val group: HskGroupData? = HskCourse.level(getApplication(), level)?.group(topicId, groupIndex)
            if (group == null || group.words.size < MIN_WORDS) {
                _state.update { it.copy(loading = false, notReady = true) }
                return@launch
            }
            val questions = group.words.shuffled().map { word ->
                buildQuestion(word, group.words, kind)
            }
            _state.value = HskGameState(
                loading = false,
                title = kind.title,
                subtitle = "${group.topicTitle} · группа ${group.number}",
                game = kind,
                level = level,
                topicId = topicId,
                groupIndex = groupIndex,
                words = group.words,
                questions = questions,
                ttsAvailable = speaker.isAvailable
            )
            sounds.whoosh()
            speakCurrent(force = true)
        }
    }

    /** Экзамен по уровню: смешанные вопросы по всем словам уровня. */
    fun startExam(level: Int) {
        _state.value = HskGameState(loading = true, isExam = true, level = level)
        viewModelScope.launch {
            val levelData = HskCourse.level(getApplication(), level)
            val pool = levelData?.words.orEmpty()
            if (pool.size < MIN_WORDS) {
                _state.update { it.copy(loading = false, notReady = true) }
                return@launch
            }
            val examKinds = listOf(
                HskGameKind.TRANSLATION_TO_HANZI,
                HskGameKind.HANZI_TO_TRANSLATION,
                HskGameKind.PINYIN_TO_HANZI,
                HskGameKind.AUDIO_TO_HANZI
            )
            val picked = pool.shuffled().take(HskCourse.EXAM_QUESTIONS)
            val questions = picked.mapIndexed { position, word ->
                buildQuestion(word, pool, examKinds[position % examKinds.size])
            }
            _state.value = HskGameState(
                loading = false,
                title = "Экзамен HSK $level",
                subtitle = "${questions.size} вопросов · проход ${EXAM_PASS_TEXT}",
                isExam = true,
                level = level,
                words = picked,
                questions = questions,
                ttsAvailable = speaker.isAvailable
            )
            sounds.whoosh()
            speakCurrent(force = true)
        }
    }

    private fun buildQuestion(
        word: HskWordData,
        pool: List<HskWordData>,
        kind: HskGameKind
    ): HskQuestionData = when (kind) {
        HskGameKind.PINYIN_TO_HANZI -> choiceQuestion(
            word = word,
            pool = pool,
            prompt = word.pinyin,
            promptSub = null,
            optionText = { it.hanzi }
        )

        HskGameKind.HANZI_TO_PINYIN -> choiceQuestion(
            word = word,
            pool = pool,
            prompt = word.hanzi,
            promptSub = null,
            optionText = { it.pinyin }
        )

        HskGameKind.AUDIO_TO_HANZI -> choiceQuestion(
            word = word,
            pool = pool,
            prompt = "🔊",
            promptSub = null,
            optionText = { it.hanzi }
        )

        HskGameKind.TRANSLATION_TO_HANZI -> choiceQuestion(
            word = word,
            pool = pool,
            prompt = word.translation,
            promptSub = null,
            optionText = { it.hanzi }
        )

        HskGameKind.HANZI_TO_TRANSLATION -> choiceQuestion(
            word = word,
            pool = pool,
            prompt = word.hanzi,
            promptSub = word.pinyin,
            optionText = { it.translation }
        )

        HskGameKind.TYPE_PINYIN -> HskQuestionData(
            word = word,
            prompt = word.hanzi,
            promptSub = word.translation,
            options = emptyList(),
            correctIndex = -1,
            answer = word.pinyin,
            typing = true
        )
    }

    /** Варианты ответа: верный + три непохожих из той же группы. */
    private fun choiceQuestion(
        word: HskWordData,
        pool: List<HskWordData>,
        prompt: String,
        promptSub: String?,
        optionText: (HskWordData) -> String
    ): HskQuestionData {
        val correct = optionText(word)
        val distractors = LinkedHashSet<String>()
        pool.filter { it.hanzi != word.hanzi }
            .shuffled()
            .forEach { candidate ->
                val text = optionText(candidate)
                if (text.isNotBlank() && text != correct) distractors.add(text)
            }
        val options = ArrayList<String>(4)
        options.add(correct)
        options.addAll(distractors.take(OPTIONS - 1))
        options.shuffle()
        return HskQuestionData(
            word = word,
            prompt = prompt,
            promptSub = promptSub,
            options = options,
            correctIndex = options.indexOf(correct).coerceAtLeast(0),
            answer = correct
        )
    }

    /* ------------------------------- Ответы ------------------------------- */

    fun updateInput(value: String) {
        val s = _state.value
        if (s.revealed || s.finished) return
        _state.update { it.copy(input = value.take(40)) }
    }

    fun answer(optionIndex: Int) {
        val s = _state.value
        val question = s.question ?: return
        if (s.revealed || s.finished || question.typing) return
        if (optionIndex !in question.options.indices) return
        reveal(isCorrect = optionIndex == question.correctIndex, chosen = optionIndex)
    }

    /** Проверка введённого пиньиня. */
    fun submitTyped() {
        val s = _state.value
        val question = s.question ?: return
        if (s.revealed || s.finished || !question.typing) return
        if (s.input.isBlank()) return
        val expected = PinyinText.normalize(question.answer)
        val entered = PinyinText.normalize(s.input)
        reveal(isCorrect = expected.isNotEmpty() && entered == expected, chosen = null)
    }

    /** «Не знаю» — показываем правильный ответ и считаем ошибкой. */
    fun skip() {
        val s = _state.value
        if (s.revealed || s.finished) return
        reveal(isCorrect = false, chosen = null)
    }

    private fun reveal(isCorrect: Boolean, chosen: Int?) {
        val s = _state.value
        val question = s.question ?: return
        if (isCorrect) sounds.match() else sounds.error()

        val gained = if (isCorrect) POINTS_PER_ANSWER else -PENALTY_PER_MISTAKE

        _state.update {
            it.copy(
                chosen = chosen,
                revealed = true,
                lastCorrect = isCorrect,
                correct = it.correct + if (isCorrect) 1 else 0,
                mistakes = it.mistakes + if (isCorrect) 0 else 1,
                score = (it.score + gained).coerceAtLeast(0)
            )
        }
        speakAfterAnswer(question.word.hanzi)
        scheduleNext(if (isCorrect) 900L else 1_700L)
    }

    fun next() {
        advanceJob?.cancel()
        val s = _state.value
        if (s.finished) return
        val nextIndex = s.index + 1
        if (nextIndex >= s.questions.size) {
            finish()
            return
        }
        _state.update {
            it.copy(index = nextIndex, input = "", chosen = null, revealed = false)
        }
        speakCurrent()
    }

    /* --------------------------- Озвучка и пауза --------------------------- */

    private fun speakCurrent(force: Boolean = false) {
        val s = _state.value
        if (s.revealed && !force) return
        if (s.game != HskGameKind.AUDIO_TO_HANZI && !s.isExam) return
        val word = s.question?.word ?: return
        speaker.speak(word.hanzi)
    }

    fun repeatAudio() {
        _state.value.question?.let { speaker.speak(it.word.hanzi) }
    }

    /** Озвучка слова после ответа (настройка «Озвучка слов»). */
    private fun speakAfterAnswer(hanzi: String) {
        if (!settings.settings.speakWords || hanzi.isBlank()) return
        speechJob?.cancel()
        speechJob = viewModelScope.launch {
            delay(300)
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

        viewModelScope.launch {
            if (s.isExam) {
                val exam = repo.registerExam(s.level, s.correct, s.total, s.score)
                _state.update { it.copy(passed = exam.passed) }
            } else {
                val group = HskCourse.level(getApplication(), s.level)?.group(s.topicId, s.groupIndex)
                val key = group?.key ?: return@launch
                val progress = repo.registerGroupGame(
                    key = key,
                    level = s.level,
                    topicId = s.topicId,
                    groupIndex = s.groupIndex,
                    gameBit = s.game.bit,
                    passed = passed,
                    score = s.score
                )
                if (progress.learned) {
                    // Группа закрыта на 100% — слова уезжают в папку «Выученное».
                    val saved = repo.addLearnedWords(group.words)
                    _state.update { it.copy(wordsSaved = saved) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        advanceJob?.cancel()
        speechJob?.cancel()
    }

    companion object {
        const val MIN_WORDS = 3
        const val OPTIONS = 4
        const val POINTS_PER_ANSWER = 100
        const val PENALTY_PER_MISTAKE = 25
        const val EXAM_PASS_TEXT = "80%"
    }
}
