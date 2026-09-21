package com.chinesegames.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    @Query("SELECT * FROM game_word_stats WHERE wordId = :wordId")
    suspend fun getWordStat(wordId: Long): GameWordStat?

    @Query("SELECT * FROM game_word_stats")
    suspend fun allWordStats(): List<GameWordStat>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWordStat(stat: GameWordStat)

    @Query("DELETE FROM game_word_stats WHERE wordId IN (:wordIds)")
    suspend fun deleteWordStats(wordIds: List<Long>)

    @Insert
    suspend fun insertResult(result: MatchResult)

    @Insert
    suspend fun insertGameResult(result: GameResult)

    /* ---------------- Общая статистика (для главного экрана и хаба игр) ---------------- */

    @Query("SELECT COUNT(*) FROM match_results")
    fun observeGamesPlayed(): Flow<Int>

    @Query("SELECT SUM(pairs) FROM match_results")
    fun observePairsFound(): Flow<Int?>

    @Query("SELECT AVG(accuracy) FROM match_results")
    fun observeAverageAccuracy(): Flow<Float?>

    @Query("SELECT SUM(durationSeconds) FROM match_results")
    fun observeTotalSeconds(): Flow<Int?>

    @Query("SELECT MAX(score) FROM match_results")
    fun observeBestScore(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM game_word_stats WHERE accuracy >= 0.75")
    fun observeLearnedWords(): Flow<Int?>

    /* ------------------- Партии остальных игр (новые режимы) ------------------- */

    @Query("SELECT COUNT(*) FROM game_results")
    fun observeGameResultsCount(): Flow<Int>

    @Query("SELECT SUM(correct) FROM game_results")
    fun observeGameResultsCorrect(): Flow<Int?>

    @Query("SELECT SUM(durationSeconds) FROM game_results")
    fun observeGameResultsSeconds(): Flow<Int?>

    @Query("SELECT AVG(accuracy) FROM game_results")
    fun observeGameResultsAccuracy(): Flow<Float?>

    @Query("SELECT MAX(score) FROM game_results")
    fun observeGameResultsBestScore(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM game_results WHERE game = :game")
    fun observeGamesFor(game: String): Flow<Int>

    @Query("SELECT MAX(score) FROM game_results WHERE game = :game")
    fun observeBestScoreFor(game: String): Flow<Int?>

    @Query("SELECT COUNT(*) FROM game_results WHERE game = :game AND playedAt >= :since")
    fun observeGamesForSince(game: String, since: Long): Flow<Int>

    /* ---------------------------- Интервальное повторение ---------------------------- */

    /** Слова, которые отвечаются хуже всего — с них начинаем раздачу. */
    @Query(
        """
        SELECT wordId FROM game_word_stats
        WHERE timesPlayed > 0
        ORDER BY accuracy ASC, timesWrong DESC, lastPlayedAt ASC
        LIMIT :limit
        """
    )
    suspend fun hardWordIds(limit: Int): List<Long>

    @Query("SELECT COUNT(*) FROM game_word_stats WHERE timesPlayed > 0 AND accuracy < 0.75")
    fun observeHardWordCount(): Flow<Int>

    @Query(
        """
        SELECT w.id AS wordId, w.deckId AS deckId, w.hanzi AS hanzi, w.pinyin AS pinyin,
               w.translation AS translation, s.accuracy AS accuracy,
               s.timesPlayed AS timesPlayed, s.timesWrong AS timesWrong
        FROM words w
        INNER JOIN game_word_stats s ON s.wordId = w.id
        WHERE s.timesPlayed > 0
        ORDER BY s.accuracy ASC, s.timesWrong DESC
        LIMIT :limit
        """
    )
    fun observeHardWords(limit: Int): Flow<List<WordStatRow>>

    /* ------------------------------ Статистика по дням ------------------------------ */

    @Query(
        """
        SELECT day AS day,
               COUNT(*) AS sessions,
               SUM(seconds) AS seconds,
               SUM(answers) AS answers,
               SUM(mistakes) AS mistakes,
               SUM(score) AS score
        FROM (
            SELECT strftime('%Y-%m-%d', playedAt / 1000, 'unixepoch', 'localtime') AS day,
                   durationSeconds AS seconds,
                   pairs AS answers,
                   mistakes AS mistakes,
                   score AS score
            FROM match_results
            UNION ALL
            SELECT strftime('%Y-%m-%d', playedAt / 1000, 'unixepoch', 'localtime') AS day,
                   durationSeconds AS seconds,
                   asked AS answers,
                   mistakes AS mistakes,
                   score AS score
            FROM game_results
        )
        GROUP BY day
        ORDER BY day DESC
        LIMIT :days
        """
    )
    fun observeDailyStats(days: Int): Flow<List<DailyStat>>

    /* ------------------------------ Статистика по папкам ----------------------------- */

    @Query(
        """
        SELECT w.deckId AS deckId,
               COUNT(*) AS total,
               SUM(CASE WHEN s.accuracy >= 0.75 THEN 1 ELSE 0 END) AS learned,
               AVG(COALESCE(s.accuracy, 0.0)) AS averageAccuracy
        FROM words w
        LEFT JOIN game_word_stats s ON s.wordId = w.id
        GROUP BY w.deckId
        """
    )
    fun observeDeckStats(): Flow<List<DeckStat>>

    /** Отдаём наружу, чтобы «Ещё раз» в партии могло писать результат тем же путём. */
    @Query("SELECT * FROM match_results ORDER BY playedAt DESC LIMIT :limit")
    fun observeRecentResults(limit: Int): Flow<List<MatchResult>>

    @Query("SELECT * FROM game_results ORDER BY playedAt DESC LIMIT :limit")
    fun observeRecentGameResults(limit: Int): Flow<List<GameResult>>
}
