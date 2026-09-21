package com.chinesegames.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.CgPaletteState
import com.chinesegames.app.ui.theme.FuchsiaGlow
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.SkyAccent
import com.chinesegames.app.ui.theme.VividPurple
import kotlin.math.sin
import kotlin.random.Random

/**
 * Живой фон: градиент + плавающие светящиеся «орбы» + звёздная пыль,
 * а поверх — анимешно-пиксельные декорации (лепестки сакуры и облачка).
 * Используется на всех экранах, чтобы оформление было единым.
 */
@Composable
fun PurpleBackground(
    modifier: Modifier = Modifier,
    petals: Boolean = true,
    clouds: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "background")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val dust = remember {
        val rnd = Random(7)
        List(60) {
            Triple(rnd.nextFloat(), rnd.nextFloat(), 0.4f + rnd.nextFloat() * 1.6f)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(CG.background)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val night = CgPaletteState.night
            val orbAlpha = if (night) 1f else 0.55f

            glow(
                center = Offset(w * (0.16f + 0.06f * sin(t * TAU)), h * 0.10f),
                radius = w * 0.85f,
                color = VividPurple,
                alpha = 0.32f * orbAlpha
            )
            glow(
                center = Offset(w * (0.92f - 0.05f * sin(t * TAU + 1.5f)), h * 0.42f),
                radius = w * 0.75f,
                color = FuchsiaGlow,
                alpha = 0.20f * orbAlpha
            )
            glow(
                center = Offset(w * (0.30f + 0.08f * sin(t * TAU + 3f)), h * 0.92f),
                radius = w * 0.9f,
                color = SkyAccent,
                alpha = (if (night) 0.13f else 0.10f) * orbAlpha
            )

            // Звёздная пыль
            dust.forEachIndexed { index, (fx, fy, speed) ->
                val twinkle = 0.35f + 0.65f * ((sin(t * TAU * speed + index) + 1f) / 2f)
                val alpha = (if (night) 0.16f else 0.20f) * twinkle
                drawCircle(
                    color = LavenderGlow.copy(alpha = alpha),
                    radius = if (index % 7 == 0) 2.6f else 1.5f,
                    center = Offset(fx * w, fy * h)
                )
            }
        }

        if (clouds) {
            PixelCloudLayer(modifier = Modifier.fillMaxSize())
        }

        if (petals) {
            PixelPetalLayer(
                modifier = Modifier.fillMaxSize(),
                count = if (CgPaletteState.night) 12 else 14,
                alpha = if (CgPaletteState.night) 0.85f else 1f
            )
        }

        content()
    }
}

private const val TAU = 6.28318f

private fun DrawScope.glow(center: Offset, radius: Float, color: Color, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.35f), Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}
