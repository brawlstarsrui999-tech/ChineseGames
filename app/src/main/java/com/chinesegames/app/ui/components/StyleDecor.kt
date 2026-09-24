package com.chinesegames.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.chinesegames.app.R
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.CgPaletteState
import kotlin.math.sin
import kotlin.random.Random

/* =========================================================================
 *  Украшения стилевых наборов (см. data/StylePack).
 *
 *  «Китайский дракон» использует детальную иллюстрацию, а не старый
 *  пиксельный силуэт: дракон медленно проплывает над фонариками и монетами.
 *
 *  «Розовый клевер» — гладкий розовый сад: большие клеверные силуэты и
 *  листья, которые плавно кружатся по экрану. Всё лежит под UI-контентом.
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

/* ----------------------------- Китайский дракон ----------------------------- */

/**
 * Детальная, чуть менее «пиксельная» иллюстрация дракона с лёгким полётом.
 * Локальный PNG создаётся специально для приложения и показан с прозрачностью,
 * поэтому не перекрывает элементы интерфейса.
 */
@Composable
fun ChinaDragonLayer(modifier: Modifier = Modifier, alpha: Float = 1f) {
    val t = rememberFrameSeconds()
    val night = CgPaletteState.night
    val dragonAlpha = (if (night) 0.82f else 0.72f) * alpha

    Box(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.china_dragon),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .align(Alignment.TopCenter)
                .alpha(dragonAlpha)
                .graphicsLayer {
                    translationX = sin(t * 0.36f) * 10.dp.toPx()
                    translationY = sin(t * 1.05f) * 4.dp.toPx()
                    rotationZ = sin(t * 0.45f) * 0.55f
                }
        )
        ChinaOrnamentLayer(modifier = Modifier.fillMaxSize(), alpha = alpha)
    }
}

/** Фонарики, монеты и золотой отблеск поверх плавного дракона. */
@Composable
private fun ChinaOrnamentLayer(modifier: Modifier = Modifier, alpha: Float) {
    val t = rememberFrameSeconds()
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val d = density

        // Фонарики на верёвочках в верхних углах: качаются с разной фазой.
        val lanternHeight = 30f * d
        val hangs = listOf(
            Triple(0.05f, 0.05f, 0f),
            Triple(0.15f, 0.02f, 1.3f),
            Triple(0.85f, 0.02f, 2.1f),
            Triple(0.94f, 0.05f, 0.7f)
        )
        hangs.forEach { (fx, fy, phase) ->
            val swing = sin(t * 1.6f + phase) * 7f
            val px = fx * w
            val py = fy * h
            rotate(degrees = swing, pivot = androidx.compose.ui.geometry.Offset(px + lanternHeight * LanternSprite.aspect / 2f, py)) {
                drawLine(
                    color = CG.pixelGold.copy(alpha = 0.55f * alpha),
                    start = androidx.compose.ui.geometry.Offset(px + lanternHeight * LanternSprite.aspect / 2f, py - 40f * d),
                    end = androidx.compose.ui.geometry.Offset(px + lanternHeight * LanternSprite.aspect / 2f, py),
                    strokeWidth = 2f
                )
                drawSprite(LanternSprite, androidx.compose.ui.geometry.Offset(px, py), lanternHeight, 0.9f * alpha)
            }
        }

        val coinH = 12f * d
        for (i in 0 until 3) {
            drawSprite(CoinSprite, androidx.compose.ui.geometry.Offset(14f * d + i * coinH * 0.55f, h * 0.90f - i * coinH * 0.35f), coinH, 0.55f * alpha)
            drawSprite(CoinSprite, androidx.compose.ui.geometry.Offset(w - 14f * d - coinH - i * coinH * 0.55f, h * 0.90f - i * coinH * 0.35f), coinH, 0.55f * alpha)
        }
        drawGoldHaze(alpha)
    }
}

private fun DrawScope.drawGoldHaze(alpha: Float) {
    val steps = 6
    for (i in 0 until steps) {
        val a = 0.05f * (1f - i / steps.toFloat()) * alpha
        drawRect(
            color = CG.pixelGold.copy(alpha = a),
            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - (i + 1) * 12f * density),
            size = androidx.compose.ui.geometry.Size(size.width, 12f * density)
        )
    }
}

