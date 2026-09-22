package com.chinesegames.app.data

import androidx.room.ColumnInfo
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
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * Системная папка — сюда приложение само складывает слова, выученные
     * в курсе «Поэтапное изучение». Такую папку нельзя удалить или переименовать.
     *
     * Значение по умолчанию совпадает с миграцией 2 → 3
     * (`ALTER TABLE decks ADD COLUMN isSystem INTEGER NOT NULL DEFAULT 0`).
     */
    @ColumnInfo(defaultValue = "0")
    val isSystem: Boolean = false
) {
    companion object {
        /** Имя системной папки для выученных слов. */
        const val LEARNED_DECK = "Выученное"
        const val LEARNED_EMOJI = "✅"
    }
}

/** Сколько слов в папке (для списка папок). */
data class DeckWordCount(val deckId: Long, val count: Int)

/** Сколько слов из папки уже «выучено» (уверенно отвечали в играх). */
data class DeckLearnedCount(val deckId: Long, val count: Int)
