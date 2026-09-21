package com.chinesegames.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.chinesegames.app.data.Deck
import com.chinesegames.app.data.DeckRepository
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.PixelType
import com.chinesegames.app.ui.theme.RoseAccent
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.wordsLabel

/** Одна строка выбора папки (или псевдо-папки «Избранное» / «Сложные слова»). */
@Composable
fun DeckSelectRow(
    emoji: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = LavenderGlow,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.28f), CG.glass(0.05f))
                ) else Brush.linearGradient(
                    listOf(CG.glass(0.05f), CG.glass(0.03f))
                )
            )
            .border(
                1.dp,
                if (selected) accent.copy(alpha = 0.6f) else CG.glass(0.09f),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = subtitle, style = PixelType.caption, color = TextMuted)
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (selected) Brush.horizontalGradient(CG.primaryGradient)
                    else Brush.horizontalGradient(listOf(CG.glass(0.08f), CG.glass(0.05f)))
                )
                .border(1.dp, if (selected) Color.Transparent else accent.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * Список всех источников слов: обычные папки + «Избранное» + «Сложные слова».
 * Псевдо-папки показываются, только если в них есть слова.
 */
@Composable
fun DeckSelectionColumn(
    decks: List<Deck>,
    wordCounts: Map<Long, Int>,
    favoritesCount: Int,
    hardWordsCount: Int,
    selected: Set<Long>,
    onToggle: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        if (favoritesCount > 0) {
            DeckSelectRow(
                emoji = "⭐",
                title = "Избранное",
                subtitle = "${wordsLabel(favoritesCount)} из всех папок",
                selected = selected.contains(DeckRepository.FAVORITES_ID),
                accent = GoldAccent
            ) { onToggle(DeckRepository.FAVORITES_ID) }
        }
        if (hardWordsCount > 0) {
            DeckSelectRow(
                emoji = "🧠",
                title = "Сложные слова",
                subtitle = "$hardWordsCount ${if (hardWordsCount % 10 == 1 && hardWordsCount != 11) "слово" else "слова"} с низкой точностью",
                selected = selected.contains(DeckRepository.HARD_WORDS_ID),
                accent = RoseAccent
            ) { onToggle(DeckRepository.HARD_WORDS_ID) }
        }
        decks.forEach { deck ->
            DeckSelectRow(
                emoji = deck.emoji,
                title = deck.name,
                subtitle = wordsLabel(wordCounts[deck.id] ?: 0),
                selected = selected.contains(deck.id),
                accent = MintAccent
            ) { onToggle(deck.id) }
        }
    }
}

/** Заголовок «Папки со словами» с кнопкой «выбрать всё / снять всё». */
@Composable
fun DeckSelectionHeader(
    allSelected: Boolean,
    modifier: Modifier = Modifier,
    onToggleAll: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle("Откуда берём слова", modifier = Modifier.weight(1f))
        Text(
            text = if (allSelected) "Снять всё" else "Выбрать всё",
            style = PixelType.chip,
            color = LavenderGlow,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onToggleAll)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
    Spacer(Modifier.height(2.dp))
}
