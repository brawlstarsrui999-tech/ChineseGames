package com.chinesegames.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Курс «Поэтапное изучение»: HSK 1 → HSK 6.
 *
 * Материал лежит в `assets/hsk/` (level1.json … level6.json и sentences.json)
 * и собран скриптом `tools/generate_hsk_course.py` из официальных списков HSK
 * (150 / 300 / 600 / 1200 / 2500 / 5000 слов) с русскими переводами из БКРС.
 *
 * Слова разложены по смысловым разделам («Местоимения», «Люди и семья»,
 * «Еда и напитки», «Время и погода», «Грамматика»…), каждый раздел — на
 * группы по 5 слов: столько человек реально запоминает за один подход.
 */

/** Одно слово курса. */
data class HskWordData(
    val hanzi: String,
    val pinyin: String,
    val translation: String
)

/** Учебная группа — 5 слов, которые учатся и играются вместе. */
data class HskGroupData(
    val level: Int,
    val topicId: String,
    val topicTitle: String,
    val index: Int,
    val words: List<HskWordData>
) {
    /** Ключ прогресса в базе. */
    val key: String get() = "$level|$topicId|$index"

    /** Номер группы внутри раздела (для подписи «Группа 3»). */
    val number: Int get() = index + 1
}

/** Раздел уровня: «Местоимения», «Еда и напитки»… */
data class HskTopicData(
    val id: String,
    val title: String,
    val emoji: String,
    val level: Int,
    val groups: List<HskGroupData>
) {
    val words: List<HskWordData> get() = groups.flatMap { it.words }

    val wordCount: Int get() = groups.sumOf { it.words.size }
}

/** Уровень HSK целиком. */
data class HskLevelData(
    val level: Int,
    val wordCount: Int,
    val topics: List<HskTopicData>
) {
    val groupCount: Int get() = topics.sumOf { it.groups.size }

    val words: List<HskWordData> get() = topics.flatMap { it.words }

    fun topic(id: String?): HskTopicData? = topics.firstOrNull { it.id == id }

    fun group(topicId: String?, index: Int): HskGroupData? =
        topic(topicId)?.groups?.firstOrNull { it.index == index }
}

/** Предложение для раздела «Изучение предложений». */
data class HskSentenceData(
    val hanzi: String,
    val pinyin: String,
    val translation: String,
    /** Китайское предложение, разбитое на слова — из них собираются плитки. */
    val tokens: List<String>,
    /** Русский перевод, разбитый на слова. */
    val ruTokens: List<String>
)

/** Раздел предложений: «Знакомство», «Еда и напитки»… */
data class HskSentenceTopicData(
    val id: String,
    val title: String,
    val emoji: String,
    val level: Int,
    val sentences: List<HskSentenceData>
) {
    val key: String get() = "$level|$id"
}

/** Загрузка и кэш материала курса. */
object HskCourse {

    /** Уровней в курсе. */
    const val LEVELS = 6

    /** Сколько вопросов в экзамене по уровню. */
    const val EXAM_QUESTIONS = 30

    /** Какая доля верных ответов засчитывает экзамен. */
    const val EXAM_PASS = 0.8f

    /** Игр в одной группе слов: прогресс группы = пройденные игры / 6. */
    const val GAMES_PER_GROUP = 6

    /** Игр в разделе предложений. */
    const val SENTENCE_GAMES = 4

    private val levelCache = HashMap<Int, HskLevelData>()
    private var sentenceCache: List<HskSentenceTopicData>? = null

    fun levelTitle(level: Int): String = "HSK $level"

    /** Официальный объём словаря: к концу уровня вы знаете столько слов. */
    fun cumulativeWords(level: Int): Int = when (level) {
        1 -> 150
        2 -> 300
        3 -> 600
        4 -> 1200
        5 -> 2500
        else -> 5000
    }

    fun levelDescription(level: Int): String = when (level) {
        1 -> "Первые 150 слов: приветствия, семья, еда, числа"
        2 -> "Повседневные темы: покупки, погода, транспорт"
        3 -> "Учёба, работа, хобби и простые рассказы о себе"
        4 -> "Развёрнутые темы: карьера, общество, природа"
        5 -> "Публицистика, наука, культура, абстрактные понятия"
        else -> "Свободное владение: политика, экономика, литература"
    }

