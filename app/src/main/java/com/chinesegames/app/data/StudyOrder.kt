package com.chinesegames.app.data

import kotlin.random.Random

/**
 * Порядок выдачи слов в играх — интервальное повторение «на минималках»:
 * сначала идут слова с самой низкой точностью (и те, что давно не попадались),
 * а новые слова — в середине очереди, чтобы не вытеснять проблемные.
 *
 * [srsFirst] = false возвращает обычную случайную раздачу.
 */
object StudyOrder {

    /** Слова, которые давно не попадались, получают небольшую «скидку» к точности. */
    private const val UNSEEN_ACCURACY = 0.5f
    private const val JITTER = 0.07f

    fun order(
        words: List<Word>,
        stats: Map<Long, GameWordStat>,
        srsFirst: Boolean,
        random: Random = Random.Default
    ): List<Word> {
        if (!srsFirst) return words.shuffled(random)
        val shuffled = words.shuffled(random)
        return shuffled.sortedBy { priority(it, stats[it.id], random) }
    }

    private fun priority(word: Word, stat: GameWordStat?, random: Random): Float {
        val base = when {
            stat == null || stat.timesPlayed == 0 -> UNSEEN_ACCURACY
            else -> stat.accuracy
        }
        // Немного шума, чтобы одинаково слабые слова не шли всегда одним порядком.
        return base + random.nextFloat() * JITTER
    }
}
