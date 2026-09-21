package com.chinesegames.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.chinesegames.app.audio.ChineseSpeaker
import com.chinesegames.app.audio.GameSounds

private val PurpleScheme = darkColorScheme(
    primary = VividPurple,
    onPrimary = Color.White,
    primaryContainer = RoyalPurple,
    onPrimaryContainer = Lilac,
    secondary = LavenderGlow,
    onSecondary = DeepPurple,
    secondaryContainer = IndigoDeep,
    onSecondaryContainer = Lilac,
    tertiary = FuchsiaGlow,
    onTertiary = Color.White,
    background = VoidPurple,
    onBackground = TextPrimary,
    surface = DeepPurple,
    onSurface = TextPrimary,
    surfaceVariant = MidnightPurple,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = MidnightPurple,
    surfaceContainerHigh = Color(0xFF241249),
    outline = GlassStroke,
    outlineVariant = Color(0x33A78BFA),
    error = RoseAccent,
    onError = Color.White,
    scrim = Color(0xCC0B0618)
)

/** Доступ к звукам и озвучке из любого экрана. */
val LocalSounds = staticCompositionLocalOf<GameSounds> { error("GameSounds не подключены") }
val LocalSpeaker = staticCompositionLocalOf<ChineseSpeaker> { error("ChineseSpeaker не подключены") }

@Composable
fun ChineseGamesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PurpleScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
