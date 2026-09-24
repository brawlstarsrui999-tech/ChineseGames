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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
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
import com.chinesegames.app.ads.AdBanner
import com.chinesegames.app.ui.components.BreathingIcon
import com.chinesegames.app.ui.components.CircleIconButton
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
import com.chinesegames.app.ui.theme.TextSecondary
import com.chinesegames.app.ui.theme.VividPurple

/**
 * Главный экран: логотип, статистика и переключатели звука.
 *
 * Разделов в нижней навигации стало много, поэтому плиток-кнопок и длинных
 * пояснений здесь больше нет — экран короткий и не перегружен текстом.
 */
@Composable
fun MainMenuScreen(
    totalWords: Int,
    learnedWords: Int,
    gamesPlayed: Int,
    bestScore: Int,
    onOpenShop: () -> Unit
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
                    icon = Icons.Filled.ShoppingBag,
                    contentDescription = "Магазин украшений",
                    tint = GoldAccent
                ) {
                    sounds.click()
                    onOpenShop()
                }
                Spacer(Modifier.width(10.dp))
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

            Spacer(Modifier.height(34.dp))

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

            Spacer(Modifier.height(26.dp))

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

            Spacer(Modifier.height(28.dp))

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

            Spacer(Modifier.height(16.dp))
            // В уроках и играх рекламы нет; на главной — один ненавязчивый баннер.
            // После покупки «Без рекламы» AdBanner сам перестаёт рисоваться.
            AdBanner()
            Spacer(Modifier.height(12.dp))

            Text(
                text = "Разделы — в панели внизу экрана",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(22.dp))
            Text(
                text = "by CloverTeam",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}
