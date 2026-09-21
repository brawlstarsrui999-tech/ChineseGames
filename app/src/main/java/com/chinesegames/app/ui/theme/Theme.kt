package com.chinesegames.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.chinesegames.app.audio.BackgroundMusic
import com.chinesegames.app.audio.ChineseSpeaker
import com.chinesegames.app.audio.GameSounds
import com.chinesegames.app.data.SettingsStore
import com.chinesegames.app.data.ThemeMode

private fun nightScheme() = darkColorScheme(
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

private fun dayScheme() = lightColorScheme(
    primary = VividPurple,
    onPrimary = Color.White,
    primaryContainer = Lilac,
    onPrimaryContainer = Color(0xFF2B1657),
    secondary = LavenderGlow,
    onSecondary = Color.White,
    secondaryContainer = IndigoDeep,
    onSecondaryContainer = Color(0xFF2B1657),
    tertiary = FuchsiaGlow,
    onTertiary = Color.White,
    background = VoidPurple,
    onBackground = TextPrimary,
    surface = MidnightPurple,
    onSurface = TextPrimary,
    surfaceVariant = DeepPurple,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = DeepPurple,
    surfaceContainerHigh = Color(0xFFEFE4FF),
    outline = GlassStroke,
    outlineVariant = Color(0x338B5CF6),
    error = RoseAccent,
    onError = Color.White,
    scrim = Color(0xCC2B1657)
)

/** Доступ к звукам, музыке, озвучке и настройкам из любого экрана. */
val LocalSounds = staticCompositionLocalOf<GameSounds> { error("GameSounds не подключены") }
val LocalSpeaker = staticCompositionLocalOf<ChineseSpeaker> { error("ChineseSpeaker не подключены") }
val LocalMusic = staticCompositionLocalOf<BackgroundMusic> { error("BackgroundMusic не подключены") }
val LocalSettings = staticCompositionLocalOf<SettingsStore> { error("SettingsStore не подключены") }
val LocalThemeMode = staticCompositionLocalOf { ThemeMode.NIGHT }

@Composable
fun ChineseGamesTheme(content: @Composable () -> Unit) {
    val night = CgPaletteState.night
    val scheme = remember(night) { if (night) nightScheme() else dayScheme() }
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
