package com.chinesegames.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Сетка, которая всегда влезает в отведённое место: число столбцов подбирается
 * так, чтобы карточки были максимально крупными и при этом полностью
 * помещались на экране — без прокрутки.
 *
 * Используется во всех играх с карточками, поэтому карточек на экране
 * помещается заметно больше, чем в «резиновой» сетке.
 */
@Composable
fun FitGrid(
    count: Int,
    modifier: Modifier = Modifier,
    maxColumns: Int = 6,
    spacing: Dp = 6.dp,
    /** Во сколько раз ячейка может быть выше своей ширины. */
    maxHeightRatio: Float = 1.5f,
    content: @Composable (index: Int, cellWidth: Dp, cellHeight: Dp) -> Unit
) {
    if (count <= 0) return
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val availableWidth = maxWidth
        val availableHeight = maxHeight
        val columns = bestColumns(
            count = count,
            maxColumns = maxColumns,
            width = availableWidth.value,
            height = availableHeight.value,
            spacing = spacing.value
        )
        val rows = ceil(count.toFloat() / columns).toInt()
        val rawWidth = ((availableWidth - spacing * (columns - 1)) / columns).coerceAtLeast(28.dp)
        val rawHeight = ((availableHeight - spacing * (rows - 1)) / rows).coerceAtLeast(26.dp)
        val cellWidth = rawWidth
        val cellHeight = minOf(rawHeight, rawWidth * maxHeightRatio)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var index = 0
            repeat(rows) {
                val inRow = minOf(columns, count - index)
                if (inRow <= 0) return@repeat
                Row(
                    modifier = Modifier.height(cellHeight),
                    horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(inRow) {
                        content(index, cellWidth, cellHeight)
                        index++
                    }
                }
            }
        }
    }
}

/**
 * Подбор числа столбцов: сравниваем «квадратность» ячейки и заполненность
 * сетки, выбирая самый аккуратный вариант.
 */
private fun bestColumns(
    count: Int,
    maxColumns: Int,
    width: Float,
    height: Float,
    spacing: Float
): Int {
    var best = 1
    var bestScore = Float.MAX_VALUE
    val limit = maxColumns.coerceAtLeast(1)
    for (columns in 1..limit) {
        val cellW = (width - spacing * (columns - 1)) / columns
        if (cellW < 30f) break
        val rows = ceil(count.toFloat() / columns).toInt()
        val cellH = (height - spacing * (rows - 1)) / rows
        if (cellH < 24f) continue
        val squareness = abs((cellW / cellH) - 1f)
        val used = count.toFloat() / (columns * rows).toFloat()
        val score = squareness * 1.1f + (1f - used) * 3f
        if (score < bestScore) {
            bestScore = score
            best = columns
        }
    }
    return best
}
