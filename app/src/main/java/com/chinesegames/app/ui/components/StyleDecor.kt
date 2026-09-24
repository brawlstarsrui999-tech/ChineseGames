package com.chinesegames.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.CgPaletteState
import kotlin.math.sin
import kotlin.random.Random

/* =========================================================================
 *  Украшения стилевых наборов (см. data/StylePack).
 *
 *  «Китайский дракон»: по верху экрана медленно летит пиксельный дракон,
 *  из углов свисают качающиеся фонарики, сверху сыплются монеты и узлы.
 *
 *  «Аниме»: слева — Вагури, справа — Сукуна в теле Мэгуми; оба чуть
 *  покачиваются, вокруг падают звёзды, блёстки и сердечки.
 *
 *  Всё рисуется кодом на Canvas и лежит ПОД содержимым экрана.
 * ========================================================================= */

/** Секунды с момента появления слоя — общий «таймер» для анимаций. */
@Composable
private fun rememberFrameSeconds(): Float {
    var timeSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> timeSec = (now - startNanos) / 1_000_000_000f }
        }
    }
    return timeSec
}

/** Спрайты, которые сыплются в стиле «Китайский дракон». */
val ChinaFallingSprites: List<PixelSprite> = listOf(CoinSprite, KnotSprite, SparkSprite, CoinSprite)

/** Спрайты, которые сыплются в аниме-стиле. */
val AnimeFallingSprites: List<PixelSprite> = listOf(StarSprite, SparkSprite, HeartSprite, SakuraSprite, StarSprite)

/* ----------------------------- Китайский дракон ----------------------------- */

@Composable
fun PixelChinaLayer(modifier: Modifier = Modifier, alpha: Float = 1f) {
    val t = rememberFrameSeconds()
    val night = CgPaletteState.night

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val d = density

        // Дракон: пролетает слева направо за ~26 секунд, слегка «дышит» по вертикали.
        val dragonHeight = 58f * d
        val dragonWidth = dragonHeight * DragonSprite.aspect
        val cycle = 26f
        val progress = (t / cycle) % 1f
        val x = -dragonWidth + progress * (w + dragonWidth * 2f)
        val y = h * 0.12f + sin(t * 1.1f) * 10f * d
        drawSprite(
            sprite = DragonSprite,
            topLeft = Offset(x, y),
            heightPx = dragonHeight,
            alpha = (if (night) 0.85f else 0.75f) * alpha
        )

        // Второй, дальний дракон — меньше, бледнее, летит в другую сторону.
        val farHeight = 30f * d
        val farWidth = farHeight * DragonSprite.aspect
        val farProgress = ((t + 9f) / 41f) % 1f
        val farX = w + farWidth - farProgress * (w + farWidth * 2f)
        scale(scaleX = -1f, scaleY = 1f, pivot = Offset(farX + farWidth / 2f, h * 0.30f + farHeight / 2f)) {
            drawSprite(
                sprite = DragonSprite,
                topLeft = Offset(farX, h * 0.30f + sin(t * 0.8f + 2f) * 6f * d),
                heightPx = farHeight,
                alpha = 0.32f * alpha
            )
        }

        // Фонарики на верёвочках в верхних углах: качаются с разной фазой.
        val lanternHeight = 30f * d
        val hangs = listOf(
            Triple(0.06f, 0.05f, 0f),
            Triple(0.15f, 0.02f, 1.3f),
            Triple(0.85f, 0.02f, 2.1f),
            Triple(0.94f, 0.05f, 0.7f)
        )
        hangs.forEach { (fx, fy, phase) ->
            val swing = sin(t * 1.6f + phase) * 7f
            val px = fx * w
            val py = fy * h
            rotate(degrees = swing, pivot = Offset(px + lanternHeight * LanternSprite.aspect / 2f, py)) {
                drawLine(
                    color = CG.pixelGold.copy(alpha = 0.55f * alpha),
                    start = Offset(px + lanternHeight * LanternSprite.aspect / 2f, py - 40f * d),
                    end = Offset(px + lanternHeight * LanternSprite.aspect / 2f, py),
                    strokeWidth = 2f
                )
                drawSprite(
                    sprite = LanternSprite,
                    topLeft = Offset(px, py),
                    heightPx = lanternHeight,
                    alpha = 0.9f * alpha
                )
            }
        }

        // Стопки монет по нижним углам.
        val coinH = 12f * d
        for (i in 0 until 3) {
            drawSprite(CoinSprite, Offset(14f * d + i * coinH * 0.55f, h * 0.90f - i * coinH * 0.35f), coinH, 0.55f * alpha)
            drawSprite(CoinSprite, Offset(w - 14f * d - coinH - i * coinH * 0.55f, h * 0.90f - i * coinH * 0.35f), coinH, 0.55f * alpha)
        }

        // Мягкое золотое свечение по нижнему краю — как отблеск фонарей.
        drawGoldHaze(alpha)
    }
}

