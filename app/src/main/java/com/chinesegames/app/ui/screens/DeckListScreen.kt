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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chinesegames.app.data.Deck
import com.chinesegames.app.data.HskCourse
import com.chinesegames.app.ui.DeckViewModel
import com.chinesegames.app.ui.components.CgTopBar
import com.chinesegames.app.ui.components.DeckEditorDialog
import com.chinesegames.app.ui.components.ConfirmDialog
import com.chinesegames.app.ui.components.EmptyState
import com.chinesegames.app.ui.components.GlassCard
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.GradientProgress
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.SearchField
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.RoseAccent
import com.chinesegames.app.ui.theme.SkyAccent
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary
import com.chinesegames.app.ui.theme.VividPurple
import com.chinesegames.app.ui.decksLabel
import com.chinesegames.app.ui.wordsLabel

/**
 * Список папок («колод») со словами.
 */
@Composable
fun DeckListScreen(
    viewModel: DeckViewModel,
    favoritesCount: Int,
    hardWordsCount: Int,
    onBack: (() -> Unit)? = null,
    onOpenDeck: (Long) -> Unit,
    onOpenFolder: (Long) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenHardWords: () -> Unit
) {
    val sounds = LocalSounds.current
    val decks by viewModel.decks.collectAsState()
    val wordCounts by viewModel.wordCounts.collectAsState()
    val learnedCounts by viewModel.learnedCounts.collectAsState()

    var query by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    var deckToEdit by remember { mutableStateOf<Deck?>(null) }
    var deckToDelete by remember { mutableStateOf<Deck?>(null) }

    // Папки курса «HSK 1» … «HSK 7» показываем отдельным блоком; их разделы
    // (подпапки) открываются внутри папки уровня. Остальное — папки пользователя.
    val courseLevels = remember(decks) { decks.filter { it.isCourseLevel } }
    val ownDecks = remember(decks) { decks.filter { !it.isCourse } }

    val filtered = remember(ownDecks, query) {
        if (query.isBlank()) ownDecks
        else ownDecks.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }
    val filteredCourse = remember(courseLevels, query) {
        if (query.isBlank()) courseLevels
        else courseLevels.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }

    PurpleBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            CgTopBar(
                title = "Словарь",
                subtitle = if (ownDecks.isEmpty()) "Курс HSK и ваши папки" else decksLabel(ownDecks.size),
                onBack = onBack?.let { handler ->
                    {
                        sounds.whoosh()
                        handler()
                    }
                }
            )

            if (ownDecks.size > 3) {
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Поиск по названию папки",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(12.dp))
            }

            if (decks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    EmptyState(
                        glyph = "字",
                        title = "Папок пока нет",
                        message = "Папка — это тема слов: «Еда», «Путешествия», «Мой урок». " +
                            "Внутри папки можно хранить сколько угодно слов.",
                        action = {
                            GradientButton(
                                text = "Создать первую папку",
                                icon = Icons.Filled.Add
                            ) {
                                sounds.click()
                                showCreate = true
                            }
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Папка «Избранное» — слова со звёздочкой из всех папок
                    item(key = "favorites") {
                        SpecialRow(
                            emoji = "⭐",
                            title = "Избранное",
                            subtitle = if (favoritesCount == 0) {
                                "Отметьте звёздочкой слова из любой папки"
                            } else {
                                wordsLabel(favoritesCount)
                            },
                            accent = GoldAccent,
                            onClick = {
                                sounds.click()
                                onOpenFavorites()
                            }
                        )
                    }

                    if (hardWordsCount > 0) {
                        item(key = "hard") {
                            SpecialRow(
                                emoji = "🔥",
                                title = "Сложные слова",
                                subtitle = "Пока отвечаются хуже всего · $hardWordsCount",
                                accent = RoseAccent,
                                onClick = {
                                    sounds.click()
                                    onOpenHardWords()
                                }
                            )
                        }
                    }

                    // Папки курса: слова уровней HSK 1–7 и их разделов — играйте с любыми
                    if (filteredCourse.isNotEmpty()) {
                        item(key = "course_header") {
                            SectionHeader(
                                title = "Курс HSK",
                                subtitle = "Те же слова, что в курсе: уровень целиком или отдельный раздел"
                            )
                        }
                        items(filteredCourse, key = { "course_${it.id}" }) { deck ->
                            CourseFolderRow(
                                deck = deck,
                                wordCount = wordCounts[deck.id] ?: 0,
                                learnedCount = learnedCounts[deck.id] ?: 0,
                                onClick = {
                                    sounds.click()
                                    onOpenFolder(deck.id)
                                }
                            )
                        }
                    }

                    item(key = "own_header") {
                        SectionHeader(
                            title = "Мои папки",
                            subtitle = if (ownDecks.isEmpty()) {
                                "Создайте папку и добавьте свои слова — из учебника или урока"
                            } else {
                                decksLabel(ownDecks.size)
                            }
                        )
                    }

                    items(filtered, key = { it.id }) { deck ->
                        DeckRow(
                            deck = deck,
                            wordCount = wordCounts[deck.id] ?: 0,
                            learnedCount = learnedCounts[deck.id] ?: 0,
                            onClick = {
                                sounds.click()
                                onOpenDeck(deck.id)
                            },
                            onEdit = {
                                sounds.click()
                                deckToEdit = deck
                            },
                            onDelete = {
                                sounds.click()
                                deckToDelete = deck
                            }
                        )
                    }
                    if (filtered.isEmpty() && query.isNotBlank()) {
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

        // Кнопка создания новой папки
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomEnd
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    sounds.click()
                    showCreate = true
                },
                containerColor = VividPurple,
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Новая папка", fontWeight = FontWeight.Bold) },
                modifier = Modifier.padding(20.dp)
            )
        }
    }

    if (showCreate) {
        DeckEditorDialog(
            deck = null,
            onDismiss = { showCreate = false },
            onSave = { name, emoji ->
                sounds.match()
                viewModel.createDeck(name, emoji)
            }
        )
    }

    deckToEdit?.let { deck ->
        DeckEditorDialog(
            deck = deck,
            onDismiss = { deckToEdit = null },
            onSave = { name, emoji ->
                sounds.click()
                viewModel.updateDeck(deck, name, emoji)
            }
        )
    }

    deckToDelete?.let { deck ->
        ConfirmDialog(
            title = "Удалить папку?",
            message = "Папка «${deck.name}» и все слова из неё будут удалены безвозвратно.",
            onDismiss = { deckToDelete = null },
            onConfirm = {
                sounds.error()
                viewModel.deleteDeck(deck)
            }
        )
    }
}

