package com.chinesegames.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chinesegames.app.ui.theme.FuchsiaGlow
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.SkyAccent
import com.chinesegames.app.ui.theme.VividPurple
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val x: Float,
    val delay: Float,
    val speed: Float,
    val sway: Float,
    val size: Float,
    val color: Color,
    val tilt: Float
)

private val confettiColors = listOf(
    VividPurple, FuchsiaGlow, LavenderGlow, GoldAccent, MintAccent, SkyAccent, Color.White
)

/** Праздничное конфетти — показывается на экране победы. */
@Composable
fun ConfettiOverlay(
    modifier: Modifier = Modifier,
    particleCount: Int = 90
) {
    val particles = remember {
        val rnd = Random(1337)
        List(particleCount) {
            ConfettiParticle(
                x = rnd.nextFloat(),
                delay = rnd.nextFloat(),
                speed = 0.09f + rnd.nextFloat() * 0.22f,
                sway = 0.01f + rnd.nextFloat() * 0.06f,
                size = 4f + rnd.nextFloat() * 7f,
                color = confettiColors[rnd.nextInt(confettiColors.size)],
                tilt = rnd.nextFloat() * 360f
            )
        }
    }

    var timeSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                timeSec = (now - startNanos) / 1_000_000_000f
            }
        }
    }

    Canvas(modifier = modifier) {
        particles.forEach { p ->
            val t = (timeSec * p.speed + p.delay) % 1f
            val y = t * (size.height + 120f) - 60f
            val x = p.x * size.width + sin((timeSec + p.delay * 8f) * 1.8f) * p.sway * size.width
            val alpha = when {
                t < 0.06f -> t / 0.06f
                t > 0.88f -> (1f - t) / 0.12f
                else -> 1f
            }
            rotate(degrees = p.tilt + timeSec * 120f, pivot = Offset(x, y)) {
                drawRoundRect(
                    color = p.color.copy(alpha = alpha.coerceIn(0f, 1f) * 0.92f),
                    topLeft = Offset(x - p.size, y - p.size * 0.6f),
                    size = Size(p.size * 2f, p.size * 1.1f),
                    cornerRadius = CornerRadius(p.size * 0.35f)
                )
            }
        }
    }
}

/** Пульсирующее свечение за иконкой или логотипом. */
@Composable
fun PulsingGlow(
    modifier: Modifier = Modifier,
    color: Color = VividPurple,
    size: Dp = 160.dp,
    pulse: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val p by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier.size(size).scale(if (pulse) p else 1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.45f),
                        color.copy(alpha = 0.16f),
                        Color.Transparent
                    )
                ),
                radius = this.size.minDimension / 2f
            )
        }
    }
}

/** Мягко «дышащий» элемент — например, логотип на главном экране. */
@Composable
fun BreathingIcon(
    modifier: Modifier = Modifier,
    amplitude: Float = 0.05f,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "breath")
    val scale by transition.animateFloat(
        initialValue = 1f - amplitude,
        targetValue = 1f + amplitude,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )
    Box(modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
        content()
    }
}

/** Всплывающая надпись «Комбо!» / «+150». */
@Composable
fun ComboBurst(
    text: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
    color: Color = GoldAccent
) {
    val transition = rememberInfiniteTransition(label = "burst")
    val glow by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "burstGlow"
    )
    if (visible && text.isNotEmpty()) {
        Box(modifier = modifier.graphicsLayer { alpha = glow }) {
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
