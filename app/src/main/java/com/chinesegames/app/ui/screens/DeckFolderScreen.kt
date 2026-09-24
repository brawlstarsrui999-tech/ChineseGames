package com.chinesegames.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chinesegames.app.data.Deck
import com.chinesegames.app.data.HskCourse
import com.chinesegames.app.ui.DeckViewModel
import com.chinesegames.app.ui.components.CgTopBar
import com.chinesegames.app.ui.components.GlassCard
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.GradientProgress
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.SkyAccent
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary
import com.chinesegames.app.ui.wordsLabel

/**
 * Папка уровня курса в словаре: «HSK 3» → список разделов-подпапок
 * («Местоимения», «Еда и напитки»…) и кнопка «играть со всем уровнем».
 */
@Composable
fun DeckFolderScreen(
    deckId: Long,
    viewModel: DeckViewModel,
    onBack: () -> Unit,
    onOpenDeck: (Long) -> Unit,
    onPlayWithDeck: (Long) -> Unit
) {
    val sounds = LocalSounds.current
    val deckFlow = remember(deckId) { viewModel.deckFlow(deckId) }
    val deck by deckFlow.collectAsState(initial = null)
    val decks by viewModel.decks.collectAsState()
    val wordCounts by viewModel.wordCounts.collectAsState()
    val learnedCounts by viewModel.learnedCounts.collectAsState()

    val sections = remember(decks, deckId) { decks.filter { it.parentId == deckId } }
    val total = wordCounts[deckId] ?: 0
    val learned = learnedCounts[deckId] ?: 0
    val level = deck?.courseLevel

    PurpleBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            CgTopBar(
                title = deck?.name ?: "Папка",
                subtitle = "${wordsLabel(total)} · ${sectionsLabel(sections.size)}",
                emoji = deck?.emoji,
                onBack = {
                    sounds.whoosh()
                    onBack()
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "summary") {
                    GlassCard(contentPadding = PaddingValues(16.dp)) {
                        Column {
                            Text(
                                text = level?.let { HskCourse.levelDescription(it) }
                                    ?: "Слова курса, разложенные по разделам",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Знаю $learned из $total",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MintAccent,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Ответы в играх точнее 75 %",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            GradientProgress(
                                progress = if (total == 0) 0f else learned.toFloat() / total,
                                height = 8.dp
                            )
                            if (total >= 3) {
                                Spacer(Modifier.height(14.dp))
                                GradientButton(
                                    text = "Играть со всем уровнем",
                                    icon = Icons.Filled.PlayArrow,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        sounds.click()
                                        onPlayWithDeck(deckId)
                                    }
                                )
                            }
                        }
                    }
                }

                item(key = "sections_title") {
                    Text(
                        text = "Разделы",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                    )
                }

                items(sections, key = { it.id }) { section ->
                    SectionRow(
                        deck = section,
                        wordCount = wordCounts[section.id] ?: 0,
                        learnedCount = learnedCounts[section.id] ?: 0,
                        onClick = {
                            sounds.click()
                            onOpenDeck(section.id)
                        }
                    )
                }

                if (sections.isEmpty()) {
                    item {
                        Text(
                            text = "Разделы появятся через пару секунд — папка ещё заполняется.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionRow(
    deck: Deck,
    wordCount: Int,
    learnedCount: Int,
    onClick: () -> Unit
) {
    GlassCard(onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(SkyAccent.copy(alpha = 0.28f), Color.White.copy(alpha = 0.06f))
                        )
                    )
                    .border(1.dp, SkyAccent.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = deck.emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deck.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = wordsLabel(wordCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    if (learnedCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "· знаю $learnedCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MintAccent
                        )
                    }
                }
                Spacer(Modifier.height(7.dp))
                GradientProgress(
                    progress = if (wordCount == 0) 0f else learnedCount.toFloat() / wordCount,
                    height = 5.dp
                )
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = SkyAccent
            )
        }
    }
}

private fun sectionsLabel(count: Int): String {
    val form = when {
        count % 10 == 1 && count % 100 != 11 -> "раздел"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "раздела"
        else -> "разделов"
    }
    return "$count $form"
}
