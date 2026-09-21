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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWordStat(stat: GameWordStat)

    @Query("DELETE FROM game_word_stats WHERE wordId IN (:wordIds)")
    suspend fun deleteWordStats(wordIds: List<Long>)

    @Insert
    suspend fun insertResult(result: MatchResult)

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
}