    suspend fun level(context: Context, level: Int): HskLevelData? = withContext(Dispatchers.IO) {
        cachedLevel(level) ?: parseLevel(context, level)?.also { parsed ->
            synchronized(levelCache) { levelCache[level] = parsed }
        }
    }

    private fun cachedLevel(level: Int): HskLevelData? =
        synchronized(levelCache) { levelCache[level] }

    suspend fun sentenceTopics(context: Context): List<HskSentenceTopicData> =
        withContext(Dispatchers.IO) {
            sentenceCache ?: parseSentences(context).orEmpty().also { sentenceCache = it }
        }

    suspend fun sentenceTopics(context: Context, level: Int): List<HskSentenceTopicData> =
        sentenceTopics(context).filter { it.level == level }

    /* ------------------------------ Разбор ------------------------------ */

    private fun readAsset(context: Context, name: String): String? = runCatching {
        context.assets.open("hsk/$name").bufferedReader().use { it.readText() }
    }.getOrNull()

    private fun parseLevel(context: Context, level: Int): HskLevelData? {
        val text = readAsset(context, "level$level.json") ?: return null
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return null
        val topicsArray = json.optJSONArray("topics") ?: return null

        val topics = ArrayList<HskTopicData>(topicsArray.length())
        for (topicIndex in 0 until topicsArray.length()) {
            val topicJson = topicsArray.optJSONObject(topicIndex) ?: continue
            val id = topicJson.optString("id")
            val title = topicJson.optString("title")
            val emoji = topicJson.optString("emoji")
            val groupsArray = topicJson.optJSONArray("groups") ?: continue

            val groups = ArrayList<HskGroupData>(groupsArray.length())
            for (groupIndex in 0 until groupsArray.length()) {
                val groupArray = groupsArray.optJSONArray(groupIndex) ?: continue
                val words = ArrayList<HskWordData>(groupArray.length())
                for (wordIndex in 0 until groupArray.length()) {
                    val wordArray = groupArray.optJSONArray(wordIndex) ?: continue
                    if (wordArray.length() < 1) continue
                    words.add(
                        HskWordData(
                            hanzi = wordArray.optString(0),
                            pinyin = wordArray.optString(1),
                            translation = wordArray.optString(2)
                        )
                    )
                }
                if (words.isNotEmpty()) {
                    groups.add(HskGroupData(level, id, title, groupIndex, words))
                }
            }
            if (groups.isNotEmpty()) {
                topics.add(HskTopicData(id, title, emoji, level, groups))
            }
        }
        if (topics.isEmpty()) return null
        return HskLevelData(level, json.optInt("words"), topics)
    }

    private fun parseSentences(context: Context): List<HskSentenceTopicData>? {
        val text = readAsset(context, "sentences.json") ?: return null
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return null
        val levels = json.optJSONArray("levels") ?: return null

        val result = ArrayList<HskSentenceTopicData>()
        for (levelIndex in 0 until levels.length()) {
            val levelJson = levels.optJSONObject(levelIndex) ?: continue
            val level = levelJson.optInt("level")
            val topics = levelJson.optJSONArray("topics") ?: continue
            for (topicIndex in 0 until topics.length()) {
                val topicJson = topics.optJSONObject(topicIndex) ?: continue
                val sentencesArray = topicJson.optJSONArray("sentences") ?: continue
                val sentences = ArrayList<HskSentenceData>(sentencesArray.length())
                for (sentenceIndex in 0 until sentencesArray.length()) {
                    val item = sentencesArray.optJSONArray(sentenceIndex) ?: continue
                    if (item.length() < 5) continue
                    sentences.add(
                        HskSentenceData(
                            hanzi = item.optString(0),
                            pinyin = item.optString(1),
                            translation = item.optString(2),
                            tokens = stringList(item.optJSONArray(3)),
                            ruTokens = stringList(item.optJSONArray(4))
                        )
                    )
                }
                if (sentences.isNotEmpty()) {
                    result.add(
                        HskSentenceTopicData(
                            id = topicJson.optString("id"),
                            title = topicJson.optString("title"),
                            emoji = topicJson.optString("emoji"),
                            level = level,
                            sentences = sentences
                        )
                    )
                }
            }
        }
        return result.ifEmpty { null }
    }

    private fun stringList(array: org.json.JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = ArrayList<String>(array.length())
        for (index in 0 until array.length()) {
            val value = array.optString(index)
            if (value.isNotBlank()) list.add(value)
        }
        return list
    }
}
