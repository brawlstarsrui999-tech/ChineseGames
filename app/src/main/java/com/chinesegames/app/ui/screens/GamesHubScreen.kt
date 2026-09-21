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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.compose.ui.unit.sp
import com.chinesegames.app.ui.components.Badge
import com.chinesegames.app.ui.components.CgTopBar
import com.chinesegames.app.ui.components.GlassCard
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.SectionTitle
import com.chinesegames.app.ui.components.StatPill
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.SkyAccent
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary
import com.chinesegames.app.ui.formatPercent

/**
 * Хаб игр: пока доступна «Найди пару», остальные — в разработке.
 */
@Composable
fun GamesHubScreen(
    gamesPlayed: Int,
    pairsFound: Int,
    averageAccuracy: Float,
    bestScore: Int,
    onBack: () -> Unit,
    onOpenMatch: () -> Unit
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
                subtitle = "Повторение в игровой форме",
                onBack = {
                    sounds.whoosh()
                    onBack()
                },
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
                    label = "пар найдено",
                    modifier = Modifier.weight(1f),
                    accent = LavenderGlow
                )
            }
            Spacer(Modifier.height(10.dp))
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

            Spacer(Modifier.height(24.dp))
            SectionTitle("Доступно сейчас")
            Spacer(Modifier.height(12.dp))

            MatchGameCard(onClick = {
                sounds.click()
                onOpenMatch()
            })

            Spacer(Modifier.height(24.dp))
            SectionTitle("Скоро")
            Spacer(Modifier.height(12.dp))

            ComingSoonCard(
                emoji = "🎧",
                title = "Аудио-квиз",
                description = "Слушай слово и выбирай правильный иероглиф"
            )
            Spacer(Modifier.height(10.dp))
            ComingSoonCard(
                emoji = "🧩",
                title = "Мемори-сетка",
                description = "Классическое мемори с сеткой больших размеров"
            )

            Spacer(Modifier.height(24.dp))
            SectionTitle("Правила «Найди пару»")
            Spacer(Modifier.height(12.dp))

            GlassCard(contentPadding = PaddingValues(18.dp)) {
                RuleRow("1", "Выберите папки со словами и число карт")
                RuleRow("2", "Открывайте по две карточки: 汉字 и его русский перевод")
                RuleRow("3", "Совпало — пара ваша. Ошиблись — карточки закроются")
                RuleRow("4", "Открывайте пары подряд, чтобы собрать комбо и очки")
            }

            Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RuleRow(number: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(CG.primaryGradient)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

/** Главная карточка игры «Найди пару». */
@Composable
private fun MatchGameCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF6D28D9), Color(0xFF9F1239), Color(0xFF7C3AED))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SportsEsports,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Найди пару",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "汉字 ↔ перевод · на память и скорость",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Мини-превью пар
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniCard("汉", "hàn", Color.White.copy(alpha = 0.18f))
            MiniCard("?", null, Color.White.copy(alpha = 0.10f))
            MiniCard("иероглиф", null, Color.White.copy(alpha = 0.18f))
        }

        Spacer(Modifier.height(16.dp))

        GradientButton(
            text = "Играть",
            icon = Icons.Filled.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
            colors = listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.12f)),
            onClick = onClick
        )
    }
}

@Composable
private fun MiniCard(main: String, sub: String?, background: Color) {
    Box(
        modifier = Modifier
            .size(width = 74.dp, height = 92.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = main,
                color = Color.White,
                fontSize = if (main.length > 2) 15.sp else 30.sp,
                fontWeight = FontWeight.Medium
            )
            if (sub != null) {
                Text(text = sub, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ComingSoonCard(emoji: String, title: String, description: String) {
    GlassCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 28.sp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary.copy(alpha = 0.75f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Badge("скоро")
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = GoldAccent.copy(alpha = 0.55f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
