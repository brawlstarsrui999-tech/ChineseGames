package com.chinesegames.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Папка со словами («колода»). Пользователь может создавать их без ограничений.
 */
@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val emoji: String = "📚",
    val createdAt: Long = System.currentTimeMillis()
)

/** Сколько слов в папке (для списка папок). */
data class DeckWordCount(val deckId: Long, val count: Int)

/** Сколько слов из папки уже «выучено» (уверенно отвечали в играх). */
data class DeckLearnedCount(val deckId: Long, val count: Int)
