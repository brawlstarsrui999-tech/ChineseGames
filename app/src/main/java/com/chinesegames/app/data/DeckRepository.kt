package com.chinesegames.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Единая точка доступа к данным: папки, слова, избранное и статистика.
 */
class DeckRepository(
    private val deckDao: DeckDao,
    private val wordDao: WordDao,
    private val statsDao: StatsDao,
    private val favoriteDao: FavoriteDao
) {

    /* ----------------------------- Папки ----------------------------- */

    val decks: Flow<List<Deck>> = deckDao.observeDecks()

    val wordCounts: Flow<Map<Long, Int>> =
        deckDao.observeWordCounts().map { list -> list.associate { it.deckId to it.count } }

    val learnedCounts: Flow<Map<Long, Int>> =
        deckDao.observeLearnedCounts().map { list -> list.associate { it.deckId to it.count } }

    val deckStats: Flow<List<DeckStat>> = statsDao.observeDeckStats()

    fun deckFlow(id: Long): Flow<Deck?> = deckDao.observeDeck(id)

    suspend fun getDeck(id: Long): Deck? = deckDao.getDeck(id)

    suspend fun createDeck(name: String, emoji: String): Long =
        deckDao.insert(Deck(name = name.trim(), emoji = emoji))

    suspend fun updateDeck(deck: Deck) = deckDao.update(deck)

    /** Удаляем папку вместе со статистикой и избранным по её словам. */
    suspend fun deleteDeck(deck: Deck) {
        val words = wordDao.getByDeck(deck.id)
        if (words.isNotEmpty()) {
            val ids = words.map { it.id }
            statsDao.deleteWordStats(ids)
            favoriteDao.removeAll(ids)
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
        favoriteDao.remove(word.id)
        wordDao.delete(word)
    }

    /* --------------------------- Избранное --------------------------- */

    val favoriteIds: Flow<Set<Long>> =
        favoriteDao.observeIds().map { ids -> ids.toSet() }

    val favoritesCount: Flow<Int> = favoriteDao.observeCount()

    /** Избранные слова отдельным потоком — для экрана «Избранное». */
    val favoriteWordsFlow: Flow<List<Word>> = favoriteDao.observeWords()

    suspend fun toggleFavorite(wordId: Long): Boolean {
        val isFavorite = favoriteIdsOnce().contains(wordId)
        if (isFavorite) favoriteDao.remove(wordId) else favoriteDao.add(Favorite(wordId))
        return !isFavorite
    }

    suspend fun setFavorite(wordId: Long, favorite: Boolean) {
        if (favorite) favoriteDao.add(Favorite(wordId)) else favoriteDao.remove(wordId)
    }

    private suspend fun favoriteIdsOnce(): Set<Long> = favoriteDao.words().map { it.id }.toSet()

    suspend fun favoriteWords(): List<Word> = favoriteDao.words()

    /* ------------------- Раздача слов по выбранным папкам ------------------- */

    /**
     * Слова выбранных папок. Специальные «папки»:
     * [FAVORITES_ID] — избранные слова, [HARD_WORDS_ID] — самые слабые слова.
     */
    suspend fun wordsFor(selection: List<Long>): List<Word> {
        val result = LinkedHashMap<Long, Word>()
        val plainDecks = selection.filter { it > 0 }
        wordsOfDecks(plainDecks).forEach { result[it.id] = it }
        if (selection.contains(FAVORITES_ID)) {
            favoriteWords().forEach { result[it.id] = it }
        }
        if (selection.contains(HARD_WORDS_ID)) {
            val ids = statsDao.hardWordIds(HARD_WORDS_LIMIT)
            val byId = wordDao.getByIds(ids).associateBy { it.id }
            ids.forEach { id -> byId[id]?.let { result[it.id] = it } }
        }
        return result.values.toList()
    }

    suspend fun countFor(selection: List<Long>): Int {
        var total = 0
        selection.filter { it > 0 }.let { decks ->
            if (decks.isNotEmpty()) total += wordDao.countInDecks(decks)
        }
        if (selection.contains(FAVORITES_ID)) total += favoriteDao.words().size
        if (selection.contains(HARD_WORDS_ID)) {
            total += statsDao.hardWordIds(HARD_WORDS_LIMIT).size
        }
        return total
    }

    /**
     * Раздача слов в игру: интервальное повторение — сначала слова
     * с низкой точностью (см. [StudyOrder]).
     */
    suspend fun studyWords(
        selection: List<Long>,
        srsFirst: Boolean,
        limit: Int = Int.MAX_VALUE
    ): List<Word> {
        val pool = wordsFor(selection)
        if (pool.isEmpty()) return emptyList()
        val stats = if (srsFirst) statsDao.allWordStats().associateBy { it.wordId } else emptyMap()
        val ordered = StudyOrder.order(pool, stats, srsFirst)
        return if (limit >= ordered.size) ordered else ordered.take(limit)
    }

    /* --------------------------- Статистика --------------------------- */

    val totalWords: Flow<Int> = wordDao.observeTotalCount()

    val learnedWords: Flow<Int> = statsDao.observeLearnedWords().map { it ?: 0 }

    val gamesPlayed: Flow<Int> =
        combine(statsDao.observeGamesPlayed(), statsDao.observeGameResultsCount()) { a, b -> a + b }

    val pairsFound: Flow<Int> = statsDao.observePairsFound().map { it ?: 0 }

    val averageAccuracy: Flow<Float> = statsDao.observeAverageAccuracy().map { it ?: 0f }

    val totalSeconds: Flow<Int> =
        combine(statsDao.observeTotalSeconds(), statsDao.observeGameResultsSeconds()) { a, b ->
            (a ?: 0) + (b ?: 0)
        }

    val bestScore: Flow<Int> =
        combine(statsDao.observeBestScore(), statsDao.observeGameResultsBestScore()) { a, b ->
            maxOf(a ?: 0, b ?: 0)
        }

    val hardWordCount: Flow<Int> = statsDao.observeHardWordCount()

    fun dailyStats(days: Int): Flow<List<DailyStat>> = statsDao.observeDailyStats(days)

    fun hardWords(limit: Int): Flow<List<WordStatRow>> = statsDao.observeHardWords(limit)

    fun gamesFor(game: String): Flow<Int> = statsDao.observeGamesFor(game)

    fun bestScoreFor(game: String): Flow<Int> =
        statsDao.observeBestScoreFor(game).map { it ?: 0 }

    fun recentResults(limit: Int): Flow<List<MatchResult>> = statsDao.observeRecentResults(limit)

    fun recentGameResults(limit: Int): Flow<List<GameResult>> =
        statsDao.observeRecentGameResults(limit)

    suspend fun saveMatchResult(result: MatchResult) = statsDao.insertResult(result)

    suspend fun saveGameResult(result: GameResult) = statsDao.insertGameResult(result)

    /**
     * Обновляем «память» по каждому слову после партии.
     * [quality] — от 0 до 1 (1.0 — ответ без единой ошибки).
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

    /* ---------------------------- CSV-обмен ---------------------------- */

    /** Выгружаем весь словарь в CSV-текст. */
    suspend fun exportCsv(): String {
        val allDecks = deckDao.allDecks().associateBy { it.id }
        val rows = wordDao.allWords().map { word ->
            val deck = allDecks[word.deckId]
            CsvRow(
                deck = deck?.name ?: CsvCodec.DEFAULT_DECK,
                emoji = deck?.emoji ?: "",
                hanzi = word.hanzi,
                pinyin = word.pinyin,
                translation = word.translation
            )
        }
        return CsvCodec.encode(rows)
    }

    /**
     * Импорт CSV: папки создаются по имени, если их нет, одинаковые слова
     * (иероглиф + перевод) пропускаются, чтобы не плодить дубли.
     */
    suspend fun importCsv(text: String): CsvImportSummary {
        val rows = CsvCodec.decode(text)
        var decksCreated = 0
        var added = 0
        var skipped = 0
        val deckCache = HashMap<String, Long>()
        val existing = HashSet<String>()

        deckDao.allDecks().forEach { deck ->
            deckCache[deck.name.lowercase()] = deck.id
            wordDao.getByDeck(deck.id).forEach { existing.add(key(deck.id, it.hanzi, it.translation)) }
        }

        rows.forEach { row ->
            if (row.hanzi.isBlank() || row.translation.isBlank()) {
                skipped++
                return@forEach
            }
            val name = row.deck.ifBlank { CsvCodec.DEFAULT_DECK }
            val deckId = deckCache.getOrPut(name.lowercase()) {
                decksCreated++
                deckDao.insert(Deck(name = name, emoji = row.emoji.ifBlank { "📚" }))
            }
            val k = key(deckId, row.hanzi, row.translation)
            if (existing.contains(k)) {
                skipped++
                return@forEach
            }
            existing.add(k)
            wordDao.insert(
                Word(
                    deckId = deckId,
                    hanzi = row.hanzi.trim(),
                    pinyin = row.pinyin.trim(),
                    translation = row.translation.trim()
                )
            )
            added++
        }
        return CsvImportSummary(decksCreated = decksCreated, wordsAdded = added, wordsSkipped = skipped)
    }

    private fun key(deckId: Long, hanzi: String, translation: String) =
        "$deckId|${hanzi.trim()}|${translation.trim()}"

    companion object {
        /** Псевдо-папка «Избранное». */
        const val FAVORITES_ID = -1L

        /** Псевдо-папка «Сложные слова» (низкая точность). */
        const val HARD_WORDS_ID = -2L

        /** Сколько слабых слов максимум попадает в игру. */
        const val HARD_WORDS_LIMIT = 40
    }
}
