package com.chinesegames.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
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
import com.chinesegames.app.ui.theme.SkyAccent
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
    /** Часть подпапок выбрана — рисуем «минус» вместо галочки. */
    partial: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val highlighted = selected || partial
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (highlighted) Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.28f), CG.glass(0.05f))
                ) else Brush.linearGradient(
                    listOf(CG.glass(0.05f), CG.glass(0.03f))
                )
            )
            .border(
                1.dp,
                if (highlighted) accent.copy(alpha = 0.6f) else CG.glass(0.09f),
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
        if (trailing != null) {
            trailing()
            Spacer(Modifier.width(6.dp))
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (highlighted) Brush.horizontalGradient(CG.primaryGradient)
                    else Brush.horizontalGradient(listOf(CG.glass(0.08f), CG.glass(0.05f)))
                )
                .border(1.dp, if (highlighted) Color.Transparent else accent.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            } else if (partial) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * Список всех источников слов: «Избранное», «Сложные слова», папки курса
 * «HSK 1» … «HSK 7» (раскрываются на разделы) и обычные папки пользователя.
 * Псевдо-папки показываются, только если в них есть слова.
 *
 * Выбор папки уровня означает «все её разделы»: в набор попадает
 * идентификатор уровня, репозиторий сам раскроет его в подпапки.
 * Если отметить только часть разделов — в наборе будут их идентификаторы.
 */
@Composable
fun DeckSelectionColumn(
    decks: List<Deck>,
    wordCounts: Map<Long, Int>,
    favoritesCount: Int,
    hardWordsCount: Int,
    selected: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    val childrenOf = remember(decks) { decks.filter { it.parentId != null }.groupBy { it.parentId!! } }
    val topLevel = remember(decks) { decks.filter { it.parentId == null } }
    val courseLevels = remember(topLevel) { topLevel.filter { it.isCourseLevel } }
    val plainDecks = remember(topLevel) { topLevel.filter { !it.isCourseLevel } }

    fun toggle(id: Long) {
        onSelectionChange(if (selected.contains(id)) selected - id else selected + id)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        if (favoritesCount > 0) {
            DeckSelectRow(
                emoji = "⭐",
                title = "Избранное",
                subtitle = "${wordsLabel(favoritesCount)} из всех папок",
                selected = selected.contains(DeckRepository.FAVORITES_ID),
                accent = GoldAccent
            ) { toggle(DeckRepository.FAVORITES_ID) }
        }
        if (hardWordsCount > 0) {
            DeckSelectRow(
                emoji = "🧠",
                title = "Сложные слова",
                subtitle = "$hardWordsCount ${if (hardWordsCount % 10 == 1 && hardWordsCount != 11) "слово" else "слова"} с низкой точностью",
                selected = selected.contains(DeckRepository.HARD_WORDS_ID),
                accent = RoseAccent
            ) { toggle(DeckRepository.HARD_WORDS_ID) }
        }
        plainDecks.forEach { deck ->
            DeckSelectRow(
                emoji = deck.emoji,
                title = deck.name,
                subtitle = wordsLabel(wordCounts[deck.id] ?: 0),
                selected = selected.contains(deck.id),
                accent = MintAccent
            ) { toggle(deck.id) }
        }
        courseLevels.forEach { level ->
            CourseFolderSelect(
                level = level,
                sections = childrenOf[level.id].orEmpty(),
                wordCounts = wordCounts,
                selected = selected,
                onSelectionChange = onSelectionChange
            )
        }
    }
}

/** Папка уровня курса с раскрывающимся списком разделов. */
@Composable
private fun CourseFolderSelect(
    level: Deck,
    sections: List<Deck>,
    wordCounts: Map<Long, Int>,
    selected: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit
) {
    var expanded by remember(level.id) { mutableStateOf(false) }
    val sectionIds = remember(sections) { sections.map { it.id } }
    val wholeSelected = selected.contains(level.id)
    val chosenSections = if (wholeSelected) sectionIds.size else sectionIds.count { selected.contains(it) }
    val partial = !wholeSelected && chosenSections > 0

    val subtitle = when {
        wholeSelected -> "${wordsLabel(wordCounts[level.id] ?: 0)} · весь уровень"
        partial -> "выбрано разделов: $chosenSections из ${sections.size}"
        else -> "${wordsLabel(wordCounts[level.id] ?: 0)} · ${sections.size} разд."
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DeckSelectRow(
            emoji = level.emoji,
            title = level.name,
            subtitle = subtitle,
            selected = wholeSelected,
            partial = partial,
            accent = SkyAccent,
            trailing = {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Свернуть разделы" else "Показать разделы",
                    tint = SkyAccent,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable { expanded = !expanded }
                        .padding(4.dp)
                )
            },
            onClick = {
                // Тап по уровню: включаем весь уровень или снимаем его вместе с разделами
                onSelectionChange(
                    if (wholeSelected || partial) selected - level.id - sectionIds.toSet()
                    else selected + level.id
                )
            }
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(start = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sections.forEach { section ->
                    val sectionSelected = wholeSelected || selected.contains(section.id)
                    DeckSelectRow(
                        emoji = section.emoji,
                        title = section.name,
                        subtitle = wordsLabel(wordCounts[section.id] ?: 0),
                        selected = sectionSelected,
                        accent = SkyAccent,
                        onClick = {
                            val next: Set<Long> = when {
                                // весь уровень был выбран — оставляем все разделы, кроме этого
                                wholeSelected -> selected - level.id + (sectionIds.toSet() - section.id)
                                sectionSelected -> selected - section.id
                                else -> {
                                    val withSection = selected + section.id
                                    // выбраны все разделы — сворачиваем в «весь уровень»
                                    if (sectionIds.all { withSection.contains(it) }) {
                                        withSection - sectionIds.toSet() + level.id
                                    } else {
                                        withSection
                                    }
                                }
                            }
                            onSelectionChange(next)
                        }
                    )
                }
            }
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
