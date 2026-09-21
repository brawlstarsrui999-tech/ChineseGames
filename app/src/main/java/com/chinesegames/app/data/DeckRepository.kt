package com.chinesegames.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Единая точка доступа к данным: папки, слова и статистика.
 */
class DeckRepository(
    private val deckDao: DeckDao,
    private val wordDao: WordDao,
    private val statsDao: StatsDao
) {

    /* ----------------------------- Папки ----------------------------- */

    val decks: Flow<List<Deck>> = deckDao.observeDecks()

    val wordCounts: Flow<Map<Long, Int>> =
        deckDao.observeWordCounts().map { list -> list.associate { it.deckId to it.count } }

    val learnedCounts: Flow<Map<Long, Int>> =
        deckDao.observeLearnedCounts().map { list -> list.associate { it.deckId to it.count } }

    fun deckFlow(id: Long): Flow<Deck?> = deckDao.observeDeck(id)

    suspend fun getDeck(id: Long): Deck? = deckDao.getDeck(id)

    suspend fun createDeck(name: String, emoji: String): Long =
        deckDao.insert(Deck(name = name.trim(), emoji = emoji))

    suspend fun updateDeck(deck: Deck) = deckDao.update(deck)

    /** Удаляем папку вместе со статистикой по её словам. */
    suspend fun deleteDeck(deck: Deck) {
        val words = wordDao.getByDeck(deck.id)
        if (words.isNotEmpty()) {
            statsDao.deleteWordStats(words.map { it.id })
        }
        deckDao.delete(deck)
    }

    /* ----------------------------- Слова ----------------------------- */

    fun words(deckId: Long): Flow<List<Word>> = wordDao.observeByDeck(deckId)

    suspend fun wordsOf(deckId: Long): List<Word> = wordDao.getByDeck(deckId)

    suspend fun wordsOfDecks(deckIds: List<Long>): List<Word> =
        if (deckIds.isEmpty()) emptyList() else wordDao.getByDecks(deckIds)

    suspend fun wordsByIds(ids: List<Long>): List<Word> =
        if (ids.isEmpty()) emptyList() else wordDao.getByIds(ids)

    suspend fun countWordsInDecks(deckIds: List<Long>): Int =
        if (deckIds.isEmpty()) 0 else wordDao.countInDecks(deckIds)

    suspend fun addWord(deckId: Long, hanzi: String, pinyin: String, translation: String): Long =
        wordDao.insert(
            Word(
                deckId = deckId,
                hanzi = hanzi.trim(),
                pinyin = pinyin.trim(),
                translation = translation.trim()
            )
        )

    suspend fun updateWord(word: Word) = wordDao.update(word)

    suspend fun deleteWord(word: Word) {
        statsDao.deleteWordStats(listOf(word.id))
        wordDao.delete(word)
    }

    /* --------------------------- Статистика --------------------------- */

    val totalWords: Flow<Int> = wordDao.observeTotalCount()

    val learnedWords: Flow<Int> = statsDao.observeLearnedWords().map { it ?: 0 }

    val gamesPlayed: Flow<Int> = statsDao.observeGamesPlayed()

    val pairsFound: Flow<Int> = statsDao.observePairsFound().map { it ?: 0 }

    val averageAccuracy: Flow<Float> = statsDao.observeAverageAccuracy().map { it ?: 0f }

    val totalSeconds: Flow<Int> = statsDao.observeTotalSeconds().map { it ?: 0 }

    val bestScore: Flow<Int> = statsDao.observeBestScore().map { it ?: 0 }

    suspend fun saveMatchResult(result: MatchResult) = statsDao.insertResult(result)

    /**
     * Обновляем «память» по каждому слову после партии.
     * [quality] — от 0 до 1 (1.0 — нашли пару без единой ошибки).
     */
    suspend fun registerWordResults(quality: Map<Long, Float>, mistakes: Map<Long, Int>) {
        quality.forEach { (wordId, q) ->
            val old = statsDao.getWordStat(wordId)
            val played = (old?.timesPlayed ?: 0) + 1
            val qualitySum = (old?.qualitySum ?: 0f) + q
            val wrong = (old?.timesWrong ?: 0) + (mistakes[wordId] ?: 0)
            statsDao.upsertWordStat(
                GameWordStat(
                    wordId = wordId,
                    timesPlayed = played,
                    timesWrong = wrong,
                    qualitySum = qualitySum,
                    accuracy = (qualitySum / played).coerceIn(0f, 1f),
                    lastPlayedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
