package com.chinesegames.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Папка «Избранное»: звёздочки на словах из любых папок. */
@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE wordId = :wordId")
    suspend fun remove(wordId: Long)

    @Query("DELETE FROM favorites WHERE wordId IN (:wordIds)")
    suspend fun removeAll(wordIds: List<Long>)

    @Query("SELECT wordId FROM favorites ORDER BY addedAt DESC")
    fun observeIds(): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM favorites")
    fun observeCount(): Flow<Int>

    @Query(
        """
        SELECT w.* FROM words w
        INNER JOIN favorites f ON f.wordId = w.id
        ORDER BY f.addedAt DESC
        """
    )
    suspend fun words(): List<Word>

    @Query(
        """
        SELECT w.* FROM words w
        INNER JOIN favorites f ON f.wordId = w.id
        ORDER BY f.addedAt DESC
        """
    )
    fun observeWords(): Flow<List<Word>>
}