@Composable
private fun DeckRow(
    deck: Deck,
    wordCount: Int,
    learnedCount: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    GlassCard(onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(VividPurple.copy(alpha = 0.35f), Color.White.copy(alpha = 0.08f))
                        )
                    )
                    .border(1.dp, LavenderGlow.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = deck.emoji, fontSize = 26.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deck.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
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
                Spacer(Modifier.height(9.dp))
                GradientProgress(
                    progress = if (wordCount == 0) 0f else learnedCount.toFloat() / wordCount,
                    height = 6.dp
                )
            }

            Spacer(Modifier.width(6.dp))

            // Системную папку «Выученное» нельзя переименовать или удалить
            if (deck.isSystem) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Системная папка",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(36.dp)
                        .padding(7.dp)
                )
            } else {
                Box {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Меню папки",
                        tint = TextMuted,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { menuOpen = true }
                            .padding(6.dp)
                    )
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Изменить") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onEdit()
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
}

/** Заголовок блока списка («Курс HSK», «Мои папки»). */
@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(start = 4.dp, top = 6.dp, end = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

/** Папка уровня курса: «HSK 3 · 298 слов · 18 разделов». */
@Composable
private fun CourseFolderRow(
    deck: Deck,
    wordCount: Int,
    learnedCount: Int,
    onClick: () -> Unit
) {
    val level = deck.courseLevel ?: 0
    GlassCard(onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(SkyAccent.copy(alpha = 0.35f), Color.White.copy(alpha = 0.08f))
                        )
                    )
                    .border(1.dp, SkyAccent.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = deck.emoji, fontSize = 26.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deck.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (level > 0) HskCourse.levelDescription(level) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
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
                Spacer(Modifier.height(9.dp))
                GradientProgress(
                    progress = if (wordCount == 0) 0f else learnedCount.toFloat() / wordCount,
                    height = 6.dp
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

/** Особый ряд списка: «Избранное» или «Сложные слова». */
@Composable
private fun SpecialRow(
    emoji: String,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(CG.cardGradient))
            .border(1.dp, accent.copy(alpha = 0.55f), shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = accent
        )
    }
}
