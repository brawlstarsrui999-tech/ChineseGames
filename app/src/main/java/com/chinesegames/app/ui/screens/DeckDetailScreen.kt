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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.chinesegames.app.data.Word
import com.chinesegames.app.ui.DeckViewModel
import com.chinesegames.app.ui.components.CgTopBar
import com.chinesegames.app.ui.components.ConfirmDialog
import com.chinesegames.app.ui.components.DeckEditorDialog
import com.chinesegames.app.ui.components.EmptyState
import com.chinesegames.app.ui.components.GlassCard
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.SearchField
import com.chinesegames.app.ui.components.WordEditorDialog
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.LocalSpeaker
import com.chinesegames.app.ui.theme.RoseAccent
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary
import com.chinesegames.app.ui.theme.VividPurple
import com.chinesegames.app.ui.wordsLabel

/**
 * Экран одной папки: список слов + добавление, изменение и озвучка.
 */
@Composable
fun DeckDetailScreen(
    deckId: Long,
    viewModel: DeckViewModel,
    onBack: () -> Unit,
    onPlayWithDeck: () -> Unit
) {
    val sounds = LocalSounds.current
    val speaker = LocalSpeaker.current

    val deckFlow = remember(deckId) { viewModel.deckFlow(deckId) }
    val wordsFlow = remember(deckId) { viewModel.wordsOf(deckId) }
    val deck by deckFlow.collectAsState(initial = null)
    val words by wordsFlow.collectAsState(initial = emptyList())

    var query by remember { mutableStateOf("") }
    var showAddWord by remember { mutableStateOf(false) }
    var wordToEdit by remember { mutableStateOf<Word?>(null) }
    var wordToDelete by remember { mutableStateOf<Word?>(null) }
    var showEditDeck by remember { mutableStateOf(false) }
    var showDeleteDeck by remember { mutableStateOf(false) }

    val filtered = remember(words, query) {
        val q = query.trim()
        if (q.isBlank()) words
        else words.filter {
            it.hanzi.contains(q, ignoreCase = true) ||
                it.pinyin.contains(q, ignoreCase = true) ||
                it.translation.contains(q, ignoreCase = true)
        }
    }

    PurpleBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            CgTopBar(
                title = deck?.name ?: "Папка",
                subtitle = wordsLabel(words.size),
                emoji = deck?.emoji,
                onBack = {
                    sounds.whoosh()
                    onBack()
                },
                actions = {
                    Box {
                        var open by remember { mutableStateOf(false) }
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Меню папки",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable { open = true }
                                .padding(10.dp)
                        )
                        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                            DropdownMenuItem(
                                text = { Text("Изменить папку") },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                onClick = {
                                    open = false
                                    showEditDeck = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Удалить папку", color = RoseAccent) },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                onClick = {
                                    open = false
                                    showDeleteDeck = true
                                }
                            )
                        }
                    }
                }
            )

            if (words.isNotEmpty()) {
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Поиск: 汉字, пиньинь или перевод",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(12.dp))

                if (words.size >= 3) {
                    GradientButton(
                        text = "Играть с этой папкой",
                        icon = Icons.Filled.PlayArrow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        onClick = {
                            sounds.click()
                            onPlayWithDeck()
                        }
                    )
                    Spacer(Modifier.height(14.dp))
                }
            }

            if (words.isEmpty()) {
                EmptyState(
                    glyph = "词",
                    title = "В папке пока нет слов",
                    message = "Добавьте слово: иероглиф, пиньинь и перевод на русский. " +
                        "Слов можно добавить сколько угодно.",
                    action = {
                        GradientButton(text = "Добавить слово", icon = Icons.Filled.Add) {
                            sounds.click()
                            showAddWord = true
                        }
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { word ->
                        WordRow(
                            word = word,
                            onSpeak = {
                                sounds.click()
                                speaker.speak(it.hanzi)
                            },
                            onEdit = {
                                sounds.click()
                                wordToEdit = word
                            },
                            onDelete = {
                                sounds.click()
                                wordToDelete = word
                            }
                        )
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                text = "Ничего не найдено",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                modifier = Modifier.fillMaxWidth().padding(24.dp)
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomEnd
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    sounds.click()
                    showAddWord = true
                },
                containerColor = VividPurple,
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Добавить слово", fontWeight = FontWeight.Bold) },
                modifier = Modifier.padding(20.dp)
            )
        }
    }

    if (showAddWord) {
        WordEditorDialog(
            word = null,
            onDismiss = { showAddWord = false },
            onSpeak = { speaker.speak(it) },
            onSave = { hanzi, pinyin, translation ->
                sounds.match()
                viewModel.addWord(deckId, hanzi, pinyin, translation)
            }
        )
    }

    wordToEdit?.let { word ->
        WordEditorDialog(
            word = word,
            onDismiss = { wordToEdit = null },
            onSpeak = { speaker.speak(it) },
            onSave = { hanzi, pinyin, translation ->
                sounds.click()
                viewModel.updateWord(word, hanzi, pinyin, translation)
            }
        )
    }

    wordToDelete?.let { word ->
        ConfirmDialog(
            title = "Удалить слово?",
            message = "«${word.hanzi}» — ${word.translation} исчезнет из словаря.",
            onDismiss = { wordToDelete = null },
            onConfirm = {
                sounds.error()
                viewModel.deleteWord(word)
            }
        )
    }

    val currentDeck: Deck? = deck
    if (showEditDeck && currentDeck != null) {
        DeckEditorDialog(
            deck = currentDeck,
            onDismiss = { showEditDeck = false },
            onSave = { name, emoji ->
                sounds.click()
                viewModel.updateDeck(currentDeck, name, emoji)
            }
        )
    }

    if (showDeleteDeck && currentDeck != null) {
        ConfirmDialog(
            title = "Удалить папку?",
            message = "Папка «${currentDeck.name}» и все ${words.size} слов из неё будут удалены.",
            onDismiss = { showDeleteDeck = false },
            onConfirm = {
                sounds.error()
                viewModel.deleteDeck(currentDeck)
                onBack()
            }
        )
    }
}

@Composable
private fun WordRow(
    word: Word,
    onSpeak: (Word) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    GlassCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = word.hanzi,
                        fontSize = 28.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    if (word.pinyin.isNotBlank()) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = word.pinyin,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LavenderGlow,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = word.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .clickable { onSpeak(word) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Озвучить",
                    tint = LavenderGlow,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Меню слова",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { menuOpen = true }
                        .padding(8.dp)
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Изменить") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Озвучить") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onSpeak(word)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Удалить", color = RoseAccent) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