/* ------------------------------ Розовый клевер ----------------------------- */

private data class CloverLeaf(
    val x: Float,
    val start: Float,
    val speed: Float,
    val drift: Float,
    val size: Float,
    val rotation: Float,
    val alpha: Float
)

/**
 * Непиксельный розовый сад клевера: крупные прозрачные клеверы на фоне и
 * 22 плавно падающих листа. Скорость низкая, поэтому стиль не мешает чтению.
 */
@Composable
fun CloverGardenLayer(modifier: Modifier = Modifier, alpha: Float = 1f) {
    val t = rememberFrameSeconds()
    val leaves = remember {
        val random = Random(4040)
        List(22) {
            CloverLeaf(
                x = random.nextFloat(),
                start = random.nextFloat(),
                speed = 0.030f + random.nextFloat() * 0.052f,
                drift = 0.018f + random.nextFloat() * 0.038f,
                size = 12f + random.nextFloat() * 22f,
                rotation = -60f + random.nextFloat() * 120f,
                alpha = 0.20f + random.nextFloat() * 0.35f
            )
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val d = density

        // Большие размытые «четырёхлистники» статично украшают сад.
        val backgroundClovers = listOf(
            Triple(0.08f, 0.22f, 52f), Triple(0.88f, 0.16f, 44f),
            Triple(0.13f, 0.76f, 58f), Triple(0.88f, 0.69f, 64f),
            Triple(0.50f, 0.50f, 34f)
        )
        backgroundClovers.forEachIndexed { index, (fx, fy, radius) ->
            rotate(sin(t * 0.24f + index) * 6f, androidx.compose.ui.geometry.Offset(fx * w, fy * h)) {
                drawClover(
                    center = androidx.compose.ui.geometry.Offset(fx * w, fy * h),
                    radius = radius * d,
                    fill = Color(0xFFFFB5D9).copy(alpha = 0.09f * alpha),
                    outline = Color(0xFFF472B6).copy(alpha = 0.16f * alpha)
                )
            }
        }

        leaves.forEach { leaf ->
            val progress = (leaf.start + t * leaf.speed) % 1f
            val y = progress * (h + 120f * d) - 60f * d
            val x = leaf.x * w + sin(t * 0.62f + leaf.start * 8f) * leaf.drift * w
            val fade = when {
                progress < 0.09f -> progress / 0.09f
                progress > 0.90f -> (1f - progress) / 0.10f
                else -> 1f
            }
            rotate(
                degrees = leaf.rotation + sin(t * 0.9f + leaf.start * 7f) * 30f,
                pivot = androidx.compose.ui.geometry.Offset(x, y)
            ) {
                drawClover(
                    center = androidx.compose.ui.geometry.Offset(x, y),
                    radius = leaf.size * d,
                    fill = Color(0xFFFFA8D1).copy(alpha = leaf.alpha * fade * alpha),
                    outline = Color(0xFFE754A3).copy(alpha = leaf.alpha * 0.85f * fade * alpha)
                )
            }
        }
    }
}

/** Четырёхлистник из плавных листьев-сердец: не пиксельный, хорошо виден на розовом фоне. */
private fun DrawScope.drawClover(
    center: androidx.compose.ui.geometry.Offset,
    radius: Float,
    fill: Color,
    outline: Color
) {
    val leafRadius = radius * 0.58f
    val distance = radius * 0.43f
    listOf(
        androidx.compose.ui.geometry.Offset(0f, -distance),
        androidx.compose.ui.geometry.Offset(distance, 0f),
        androidx.compose.ui.geometry.Offset(0f, distance),
        androidx.compose.ui.geometry.Offset(-distance, 0f)
    ).forEach { offset ->
        drawCircle(fill, leafRadius, center + offset)
        drawCircle(outline, leafRadius, center + offset, style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.07f))
    }
    drawCircle(Color(0xFFFFD5E8).copy(alpha = fill.alpha), radius * 0.22f, center)
    drawLine(
        color = outline,
        start = center + androidx.compose.ui.geometry.Offset(radius * 0.18f, radius * 0.50f),
        end = center + androidx.compose.ui.geometry.Offset(radius * 0.66f, radius * 1.05f),
        strokeWidth = radius * 0.10f
    )
}
