package com.chinesegames.app.ui

import kotlin.math.abs

/** Русские формы множественного числа: pluralRu(3, "слово", "слова", "слов"). */
fun pluralRu(count: Int, one: String, few: String, many: String): String {
    val n = abs(count) % 100
    if (n in 11..14) return many
    return when (n % 10) {
        1 -> one
        2, 3, 4 -> few
        else -> many
    }
}

fun wordsLabel(count: Int): String = "$count ${pluralRu(count, "слово", "слова", "слов")}"

fun pairsLabel(count: Int): String = "$count ${pluralRu(count, "пара", "пары", "пар")}"

fun cardsLabel(count: Int): String = "$count ${pluralRu(count, "карточка", "карточки", "карточек")}"

fun decksLabel(count: Int): String = "$count ${pluralRu(count, "папка", "папки", "папок")}"

fun gamesLabel(count: Int): String = "$count ${pluralRu(count, "партия", "партии", "партий")}"

/** 01:35 */
fun formatTime(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val minutes = safe / 60
    val seconds = safe % 60
    return "%02d:%02d".format(minutes, seconds)
}

/** Проценты для точности. */
fun formatPercent(value: Float): String = "${(value.coerceIn(0f, 1f) * 100).toInt()}%"
