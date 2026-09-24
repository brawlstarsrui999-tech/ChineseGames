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
    private val favoriteDao: FavoriteDao,
    private val hskDao: HskDao
) {

    /* ----------------------------- Папки ----------------------------- */

    /** Все папки: пользовательские, «Выученное», уровни курса и их разделы. */
    val decks: Flow<List<Deck>> = deckDao.observeDecks()

    /**
     * Слов в папке. У папок уровней курса («HSK 3») слова лежат в подпапках,
     * поэтому для них считаем сумму по разделам.
     */
    val wordCounts: Flow<Map<Long, Int>> = combine(
        deckDao.observeWordCounts().map { list -> list.associate { it.deckId to it.count } },
        deckDao.observeDecks()
    ) { counts, decks -> withParentTotals(counts, decks) }

    val learnedCounts: Flow<Map<Long, Int>> = combine(
        deckDao.observeLearnedCounts().map { list -> list.associate { it.deckId to it.count } },
        deckDao.observeDecks()
    ) { counts, decks -> withParentTotals(counts, decks) }

    /** Добавляем родительским папкам сумму по их подпапкам. */
    private fun withParentTotals(counts: Map<Long, Int>, decks: List<Deck>): Map<Long, Int> {
        val result = HashMap(counts)
        decks.forEach { deck ->
            val parent = deck.parentId ?: return@forEach
            val own = counts[deck.id] ?: 0
            if (own > 0) result[parent] = (result[parent] ?: 0) + own
        }
        return result
    }

    /** Подпапки (разделы) папки уровня курса. */
    suspend fun children(parentId: Long): List<Deck> = deckDao.children(parentId)

    /** Папка уровня курса в словаре («HSK 3»), если уже создана. */
    suspend fun courseLevelDeck(level: Int): Deck? = deckDao.getByCourseKey(Deck.levelKey(level))

    val deckStats: Flow<List<DeckStat>> = statsDao.observeDeckStats()

    fun deckFlow(id: Long): Flow<Deck?> = deckDao.observeDeck(id)

    suspend fun getDeck(id: Long): Deck? = deckDao.getDeck(id)

    suspend fun createDeck(name: String, emoji: String): Long =
        deckDao.insert(Deck(name = name.trim(), emoji = emoji))

    /**
     * Системные папки («Выученное», папки курса) переименовывать нельзя —
     * иначе курс и словарь потеряют их. Для них сохраняем прежние имя и эмодзи.
     */
    suspend fun updateDeck(deck: Deck) {
        if (deck.isSystem) {
            val current = deckDao.getDeck(deck.id) ?: return
            deckDao.update(deck.copy(name = current.name, emoji = current.emoji))
        } else {
            deckDao.update(deck)
        }
    }

    /**
     * Удаляем папку вместе со статистикой и избранным по её словам.
     * Системные папки («Выученное», «HSK 1» … «HSK 7») удалить нельзя.
     */
    suspend fun deleteDeck(deck: Deck) {
        if (deck.isSystem || deck.isCourse) return
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
        val plainDecks = expandFolders(selection.filter { it > 0 })
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
        expandFolders(selection.filter { it > 0 }).let { decks ->
            if (decks.isNotEmpty()) total += wordDao.countInDecks(decks)
        }
        if (selection.contains(FAVORITES_ID)) total += favoriteDao.words().size
        if (selection.contains(HARD_WORDS_ID)) {
            total += statsDao.hardWordIds(HARD_WORDS_LIMIT).size
        }
        return total
    }

    /**
     * Папка уровня курса («HSK 3») сама слов не хранит — они в подпапках.
     * Выбор такой папки означает «все её разделы».
     */
    private suspend fun expandFolders(deckIds: List<Long>): List<Long> {
        if (deckIds.isEmpty()) return deckIds
        val children = deckDao.childrenOf(deckIds).map { it.id }
        if (children.isEmpty()) return deckIds
        return (deckIds + children).distinct()
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

    suspend fun saveMatchResult(result: MatchResult) {
        statsDao.insertResult(result)
        GameEvents.gameFinished(GameOutcome(result.accuracy, result.stars, result.score))
    }

    suspend fun saveGameResult(result: GameResult) {
        statsDao.insertGameResult(result)
        GameEvents.gameFinished(GameOutcome(result.accuracy, result.stars, result.score))
    }

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


    /* --------------------- Курс «Поэтапное изучение» --------------------- */

    /** Прогресс всех групп, экзаменов и разделов предложений — одним потоком. */
    val hskGroups: Flow<List<HskGroupProgress>> = hskDao.observeGroups()

    val hskExams: Flow<List<HskExam>> = hskDao.observeExams()

    val hskSentenceTopics: Flow<List<HskSentenceProgress>> = hskDao.observeSentenceTopics()

    val learnedGroupsCount: Flow<Int> = hskDao.observeLearnedGroups().map { it ?: 0 }

    /**
     * Системная папка «Выученное»: создаётся один раз и дальше только пополняется.
     */
    suspend fun learnedDeckId(): Long {
        deckDao.getByName(Deck.LEARNED_DECK)?.let { deck ->
            if (!deck.isSystem) {
                deckDao.update(deck.copy(isSystem = true, emoji = Deck.LEARNED_EMOJI))
            }
            return deck.id
        }
        return deckDao.insert(
            Deck(
                name = Deck.LEARNED_DECK,
                emoji = Deck.LEARNED_EMOJI,
                isSystem = true
            )
        )
    }

    /**
     * Слова выученной группы уезжают в папку «Выученное».
     * Дубликаты (тот же иероглиф и перевод) не добавляем.
     */
    suspend fun addLearnedWords(words: List<HskWordData>): Int {
        if (words.isEmpty()) return 0
        val deckId = learnedDeckId()
        val existing = wordDao.getByDeck(deckId)
            .map { it.hanzi.trim() to it.translation.trim() }
            .toHashSet()
        var added = 0
        words.forEach { word ->
            val key = word.hanzi.trim() to word.translation.trim()
            if (existing.contains(key)) return@forEach
            existing.add(key)
            wordDao.insert(
                Word(
                    deckId = deckId,
                    hanzi = word.hanzi.trim(),
                    pinyin = word.pinyin.trim(),
                    translation = word.translation.trim()
                )
            )
            added++
        }
        return added
    }

    suspend fun groupProgress(key: String): HskGroupProgress? = hskDao.getGroup(key)

    /** Записываем результат игры в группе: бит игры + попытка. */
    suspend fun registerGroupGame(
        key: String,
        level: Int,
        topicId: String,
        groupIndex: Int,
        gameBit: Int,
        passed: Boolean,
        score: Int
    ): HskGroupProgress {
        val old = hskDao.getGroup(key)
        val mask = if (passed) (old?.passedMask ?: 0) or gameBit else (old?.passedMask ?: 0)
        val learned = Integer.bitCount(mask) >= HskCourse.GAMES_PER_GROUP
        val progress = HskGroupProgress(
            groupKey = key,
            level = level,
            topicId = topicId,
            groupIndex = groupIndex,
            passedMask = mask,
            attempts = (old?.attempts ?: 0) + 1,
            bestScore = maxOf(old?.bestScore ?: 0, score),
            learned = learned,
            updatedAt = System.currentTimeMillis()
        )
        hskDao.upsertGroup(progress)
        return progress
    }

    suspend fun examResult(level: Int): HskExam? = hskDao.getExam(level)

    suspend fun registerExam(level: Int, correct: Int, asked: Int, score: Int): HskExam {
        val accuracy = if (asked == 0) 0f else correct.toFloat() / asked
        val passed = accuracy >= HskCourse.EXAM_PASS
        val old = hskDao.getExam(level)
        val exam = HskExam(
            level = level,
            passed = passed || (old?.passed == true),
            bestAccuracy = maxOf(old?.bestAccuracy ?: 0f, accuracy),
            bestScore = maxOf(old?.bestScore ?: 0, score),
            bestCorrect = maxOf(old?.bestCorrect ?: 0, correct),
            asked = asked,
            takenAt = System.currentTimeMillis()
        )
        hskDao.upsertExam(exam)
        return exam
    }

    suspend fun sentenceProgress(key: String): HskSentenceProgress? = hskDao.getSentenceTopic(key)

    suspend fun registerSentenceGame(
        key: String,
        level: Int,
        topicId: String,
        gameBit: Int,
        passed: Boolean
    ): HskSentenceProgress {
        val old = hskDao.getSentenceTopic(key)
        val mask = if (passed) (old?.passedMask ?: 0) or gameBit else (old?.passedMask ?: 0)
        val progress = HskSentenceProgress(
            topicKey = key,
            level = level,
            topicId = topicId,
            passedMask = mask,
            attempts = (old?.attempts ?: 0) + 1,
            updatedAt = System.currentTimeMillis()
        )
        hskDao.upsertSentenceTopic(progress)
        return progress
    }

    /* ---------------------------- CSV-обмен ---------------------------- */

    /**
     * Выгружаем словарь пользователя в CSV-текст. Папки курса («HSK 1» … «HSK 7»)
     * не выгружаем: они собираются из материала приложения автоматически.
     */
    suspend fun exportCsv(): String {
        val allDecks = deckDao.allDecks().filterNot { it.isCourse }.associateBy { it.id }
        val rows = wordDao.allWords().filter { allDecks.containsKey(it.deckId) }.map { word ->
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

        // папки курса в импорте не участвуют: слова в них менять нельзя,
        // а одноимённая папка из файла («HSK 1») станет обычной папкой пользователя
        deckDao.allDecks().filterNot { it.isCourse }.forEach { deck ->
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
