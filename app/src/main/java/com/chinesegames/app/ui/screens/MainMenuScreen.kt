package com.chinesegames.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chinesegames.app.ui.components.BreathingIcon
import com.chinesegames.app.ui.components.CircleIconButton
import com.chinesegames.app.ui.components.GlassCard
import com.chinesegames.app.ui.components.MenuTile
import com.chinesegames.app.ui.components.PulsingGlow
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.StatPill
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.LocalMusic
import com.chinesegames.app.ui.theme.LocalSettings
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.SkyAccent
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary
import com.chinesegames.app.ui.theme.VividPurple

/**
 * Главный экран: логотип, статистика и два входа — словарь и игры.
 */
@Composable
fun MainMenuScreen(
    totalWords: Int,
    learnedWords: Int,
    decksCount: Int,
    gamesPlayed: Int,
    bestScore: Int,
    onOpenDecks: () -> Unit,
    onOpenGames: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val sounds = LocalSounds.current
    val music = LocalMusic.current
    val settings = LocalSettings.current
    var muted by remember { mutableStateOf(sounds.muted) }
    var musicOn by remember { mutableStateOf(settings.settings.musicEnabled) }

    PurpleBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "汉语 · 游戏",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                    modifier = Modifier.weight(1f)
                )
                CircleIconButton(
                    icon = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (muted) "Включить звук" else "Выключить звук",
                    tint = if (muted) TextMuted else LavenderGlow
                ) {
                    muted = !muted
                    sounds.muted = muted
                    if (!muted) sounds.click()
                }
                Spacer(Modifier.width(10.dp))
                CircleIconButton(
                    icon = if (musicOn) Icons.Filled.MusicNote else Icons.Filled.MusicOff,
                    contentDescription = if (musicOn) "Выключить музыку" else "Включить музыку",
                    tint = if (musicOn) LavenderGlow else TextMuted
                ) {
                    musicOn = !musicOn
                    music.enabled = musicOn
                    settings.setMusicEnabled(musicOn)
                    if (musicOn) sounds.click()
                }
            }

            Spacer(Modifier.height(18.dp))

            // Логотип
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                PulsingGlow(size = 210.dp, color = VividPurple)
                BreathingIcon {
                    Box(
                        modifier = Modifier
                            .size(118.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(Brush.linearGradient(CG.primaryGradient))
                            .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(36.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "汉",
                            fontSize = 60.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "ChineseGames",
                style = MaterialTheme.typography.displaySmall.copy(
                    brush = Brush.horizontalGradient(listOf(LavenderGlow, Color.White, GoldAccent))
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Повторяй выученный китайский — играя",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            // Статистика
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill(
                    icon = Icons.Filled.AutoStories,
                    value = "$totalWords",
                    label = "слов в словаре",
                    modifier = Modifier.weight(1f),
                    accent = LavenderGlow
                )
                StatPill(
                    icon = Icons.Filled.School,
                    value = "$learnedWords",
                    label = "уверенно знаю",
                    modifier = Modifier.weight(1f),
                    accent = MintAccent
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill(
                    icon = Icons.Filled.SportsEsports,
                    value = "$gamesPlayed",
                    label = "партий сыграно",
                    modifier = Modifier.weight(1f),
                    accent = SkyAccent
                )
                StatPill(
                    icon = Icons.Filled.EmojiEvents,
                    value = "$bestScore",
                    label = "лучший счёт",
                    modifier = Modifier.weight(1f),
                    accent = GoldAccent
                )
            }

            Spacer(Modifier.height(26.dp))

            MenuTile(
                title = "Словарь",
                subtitle = if (decksCount == 0) {
                    "Создайте первую папку со словами"
                } else {
                    "Папок: $decksCount · иероглиф, пиньинь, перевод"
                },
                icon = Icons.AutoMirrored.Filled.MenuBook,
                glyph = "字",
                gradient = CG.primaryGradient,
                onClick = {
                    sounds.click()
                    onOpenDecks()
                }
            )

            Spacer(Modifier.height(14.dp))

            MenuTile(
                title = "Игры",
                subtitle = "«Найди пару» — тренировка на скорость и память",
                icon = Icons.Filled.SportsEsports,
                glyph = "戏",
                gradient = CG.goldGradient,
                onClick = {
                    sounds.click()
                    onOpenGames()
                }
            )

            Spacer(Modifier.height(14.dp))

            MenuTile(
                title = "Статистика",
                subtitle = "График по дням, слабые слова, рекорды",
                icon = Icons.Filled.Insights,
                glyph = "📊",
                gradient = CG.primaryGradient,
                onClick = {
                    sounds.click()
                    onOpenStats()
                }
            )

            Spacer(Modifier.height(14.dp))

            MenuTile(
                title = "Избранное",
                subtitle = "Слова со звёздочкой — своя подборка для игр",
                icon = Icons.Filled.Star,
                glyph = "⭐",
                gradient = CG.goldGradient,
                onClick = {
                    sounds.click()
                    onOpenFavorites()
                }
            )

            Spacer(Modifier.height(14.dp))

            MenuTile(
                title = "Настройки",
                subtitle = "Тема день/ночь, музыка, экспорт словаря в CSV",
                icon = Icons.Filled.Settings,
                glyph = "⚙️",
                gradient = CG.audioGradient,
                onClick = {
                    sounds.click()
                    onOpenSettings()
                }
            )

            Spacer(Modifier.height(26.dp))

            GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🐉", fontSize = 26.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Как это работает",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "1. Запишите слова в папки\n" +
                                "2. Выберите папки и число карт\n" +
                                "3. Найдите пару каждому слову",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
            Text(
                text = "Учись понемногу, но каждый день 🌸",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
