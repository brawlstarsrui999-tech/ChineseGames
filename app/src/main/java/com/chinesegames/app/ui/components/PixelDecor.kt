package com.chinesegames.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.PixelType
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary
import com.chinesegames.app.ui.theme.VividPurple
import kotlin.math.sin
import kotlin.random.Random

/* =========================================================================
 *  Анимешно-пиксельные декорации: лепестки, облака, фонарики, рамочки.
 *  Всё рисуется кодом (Canvas), без картинок — поэтому выглядит одинаково
 *  чётко на любом экране и весит ноль байт.
 * ========================================================================= */

private data class Petal(
    val x: Float,
    val start: Float,
    val speed: Float,
    val sway: Float,
    val size: Float,
    val spin: Float,
    val alpha: Float,
    val spriteIndex: Int
)

/** Медленно падающие пиксельные лепестки и искры — «живой» фон приложения. */
@Composable
fun PixelPetalLayer(
    modifier: Modifier = Modifier,
    count: Int = 12,
    alpha: Float = 1f
) {
    val petals = remember(count) {
        val rnd = Random(4242)
        List(count) {
            Petal(
                x = rnd.nextFloat(),
                start = rnd.nextFloat(),
                speed = 0.035f + rnd.nextFloat() * 0.05f,
                sway = 0.01f + rnd.nextFloat() * 0.035f,
                size = 9f + rnd.nextFloat() * 11f,
                spin = if (rnd.nextBoolean()) 18f else -14f,
                alpha = 0.20f + rnd.nextFloat() * 0.35f,
                spriteIndex = rnd.nextInt(DecorSprites.size)
            )
        }
    }

    var timeSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> timeSec = (now - startNanos) / 1_000_000_000f }
        }
    }

    Canvas(modifier = modifier) {
        petals.forEach { p ->
            val progress = (timeSec * p.speed + p.start) % 1f
            val y = progress * (size.height + 140f) - 70f
            val x = p.x * size.width + sin((timeSec * 0.5f + p.start * 9f) * 1.6f) * p.sway * size.width
            val fade = when {
                progress < 0.08f -> progress / 0.08f
                progress > 0.9f -> (1f - progress) / 0.1f
                else -> 1f
            }
            val sprite = DecorSprites[p.spriteIndex]
            rotate(degrees = sin(timeSec * 0.6f + p.start * 6f) * p.spin, pivot = Offset(x, y)) {
                drawSprite(
                    sprite = sprite,
                    topLeft = Offset(x, y),
                    heightPx = p.size,
                    alpha = p.alpha * fade * alpha
                )
            }
        }
    }
}

/** Почти незаметные пиксельные облака, плывущие по верху экрана. */
@Composable
fun PixelCloudLayer(
    modifier: Modifier = Modifier,
    alpha: Float = 0.5f
) {
    val transition = rememberInfiniteTransition(label = "clouds")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(96_000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudTime"
    )

    Canvas(modifier = modifier) {
        val clouds = listOf(
            Triple(0.05f, 0.06f, 46f),
            Triple(0.55f, 0.16f, 34f),
            Triple(0.28f, 0.30f, 26f)
        )
        clouds.forEachIndexed { index, (fx, fy, h) ->
            val drift = ((t + index * 0.27f) % 1f)
            val x = (fx + drift * 1.15f) * size.width - size.width * 1.15f
            val bob = sin((t + index * 0.4f) * TAU * 3f) * 4f
            drawSprite(
                sprite = CloudSprite,
                topLeft = Offset(x, fy * size.height + bob),
                heightPx = h * density,
                alpha = alpha * 0.16f,
                tint = CG.pixelWhite
            )
        }
    }
}

private const val TAU = 6.28318f

/* ------------------------------- Мелочи ------------------------------- */

/** Пунктирная пиксельная линия. */
@Composable
fun PixelDivider(
    modifier: Modifier = Modifier,
    color: Color = LavenderGlow.copy(alpha = 0.45f),
    square: Dp = 3.dp,
    gap: Dp = 4.dp
) {
    Canvas(modifier = modifier.fillMaxWidth().height(square)) {
        val cell = square.toPx()
        val step = cell + gap.toPx()
        var x = 0f
        while (x < size.width) {
            drawRect(
                color = color,
                topLeft = Offset(x, 0f),
                size = Size(cell, size.height)
            )
            x += step
        }
    }
}

/** Маленький пиксельный бейдж с текстом (моноширинный шрифт). */
@Composable
fun PixelTag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LavenderGlow
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text = text, style = PixelType.caption, color = color)
    }
}

/**
 * Пиксельная рамка: четыре угловых квадратика и пунктир по краям.
 * Внутрь кладётся любой контент.
 */
@Composable
fun PixelFrame(
    modifier: Modifier = Modifier,
    color: Color = LavenderGlow.copy(alpha = 0.55f),
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val corner = 7.dp.toPx()
            val stroke = 2.dp.toPx()
            val w = size.width
            val h = size.height
            // Уголки
            listOf(
                Offset(0f, 0f),
                Offset(w - corner, 0f),
                Offset(0f, h - corner),
                Offset(w - corner, h - corner)
            ).forEach { offset ->
                drawRect(
                    color = color,
                    topLeft = offset,
                    size = Size(corner, corner),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )
            }
            // Пунктир по горизонтали
            var x = corner + 6.dp.toPx()
            while (x < w - corner - 4.dp.toPx()) {
                drawRect(color = color, topLeft = Offset(x, 0f), size = Size(5.dp.toPx(), stroke))
                drawRect(color = color, topLeft = Offset(x, h - stroke), size = Size(5.dp.toPx(), stroke))
                x += 11.dp.toPx()
            }
            // Пунктир по вертикали
            var y = corner + 6.dp.toPx()
            while (y < h - corner - 4.dp.toPx()) {
                drawRect(color = color, topLeft = Offset(0f, y), size = Size(stroke, 5.dp.toPx()))
                drawRect(color = color, topLeft = Offset(w - stroke, y), size = Size(stroke, 5.dp.toPx()))
                y += 11.dp.toPx()
            }
        }
        content()
    }
}

