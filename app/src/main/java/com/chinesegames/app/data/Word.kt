package com.chinesegames.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Слово внутри папки: иероглиф, пиньинь и перевод.
 * Всё вписывает сам пользователь — приложение ничего не подсказывает.
 */
@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deckId")]
)
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val deckId: Long,
    val hanzi: String,
    val pinyin: String,
    val translation: String
)
