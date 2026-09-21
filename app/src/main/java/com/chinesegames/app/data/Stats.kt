package com.chinesegames.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Статистика по слову: как часто оно попадалось в играх и как хорошо
 * пользователь его помнит. accuracy = qualitySum / timesPlayed.
 */
@Entity(tableName = "game_word_stats")
data class GameWordStat(
    @PrimaryKey val wordId: Long,
    val timesPlayed: Int = 0,
    val timesWrong: Int = 0,
    val qualitySum: Float = 0f,
    val accuracy: Float = 0f,
    val lastPlayedAt: Long = System.currentTimeMillis()
)

/** Результат сыгранной партии «Найди пару». */
@Entity(tableName = "match_results")
data class MatchResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val playedAt: Long = System.currentTimeMillis(),
    val pairs: Int,
    val cards: Int,
    val mistakes: Int,
    val hints: Int,
    val accuracy: Float,
    val durationSeconds: Int,
    val bestCombo: Int,
    val score: Int,
    val stars: Int,
    val deckIds: String
)
