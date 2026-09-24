package com.chinesegames.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Папки словаря «HSK 1» … «HSK 7» с подпапками-разделами курса.
 *
 * Слова в них — ровно те же, что в курсе «Поэтапное изучение» (assets/hsk),
 * поэтому с любым уровнем или разделом можно играть в любую игру, а также
 * отмечать слова звёздочкой. Папки открыты с первого запуска — курс с замками
 * это отдельный режим, словарь ограничений не ставит.
 *
 * Синхронизация идемпотентна: создаются только недостающие папки и слова,
 * лишнее не трогается, пользовательские папки не затрагиваются. Чтобы не
 * гонять 7 500 слов при каждом старте, в настройках хранится подпись
 * материала — если она совпала, ничего не делаем.
 */
class HskDeckSeeder(
    private val context: Context,
    private val deckDao: DeckDao,
    private val wordDao: WordDao
) {

    suspend fun sync() = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val levels = (1..HskCourse.LEVELS).mapNotNull { HskCourse.level(context, it) }
        if (levels.isEmpty()) return@withContext

        val signature = levels.joinToString(",") { "${it.level}:${it.words.size}:${it.topics.size}" } +
            "|v$VERSION"
        if (prefs.getString(KEY_SIGNATURE, null) == signature) return@withContext

        levels.forEach { level -> syncLevel(level) }
        prefs.edit().putString(KEY_SIGNATURE, signature).apply()
    }

    private suspend fun syncLevel(level: HskLevelData) {
        val levelKey = Deck.levelKey(level.level)
        val levelDeck = deckDao.getByCourseKey(levelKey)
        val levelId = if (levelDeck == null) {
            deckDao.insert(
                Deck(
                    name = HskCourse.levelTitle(level.level),
                    emoji = Deck.levelEmoji(level.level),
                    isSystem = true,
                    parentId = null,
                    courseKey = levelKey,
                    sortOrder = COURSE_SORT_BASE + level.level
                )
            )
        } else {
            val fixed = levelDeck.copy(
                name = HskCourse.levelTitle(level.level),
                emoji = Deck.levelEmoji(level.level),
                isSystem = true,
                sortOrder = COURSE_SORT_BASE + level.level
            )
            if (fixed != levelDeck) deckDao.update(fixed)
            levelDeck.id
        }

        level.topics.forEachIndexed { index, topic ->
            val topicKey = Deck.topicKey(level.level, topic.id)
            val existing = deckDao.getByCourseKey(topicKey)
            val topicId = if (existing == null) {
                deckDao.insert(
                    Deck(
                        name = topic.title,
                        emoji = topic.emoji,
                        isSystem = true,
                        parentId = levelId,
                        courseKey = topicKey,
                        sortOrder = index
                    )
                )
            } else {
                val fixed = existing.copy(
                    name = topic.title,
                    emoji = topic.emoji,
                    isSystem = true,
                    parentId = levelId,
                    sortOrder = index
                )
                if (fixed != existing) deckDao.update(fixed)
                existing.id
            }

            val present = wordDao.hanziInDeck(topicId).toHashSet()
            val missing = ArrayList<Word>()
            topic.words.forEach { word ->
                val hanzi = word.hanzi.trim()
                if (hanzi.isEmpty() || !present.add(hanzi)) return@forEach
                missing.add(
                    Word(
                        deckId = topicId,
                        hanzi = hanzi,
                        pinyin = word.pinyin.trim(),
                        translation = word.translation.trim()
                    )
                )
            }
            if (missing.isNotEmpty()) wordDao.insertAll(missing)
        }
    }

    companion object {
        private const val PREFS = "hsk_decks"
        private const val KEY_SIGNATURE = "signature"

        /** Поднимайте, если изменилась схема папок курса (не сам материал). */
        private const val VERSION = 1

        /** Папки курса идут после пользовательских (у тех sortOrder = 0). */
        const val COURSE_SORT_BASE = 1_000
    }
}
