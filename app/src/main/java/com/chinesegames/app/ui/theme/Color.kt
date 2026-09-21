package com.chinesegames.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/* --------------------------------------------------------------------------
 *  Палитра ChineseGames — глубокий фиолетовый неон
 * -------------------------------------------------------------------------- */

val VoidPurple = Color(0xFF0B0618)
val DeepPurple = Color(0xFF140A2B)
val MidnightPurple = Color(0xFF1C0F3D)
val RoyalPurple = Color(0xFF6D28D9)
val VividPurple = Color(0xFF8B5CF6)
val LightViolet = Color(0xFFA78BFA)
val LavenderGlow = Color(0xFFC4B5FD)
val Lilac = Color(0xFFE9D5FF)
val FuchsiaGlow = Color(0xFFD946EF)
val IndigoDeep = Color(0xFF4C1D95)

val GoldAccent = Color(0xFFFBBF24)
val MintAccent = Color(0xFF34D399)
val RoseAccent = Color(0xFFFB7185)
val SkyAccent = Color(0xFF60A5FA)

val TextPrimary = Color(0xFFF6F3FF)
val TextSecondary = Color(0xFFBBAEE4)
val TextMuted = Color(0xFF8271AC)

val GlassFill = Color(0x1AFFFFFF)
val GlassFillStrong = Color(0x26FFFFFF)
val GlassStroke = Color(0x4DB794F6)

/**
 * Готовые градиенты и «клейкие» цвета для карточек и кнопок,
 * чтобы оформление было одинаковым на всех экранах.
 */
object CG {
    val background = Brush.verticalGradient(
        listOf(Color(0xFF1A0B36), Color(0xFF0E0620), Color(0xFF160A2E))
    )

    val headerGlow = Brush.radialGradient(
        listOf(VividPurple.copy(alpha = 0.45f), Color.Transparent)
    )

    val cardGradient = listOf(
        Color(0x338B5CF6),
        Color(0x1A6D28D9),
        Color(0x14FFFFFF)
    )

    val cardBorder = listOf(
        LavenderGlow.copy(alpha = 0.45f),
        Color.Transparent,
        FuchsiaGlow.copy(alpha = 0.35f)
    )

    val primaryGradient = listOf(Color(0xFF9F6BFF), Color(0xFFD946EF))
    val primaryGradientDeep = listOf(Color(0xFF6D28D9), Color(0xFFA855F7))
    val goldGradient = listOf(Color(0xFFFCD34D), Color(0xFFF59E0B))
    val successGradient = listOf(Color(0xFF34D399), Color(0xFF059669))
    val dangerGradient = listOf(Color(0xFFFB7185), Color(0xFFBE123C))
    val audioGradient = listOf(Color(0xFF60A5FA), Color(0xFF7C3AED))

    const val LEARNED_THRESHOLD = 0.75f
}
