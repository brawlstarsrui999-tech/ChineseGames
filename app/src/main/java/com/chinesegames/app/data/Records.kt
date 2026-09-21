package com.chinesegames.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Избранное слово («папка Избранное»). Отдельная таблица, чтобы звёздочку
 * можно было поставить слову из любой папки, не меняя само слово.
 */
@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey val wordId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Результат партии любой игры кроме «Найди пару»
 * (у той своя таблица [MatchResult], чтобы не ломать старые данные).
 */
@Entity(tableName = "game_results")
data class GameResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** Идентификатор игры: bubble, sprint, audio, falling, chain, pinyin, memory. */
    val game: String,
    val playedAt: Long = System.currentTimeMillis(),
    /** Сколько слов спросили за партию. */
    val asked: Int = 0,
    /** Сколько ответов были верными. */
    val correct: Int = 0,
    val mistakes: Int = 0,
    val accuracy: Float = 0f,
    val durationSeconds: Int = 0,
    val bestCombo: Int = 0,
    val score: Int = 0,
    val stars: Int = 0,
    val deckIds: String = ""
)

/** Сколько избранных слов (для счётчика в списке папок). */
data class FavoriteCount(val count: Int)

/** Сколько «сложных» слов — тех, что отвечаются хуже всего. */
data class HardWordCount(val count: Int)

/** Слово вместе с его статистикой — для списка «слабых слов» и подсказок. */
data class WordStatRow(
    val wordId: Long,
    val deckId: Long,
    val hanzi: String,
    val pinyin: String,
    val translation: String,
    val accuracy: Float,
    val timesPlayed: Int,
    val timesWrong: Int
)

/** Статистика по одному дню (для графика «по дням»). */
data class DailyStat(
    val day: String,
    val sessions: Int,
    val seconds: Int,
    val answers: Int,
    val mistakes: Int,
    val score: Int
)

/** Статистика по папке: сколько слов, сколько уверенно знаю, средняя точность. */
data class DeckStat(
    val deckId: Long,
    val total: Int,
    val learned: Int,
    val averageAccuracy: Float
)
