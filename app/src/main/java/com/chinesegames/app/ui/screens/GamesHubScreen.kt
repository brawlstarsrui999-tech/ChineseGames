package com.chinesegames.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chinesegames.app.ui.components.Badge
import com.chinesegames.app.ui.components.CgTopBar
import com.chinesegames.app.ui.components.GlassCard
import com.chinesegames.app.ui.components.MainTab
import com.chinesegames.app.ui.components.accent
import com.chinesegames.app.ui.components.icon
import com.chinesegames.app.ui.components.softBorder
import com.chinesegames.app.ui.components.softGradient
import com.chinesegames.app.ui.components.softSurface
import com.chinesegames.app.ui.components.PixelCatWisdom
import com.chinesegames.app.ui.components.PixelLanternRow
import com.chinesegames.app.ui.components.PixelTag
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.SectionTitle
import com.chinesegames.app.ui.components.StatPill
import com.chinesegames.app.ui.components.VSpace
import com.chinesegames.app.ui.formatPercent
import com.chinesegames.app.ui.game.GameGroup
import com.chinesegames.app.ui.game.GameKind
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.PixelType
import com.chinesegames.app.ui.theme.SkyAccent
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary

/**
 * Хаб игр: все восемь тренировок, разложенные по группам
 * (память, скорость, аудио, письмо).
 */
@Composable
fun GamesHubScreen(
    gamesPlayed: Int,
    pairsFound: Int,
    averageAccuracy: Float,
    bestScore: Int,
    favoritesCount: Int,
    onBack: (() -> Unit)? = null,
    onOpenGame: (GameKind, List<Long>) -> Unit,
    onOpenStats: () -> Unit
) {
    val sounds = LocalSounds.current

    PurpleBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            CgTopBar(
                title = "Игры",
                subtitle = "Восемь тренировок по вашему словарю",
                icon = MainTab.GAMES.icon,
                iconTint = MainTab.GAMES.accent,
                onBack = onBack?.let { handler ->
                    {
                        sounds.whoosh()
                        handler()
                    }
                },
                actions = {
                    Icon(
                        imageVector = Icons.Filled.Insights,
                        contentDescription = "Статистика",
                        tint = LavenderGlow,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                sounds.click()
                                onOpenStats()
                            }
                            .padding(10.dp)
                    )
                }
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatPill(
                        icon = Icons.Filled.SportsEsports,
                        value = "$gamesPlayed",
                        label = "партий",
                        modifier = Modifier.weight(1f),
                        accent = SkyAccent
                    )
                    StatPill(
                        icon = Icons.Filled.Bolt,
                        value = "$pairsFound",
                        label = "слов угадано",
                        modifier = Modifier.weight(1f),
                        accent = LavenderGlow
                    )
                }
                VSpace(10.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatPill(
                        icon = Icons.Filled.School,
                        value = formatPercent(averageAccuracy),
                        label = "точность",
                        modifier = Modifier.weight(1f),
                        accent = MintAccent
                    )
                    StatPill(
                        icon = Icons.Filled.EmojiEvents,
                        value = "$bestScore",
                        label = "рекорд",
                        modifier = Modifier.weight(1f),
                        accent = GoldAccent
                    )
                }

                VSpace(22.dp)

                GameKind.hubOrder
                    .groupBy { it.group }
                    .forEach { (group, games) ->
                        VSpace(6.dp)
                        GroupHeader(group)
                        VSpace(10.dp)
                        games.forEach { kind ->
                            GameRow(
                                kind = kind,
                                onClick = {
                                    sounds.click()
                                    onOpenGame(kind, emptyList())
                                }
                            )
                            VSpace(10.dp)
                        }
                        VSpace(14.dp)
                    }

                if (favoritesCount > 0) {
                    GlassCard(contentPadding = PaddingValues(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            PixelTag(text = "ИЗБРАННОЕ", color = GoldAccent)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Своя подборка: $favoritesCount слов",
                                style = PixelType.caption,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        VSpace(10.dp)
                        Text(
                            text = "В настройке партии выберите папку «Избранное» — и играйте " +
                                "только по отмеченным словам.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        VSpace(12.dp)
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            GamePlayButton(
                                text = "Найди пару",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    sounds.click()
                                    onOpenGame(GameKind.MATCH, listOf(-1L))
                                }
                            )
                            GamePlayButton(
                                text = "Спринт",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    sounds.click()
                                    onOpenGame(GameKind.SPRINT, listOf(-1L))
                                }
                            )
                        }
                    }
                    VSpace(20.dp)
                }

                PixelLanternRow()
                VSpace(16.dp)
                PixelCatWisdom(
                    text = "Меняйте игры — так слова запоминаются прочнее",
                    modifier = Modifier.fillMaxWidth()
                )
                VSpace(26.dp)
            }
        }
    }
}

@Composable
private fun GroupHeader(group: GameGroup) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = group.icon,
            contentDescription = null,
            tint = group.accent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        SectionTitle(group.title)
    }
}

/** Карточка одной игры в хабе. */
@Composable
private fun GameRow(kind: GameKind, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val accent = kind.group.accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(kind.group.softSurface)
            .border(1.dp, kind.group.softBorder, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(kind.group.softGradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = kind.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = kind.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (kind.isCardGame) {
                    Spacer(Modifier.width(8.dp))
                    Badge("карточки")
                }
            }
            VSpace(3.dp)
            Text(
                text = kind.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            VSpace(5.dp)
            Text(
                text = kind.rules,
                style = PixelType.caption,
                color = TextMuted
            )
        }
        Spacer(Modifier.width(10.dp))
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun GamePlayButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(CG.primaryGradient))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, style = PixelType.chip, color = Color.White)
    }
}
