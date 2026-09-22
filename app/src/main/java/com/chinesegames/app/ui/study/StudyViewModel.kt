package com.chinesegames.app.ui.study

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chinesegames.app.ChineseGamesApplication
import com.chinesegames.app.data.HskCourse
import com.chinesegames.app.data.HskExam
import com.chinesegames.app.data.HskGroupProgress
import com.chinesegames.app.data.HskLevelData
import com.chinesegames.app.data.HskSentenceProgress
import com.chinesegames.app.data.HskSentenceTopicData
import com.chinesegames.app.data.HskTopicData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Всё, что нужно экранам курса: материал + прогресс. */
data class StudyState(
    /** Загруженные уровни: номер → материал. */
    val levels: Map<Int, HskLevelData> = emptyMap(),
    /** Разделы предложений: уровень → разделы. */
    val sentenceTopics: Map<Int, List<HskSentenceTopicData>> = emptyMap(),
    /** Прогресс групп по ключу группы. */
    val groups: Map<String, HskGroupProgress> = emptyMap(),
    /** Экзамены по уровням. */
    val exams: Map<Int, HskExam> = emptyMap(),
    /** Прогресс разделов предложений. */
    val sentenceProgress: Map<String, HskSentenceProgress> = emptyMap(),
    val sentencesLoading: Boolean = false
)

/**
 * Вью-модель раздела «Поэтапное изучение».
 *
 * Материал (HSK 1 → 6) читается из assets по требованию, прогресс — из базы.
 * Уровень HSK N+1 открывается только после экзамена по уровню N, а
 * «Изучение предложений» — когда все слова уровня выучены.
 */
class StudyViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as ChineseGamesApplication).repository
    private val application: Application = app

    private val _state = MutableStateFlow(StudyState())
    val state: StateFlow<StudyState> = _state.asStateFlow()

    init {
        loadCourse()
        viewModelScope.launch {
            combine(repo.hskGroups, repo.hskExams, repo.hskSentenceTopics) { groups, exams, sentences ->
                Triple(groups, exams, sentences)
            }.collect { (groups, exams, sentences) ->
                _state.update {
                    it.copy(
                        groups = groups.associateBy { progress -> progress.groupKey },
                        exams = exams.associateBy { exam -> exam.level },
                        sentenceProgress = sentences.associateBy { progress -> progress.topicKey }
                    )
                }
            }
        }
    }

    /**
     * Читаем весь материал курса (HSK 1 → 6 и предложения) один раз в фоне,
     * чтобы прогресс и замки считались сразу, а не после первого открытого уровня.
     */
    private fun loadCourse() {
        viewModelScope.launch {
            val loaded = mutableMapOf<Int, HskLevelData>()
            for (level in 1..HskCourse.LEVELS) {
                val data = HskCourse.level(application, level) ?: continue
                loaded[level] = data
            }
            if (loaded.isNotEmpty()) {
                _state.update { state ->
                    state.copy(levels = if (state.levels.isEmpty()) loaded else state.levels)
                }
            }
            openSentences()
        }
    }

    /** Подгружаем материал уровня (первый раз — из assets, дальше из кэша). */
    fun openLevel(level: Int) {
        if (_state.value.levels.containsKey(level)) return
        viewModelScope.launch {
            val data = HskCourse.level(application, level) ?: return@launch
            _state.update { it.copy(levels = it.levels + (level to data)) }
        }
    }

    fun openSentences() {
        if (_state.value.sentenceTopics.isNotEmpty() || _state.value.sentencesLoading) return
        _state.update { it.copy(sentencesLoading = true) }
        viewModelScope.launch {
            val topics = HskCourse.sentenceTopics(application)
            _state.update { state ->
                state.copy(
                    sentenceTopics = topics.groupBy { it.level },
                    sentencesLoading = false
                )
            }
        }
    }

    fun level(level: Int): HskLevelData? = _state.value.levels[level]

    fun sentenceTopics(level: Int): List<HskSentenceTopicData> =
        _state.value.sentenceTopics[level].orEmpty()

    fun group(key: String): HskGroupProgress? = _state.value.groups[key]

    fun exam(level: Int): HskExam? = _state.value.exams[level]

    fun sentenceTopic(key: String): HskSentenceProgress? = _state.value.sentenceProgress[key]

    /* ------------------------------ Прогресс ------------------------------ */

    /** Сколько слов уровня уже выучено (группы, закрытые на 100%). */
    fun learnedWords(levelData: HskLevelData): Int =
        levelData.topics.sumOf { topic ->
            topic.groups.sumOf { group ->
                if (_state.value.groups[group.key]?.learned == true) group.words.size else 0
            }
        }

    /** Доля выученных групп уровня 0..1. */
    fun levelProgress(levelData: HskLevelData): Float {
        val groups = levelData.topics.flatMap { it.groups }
        if (groups.isEmpty()) return 0f
        val passed = groups.sumOf { group ->
            _state.value.groups[group.key]?.passedGames ?: 0
        }
        return passed.toFloat() / (groups.size * HskCourse.GAMES_PER_GROUP)
    }

    fun topicProgress(topic: HskTopicData): Float {
        if (topic.groups.isEmpty()) return 0f
        val passed = topic.groups.sumOf { group ->
            _state.value.groups[group.key]?.passedGames ?: 0
        }
        return passed.toFloat() / (topic.groups.size * HskCourse.GAMES_PER_GROUP)
    }

    fun groupProgress(group: com.chinesegames.app.data.HskGroupData): Float =
        (_state.value.groups[group.key]?.passedGames ?: 0).toFloat() / HskCourse.GAMES_PER_GROUP

    /** Уровень открыт, если сдан экзамен предыдущего. */
    fun isLevelUnlocked(level: Int): Boolean =
        level <= 1 || _state.value.exams[level - 1]?.passed == true

    fun lockReason(level: Int): String? =
        if (isLevelUnlocked(level)) {
            null
        } else {
            "Сдайте экзамен HSK ${level - 1}"
        }

    /** Все ли слова уровня выучены — от этого зависят предложения. */
    fun isSentencesUnlocked(level: Int): Boolean {
        val levelData = _state.value.levels[level] ?: return false
        return levelData.topics.all { topic ->
            topic.groups.all { group -> _state.value.groups[group.key]?.learned == true }
        }
    }

    fun sentenceTopicProgress(topic: HskSentenceTopicData): Float =
        (_state.value.sentenceProgress[topic.key]?.passedGames ?: 0).toFloat() /
            HskCourse.SENTENCE_GAMES

    fun sentencesProgress(level: Int): Float {
        val topics = _state.value.sentenceTopics[level].orEmpty()
        if (topics.isEmpty()) return 0f
        val passed = topics.sumOf { topic ->
            _state.value.sentenceProgress[topic.key]?.passedGames ?: 0
        }
        return passed.toFloat() / (topics.size * HskCourse.SENTENCE_GAMES)
    }
}