/** Ряд фонариков с пунктиром — декоративный разделитель экранов. */
@Composable
fun PixelLanternRow(
    modifier: Modifier = Modifier,
    sprites: List<PixelSprite> = listOf(LanternSprite, SakuraSprite, LanternSprite)
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        PixelDivider(modifier = Modifier.weight(1f), square = 3.dp, gap = 5.dp)
        sprites.forEach { sprite ->
            Spacer(Modifier.width(8.dp))
            PixelSpriteFit(sprite = sprite, width = 24.dp, height = 26.dp, alpha = 0.85f)
            Spacer(Modifier.width(8.dp))
        }
        PixelDivider(modifier = Modifier.weight(1f), square = 3.dp, gap = 5.dp)
    }
}

/** Котик-талисман с репликой в облачке. */
@Composable
fun PixelCatWisdom(
    text: String,
    modifier: Modifier = Modifier,
    sprite: PixelSprite = CatSprite
) {
    val transition = rememberInfiniteTransition(label = "cat")
    val bob by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "catBob"
    )

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        PixelSpriteFit(
            sprite = sprite,
            width = 54.dp,
            height = 50.dp,
            modifier = Modifier.graphicsLayer { translationY = bob }
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(CG.glass(0.08f))
                .border(1.dp, LavenderGlow.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(text = text, style = PixelType.chip, color = TextPrimary)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "加油！ — держись, получится",
                    style = PixelType.caption,
                    color = TextSecondary
                )
            }
        }
    }
}

/** Уголок с цветущей веткой сакуры: ставится поверх фона. */
@Composable
fun PixelSakuraCorner(
    modifier: Modifier = Modifier,
    mirrored: Boolean = false,
    alpha: Float = 0.9f
) {
    Box(
        modifier = modifier.graphicsLayer {
            if (mirrored) scaleX = -1f
        }
    ) {
        PixelSpriteFit(
            sprite = SakuraSprite,
            width = 78.dp,
            height = 62.dp,
            alpha = alpha * 0.75f,
            modifier = Modifier.align(Alignment.TopStart)
        )
        PixelSpriteFit(
            sprite = PetalSprite,
            width = 30.dp,
            height = 30.dp,
            alpha = alpha * 0.55f,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 66.dp, top = 30.dp)
        )
        PixelSpriteFit(
            sprite = SparkSprite,
            width = 22.dp,
            height = 22.dp,
            alpha = alpha * 0.6f,
            tint = CG.pixelGold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 30.dp, top = 44.dp)
        )
    }
}

/**
 * Пиксельный «взрыв» искр — например, когда ответ верный.
 * [burstKey] меняется при каждом событии, чтобы анимация запускалась заново.
 */
@Composable
fun PixelSparkBurst(
    burstKey: Int,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    color: Color = CG.pixelGold
) {
    var progress by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(burstKey) {
        if (burstKey <= 0 || !visible) return@LaunchedEffect
        progress = 0f
        val start = withFrameNanos { it }
        while (progress < 1f) {
            withFrameNanos { now ->
                progress = ((now - start) / 620_000_000f).coerceIn(0f, 1f)
            }
        }
    }

    if (progress >= 1f || !visible) return

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = (0.35f + progress) * size.minDimension * 0.6f
        val alpha = (1f - progress) * 0.9f
        listOf(0f, 90f, 180f, 270f).forEachIndexed { index, angle ->
            val rad = Math.toRadians((angle + progress * 40f).toDouble())
            val dist = radius * (0.6f + index * 0.12f)
            drawSprite(
                sprite = if (index % 2 == 0) SparkSprite else StarSprite,
                topLeft = Offset(
                    centerX + (kotlin.math.cos(rad) * dist).toFloat() - 8f,
                    centerY + (kotlin.math.sin(rad) * dist).toFloat() - 8f
                ),
                heightPx = 16f,
                alpha = alpha,
                tint = color
            )
        }
    }
}

/** Мягкая «шахматка» из полупрозрачных квадратиков — подложка под заголовки. */
@Composable
fun PixelCheckerBackdrop(
    modifier: Modifier = Modifier,
    cell: Dp = 8.dp,
    color: Color = VividPurple.copy(alpha = 0.06f)
) {
    Canvas(modifier = modifier) {
        val size = cell.toPx()
        var y = 0f
        var row = 0
        while (y < this.size.height) {
            var x = if (row % 2 == 0) 0f else size
            while (x < this.size.width) {
                drawRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(size, size),
                    cornerRadius = CornerRadius(size * 0.15f)
                )
                x += size * 2
            }
            y += size
            row++
        }
    }
}

/** Плавный «дыхательный» градиент за большой иконкой с пиксельным блеском. */
@Composable
fun PixelGlowBackdrop(
    modifier: Modifier = Modifier,
    color: Color = VividPurple
) {
    val transition = rememberInfiniteTransition(label = "pixelGlow")
    val k by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    Canvas(modifier = modifier.graphicsLayer { scaleX = k; scaleY = k }) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(color.copy(alpha = 0.35f), Color.Transparent)
            ),
            radius = size.minDimension / 2f
        )
    }
}
