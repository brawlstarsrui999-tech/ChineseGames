package com.chinesegames.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Заголовки — с засечками (выглядит «книжно», подходит для иероглифов),
 * основной текст — санс-сериф, а подписи и цифры — моноширинные:
 * так интерфейс выглядит «по-пиксельному» без внешних шрифтов.
 */
private val Display = FontFamily.Serif
private val Body = FontFamily.SansSerif
private val Pixel = FontFamily.Monospace

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 54.sp),
    displayMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 44.sp),
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    titleSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp)
)

/**
 * «Пиксельные» стили: моноширинный шрифт + разрядка.
 * Используются для очков, таймеров, бейджей и декоративных подписей.
 */
object PixelType {
    val hud = TextStyle(
        fontFamily = Pixel,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 1.5.sp
    )
    val chip = TextStyle(
        fontFamily = Pixel,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.sp
    )
    val caption = TextStyle(
        fontFamily = Pixel,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp
    )
    val title = TextStyle(
        fontFamily = Pixel,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 2.sp
    )
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
