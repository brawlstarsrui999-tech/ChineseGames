package com.chinesegames.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Query("SELECT * FROM words WHERE deckId = :deckId ORDER BY id ASC")
    fun observeByDeck(deckId: Long): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE deckId = :deckId ORDER BY id ASC")
    suspend fun getByDeck(deckId: Long): List<Word>

    @Query("SELECT * FROM words WHERE deckId IN (:deckIds) ORDER BY id ASC")
    suspend fun getByDecks(deckIds: List<Long>): List<Word>

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<Word>

    @Query("SELECT * FROM words ORDER BY deckId ASC, id ASC")
    suspend fun allWords(): List<Word>

    @Query("SELECT COUNT(*) FROM words")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE deckId IN (:deckIds)")
    suspend fun countInDecks(deckIds: List<Long>): Int

    @Insert
    suspend fun insert(word: Word): Long

    @Insert
    suspend fun insertAll(words: List<Word>)

    @Query("SELECT hanzi FROM words WHERE deckId = :deckId")
    suspend fun hanziInDeck(deckId: Long): List<String>

    @Update
    suspend fun update(word: Word)

    @Delete
    suspend fun delete(word: Word)

    @Query("DELETE FROM words WHERE deckId = :deckId")
    suspend fun deleteByDeck(deckId: Long)
}
