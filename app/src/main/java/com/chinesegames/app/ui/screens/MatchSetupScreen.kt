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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.chinesegames.app.ui.DeckViewModel
import com.chinesegames.app.ui.cardsLabel
import com.chinesegames.app.ui.components.CgTopBar
import com.chinesegames.app.ui.components.EmptyState
import com.chinesegames.app.ui.components.GlassCard
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.SelectChip
import com.chinesegames.app.ui.components.SectionTitle
import com.chinesegames.app.ui.components.VSpace
import com.chinesegames.app.ui.game.GameMode
import com.chinesegames.app.ui.game.MatchViewModel
import com.chinesegames.app.ui.pairsLabel
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.RoseAccent
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary
import com.chinesegames.app.ui.theme.VividPurple
import com.chinesegames.app.ui.wordsLabel
import kotlin.math.roundToInt

/**
 * Настройка партии «Найди пару»: папки, количество карт и режим.
 */
@Composable
fun MatchSetupScreen(
    viewModel: DeckViewModel,
    onBack: () -> Unit,
    onStart: (deckIds: List<Long>, pairs: Int, mode: GameMode) -> Unit
) {
    val sounds = LocalSounds.current
    val decks by viewModel.decks.collectAsState()
    val wordCounts by viewModel.wordCounts.collectAsState()

    var selectedDecks by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pairs by remember { mutableIntStateOf(10) }
    var mode by remember { mutableStateOf(GameMode.CLASSIC) }

    // Как только папки загрузились — сразу выбираем все
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(decks) {
        if (!initialized && decks.isNotEmpty()) {
            selectedDecks = decks.map { it.id }.toSet()
            initialized = true
        }
    }

    val availableWords = selectedDecks.sumOf { wordCounts[it] ?: 0 }
    val effectivePairs = if (availableWords == 0) 0 else minOf(pairs, availableWords)
    val canStart = selectedDecks.isNotEmpty() && availableWords >= MatchViewModel.MIN_WORDS

    PurpleBackground {
        Box(Modifier.fillMaxSize()) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(bottom = 200.dp)
            ) {
                item {
                    CgTopBar(
                        title = "Найди пару",
                        subtitle = "Настройка партии",
                        onBack = {
                            sounds.whoosh()
                            onBack()
                        }
                    )
                }

                if (decks.isEmpty()) {
                    item {
                        EmptyState(
                            glyph = "空",
                            title = "Нет ни одной папки",
                            message = "Сначала создайте папку в разделе «Словарь» и добавьте в неё слова — " +
                                "игра берёт слова именно оттуда."
                        )
                    }
                    return@LazyColumn
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle("Папки со словами", modifier = Modifier.weight(1f))
                        Text(
                            text = if (selectedDecks.size == decks.size) "Снять всё" else "Выбрать всё",
                            style = MaterialTheme.typography.labelMedium,
                            color = LavenderGlow,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    sounds.click()
                                    selectedDecks = if (selectedDecks.size == decks.size) {
                                        emptySet()
                                    } else {
                                        decks.map { it.id }.toSet()
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    VSpace(10.dp)
                }

                items(decks, key = { it.id }) { deck ->
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                        DeckSelectRow(
                            deck = deck,
                            wordCount = wordCounts[deck.id] ?: 0,
                            selected = selectedDecks.contains(deck.id),
                            onClick = {
                                sounds.click()
                                selectedDecks = if (selectedDecks.contains(deck.id)) {
                                    selectedDecks - deck.id
                                } else {
                                    selectedDecks + deck.id
                                }
                            }
                        )
                    }
                }

                item {
                    VSpace(22.dp)
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        SectionTitle("Сколько карт в игре")
                    }
                    VSpace(10.dp)

                    GlassCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$pairs",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    brush = Brush.horizontalGradient(CG.primaryGradient)
                                )
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = pairsLabel(pairs) + " · " + cardsLabel(pairs * 2),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Slider(
                            value = pairs.toFloat(),
                            onValueChange = { pairs = it.roundToInt() },
                            valueRange = MatchViewModel.MIN_PAIRS.toFloat()..MatchViewModel.MAX_PAIRS.toFloat(),
                            steps = MatchViewModel.MAX_PAIRS - MatchViewModel.MIN_PAIRS - 1,
                            colors = SliderDefaults.colors(
                                thumbColor = VividPurple,
                                activeTrackColor = VividPurple,
                                inactiveTrackColor = Color.White.copy(alpha = 0.14f)
                            )
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(6, 8, 10, 12, 16, 20).forEach { value ->
                                SelectChip(
                                    text = "$value",
                                    selected = pairs == value,
                                    onClick = {
                                        sounds.click()
                                        pairs = value
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    VSpace(22.dp)
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        SectionTitle("Режим")
                    }
                    VSpace(10.dp)
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GameMode.entries.forEach { m ->
                            ModeRow(
                                mode = m,
                                selected = m == mode,
                                onClick = {
                                    sounds.click()
                                    mode = m
                                }
                            )
                        }
                    }
                }

                item {
                    VSpace(22.dp)
                    val hint = when {
                        selectedDecks.isEmpty() -> "Выберите хотя бы одну папку"
                        availableWords < MatchViewModel.MIN_WORDS ->
                            "В выбранных папках всего $availableWords слов — нужно минимум 3"

                        effectivePairs < pairs ->
                            "В папках только $availableWords ${wordsLabel(availableWords).substringAfter(" ")} — " +
                                "сыграем $effectivePairs ${pairsLabel(effectivePairs).substringAfter(" ")}"

                        else -> "Слова выберет случайно из всех выбранных папок"
                    }
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (canStart) TextMuted else RoseAccent
                        )
                    }
                    VSpace(14.dp)
                }
            }

            // Нижняя панель: итог и кнопка старта
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xF20B0618), Color(0xFF0B0618))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (availableWords > 0) wordsLabel(availableWords) + " доступно" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MintAccent
                    )
                    Text(
                        text = if (canStart) cardsLabel(effectivePairs * 2) else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }
                GradientButton(
                    text = "Начать игру",
                    icon = Icons.Filled.PlayArrow,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canStart,
                    contentPadding = PaddingValues(vertical = 17.dp)
                ) {
                    sounds.match()
                    onStart(selectedDecks.toList(), effectivePairs, mode)
                }
            }
        }
    }
}

@Composable
private fun DeckSelectRow(
    deck: Deck,
    wordCount: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) Brush.linearGradient(
                    listOf(VividPurple.copy(alpha = 0.32f), Color.White.copy(alpha = 0.05f))
                )
                else Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.03f))
                )
            )
            .border(
                1.dp,
                if (selected) LavenderGlow.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f),
                shape
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = deck.emoji, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = deck.name,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = wordsLabel(wordCount),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    if (selected) Brush.horizontalGradient(CG.primaryGradient)
                    else Brush.horizontalGradient(
                        listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.05f))
                    )
                )
                .border(
                    1.dp,
                    if (selected) Color.Transparent else LavenderGlow.copy(alpha = 0.35f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ModeRow(
    mode: GameMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) Brush.linearGradient(
                    listOf(Color(0xFF7C3AED).copy(alpha = 0.55f), Color(0xFFD946EF).copy(alpha = 0.28f))
                )
                else Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.03f))
                )
            )
            .border(
                1.dp,
                if (selected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                shape
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = mode.emoji, fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = mode.hint,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) TextPrimary.copy(alpha = 0.85f) else TextMuted
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