private fun DrawScope.drawGoldHaze(alpha: Float) {
    val steps = 6
    for (i in 0 until steps) {
        val a = 0.05f * (1f - i / steps.toFloat()) * alpha
        drawRect(
            color = CG.pixelGold.copy(alpha = a),
            topLeft = Offset(0f, size.height - (i + 1) * 12f * density),
            size = androidx.compose.ui.geometry.Size(size.width, 12f * density)
        )
    }
}

/* --------------------------------- Аниме --------------------------------- */

private data class Twinkle(val x: Float, val y: Float, val phase: Float, val size: Float)

@Composable
fun PixelAnimeLayer(modifier: Modifier = Modifier, alpha: Float = 1f) {
    val t = rememberFrameSeconds()
    val twinkles = remember {
        val rnd = Random(91)
        List(18) { Twinkle(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat() * 6.28f, 6f + rnd.nextFloat() * 8f) }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val d = density

        // Мерцающие звёздочки по всему экрану.
        twinkles.forEach { tw ->
            val pulse = (sin(t * 2.2f + tw.phase) + 1f) / 2f
            drawSprite(
                sprite = if (pulse > 0.5f) SparkSprite else StarSprite,
                topLeft = Offset(tw.x * w, tw.y * h),
                heightPx = tw.size * d * (0.7f + 0.3f * pulse),
                alpha = (0.15f + 0.45f * pulse) * alpha
            )
        }

        // Персонажи: Вагури слева, Сукуна-в-Мэгуми справа. Покачиваются в противофазе.
        val charHeight = 96f * d
        val baseY = h * 0.60f
        val waguriBob = sin(t * 1.4f) * 4f * d
        val sukunaBob = sin(t * 1.4f + 3.14f) * 4f * d

        drawSprite(
            sprite = WaguriSprite,
            topLeft = Offset(6f * d, baseY + waguriBob),
            heightPx = charHeight,
            alpha = 0.92f * alpha
        )
        val sukunaWidth = charHeight * SukunaSprite.aspect
        drawSprite(
            sprite = SukunaSprite,
            topLeft = Offset(w - sukunaWidth - 6f * d, baseY + sukunaBob),
            heightPx = charHeight,
            alpha = 0.92f * alpha
        )

        // Сердечки у Вагури и искры у Сукуны — по одной, всплывают и тают.
        val floatPhase = (t * 0.5f) % 1f
        drawSprite(
            sprite = HeartSprite,
            topLeft = Offset(charHeight * WaguriSprite.aspect + 8f * d, baseY - floatPhase * 30f * d),
            heightPx = 10f * d,
            alpha = (1f - floatPhase) * 0.8f * alpha
        )
        val sparkPhase = (t * 0.7f + 0.4f) % 1f
        drawSprite(
            sprite = SparkSprite,
            topLeft = Offset(w - sukunaWidth - 20f * d, baseY - sparkPhase * 26f * d),
            heightPx = 10f * d,
            alpha = (1f - sparkPhase) * 0.8f * alpha,
            tint = CG.pixelCrimson
        )
    }
}
