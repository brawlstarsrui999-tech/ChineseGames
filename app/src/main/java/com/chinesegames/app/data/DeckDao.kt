package com.chinesegames.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {

    @Query("SELECT * FROM decks ORDER BY createdAt ASC")
    fun observeDecks(): Flow<List<Deck>>

    @Query("SELECT * FROM decks WHERE id = :id")
    fun observeDeck(id: Long): Flow<Deck?>

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun getDeck(id: Long): Deck?

    @Insert
    suspend fun insert(deck: Deck): Long

    @Update
    suspend fun update(deck: Deck)

    @Delete
    suspend fun delete(deck: Deck)

    @Query("SELECT deckId, COUNT(*) AS count FROM words GROUP BY deckId")
    fun observeWordCounts(): Flow<List<DeckWordCount>>

    @Query(
        """
        SELECT w.deckId AS deckId, COUNT(*) AS count
        FROM words w
        INNER JOIN game_word_stats s ON s.wordId = w.id
        WHERE s.accuracy >= 0.75
        GROUP BY w.deckId
        """
    )
    fun observeLearnedCounts(): Flow<List<DeckLearnedCount>>
}
