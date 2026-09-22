package com.chinesegames.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.chinesegames.app.data.DeckRepository
import com.chinesegames.app.data.DisplayMode
import com.chinesegames.app.ui.DeckViewModel
import com.chinesegames.app.ui.cardsLabel
import com.chinesegames.app.ui.components.CgTopBar
import com.chinesegames.app.ui.components.DeckSelectionColumn
import com.chinesegames.app.ui.components.DeckSelectionHeader
import com.chinesegames.app.ui.components.EmptyState
import com.chinesegames.app.ui.components.GlassCard
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.PixelDivider
import com.chinesegames.app.ui.components.PixelSpriteFit
import com.chinesegames.app.ui.components.PixelTag
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.SakuraSprite
import com.chinesegames.app.ui.components.SelectChip
import com.chinesegames.app.ui.components.SectionTitle
import com.chinesegames.app.ui.components.ToriiSprite
import com.chinesegames.app.ui.components.VSpace
import com.chinesegames.app.ui.game.GameKind
import com.chinesegames.app.ui.game.GameMode
import com.chinesegames.app.ui.game.QuizPreset
import com.chinesegames.app.ui.game.QuizPresets
import com.chinesegames.app.ui.game.SettingsCodec
import com.chinesegames.app.ui.pairsLabel
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.LocalSettings
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.PixelType
import com.chinesegames.app.ui.theme.RoseAccent
import com.chinesegames.app.ui.theme.SkyAccent
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary
import com.chinesegames.app.ui.theme.VividPurple
import com.chinesegames.app.ui.wordsLabel
import kotlin.math.roundToInt

/**
 * Универсальная настройка партии: выбор папок (включая «Избранное» и «Сложные
 * слова»), количество слов/пар, вариантов ответа, таймеров и правила
 * интервального повторения. Работает для всех игр.
 */
@Composable
fun GameSetupScreen(
    kind: GameKind,
    viewModel: DeckViewModel,
    onBack: () -> Unit,
    onStartMatch: (deckIds: List<Long>, pairs: Int, mode: GameMode, previewSeconds: Int) -> Unit,
    onStartQuiz: (kind: GameKind, deckIds: List<Long>, settings: String) -> Unit,
    preselect: List<Long> = emptyList()
) {
    val sounds = LocalSounds.current
    val settings = LocalSettings.current
    val appSettings by settings.state.collectAsState()

    val decks by viewModel.decks.collectAsState()
    val wordCounts by viewModel.wordCounts.collectAsState()
    val favoritesCount by viewModel.favoritesCount.collectAsState()
    val hardWordsCount by viewModel.hardWordCount.collectAsState()

    val preset = remember(kind) { QuizPresets.of(kind) }

    var selectedDecks by remember { mutableStateOf<Set<Long>>(presetSelections(preselect)) }
    var initialized by remember { mutableStateOf(false) }
    var rounds by remember { mutableIntStateOf(preset.defaultQuestions) }
    var options by remember { mutableIntStateOf(preset.defaultOptions) }
    var secondsPerQuestion by remember { mutableIntStateOf(preset.defaultSecondsPerQuestion) }
    var totalSeconds by remember { mutableIntStateOf(preset.defaultTotalSeconds) }
    var memorize by remember { mutableIntStateOf(preset.defaultMemorize) }
    var memorizeSeconds by remember { mutableIntStateOf(preset.defaultMemorizeSeconds) }
    var pairs by remember { mutableIntStateOf(preset.defaultPairs) }
    var mode by remember { mutableStateOf(GameMode.forKind(kind)) }
    var srsFirst by remember { mutableStateOf(appSettings.srsFirst) }

    // Все папки выбраны по умолчанию (или то, что пришло из «Избранного»)
    LaunchedEffect(decks, favoritesCount, hardWordsCount) {
        if (!initialized && (decks.isNotEmpty() || favoritesCount > 0 || hardWordsCount > 0)) {
            if (preselect.isEmpty()) {
                selectedDecks = buildSet {
                    decks.forEach { add(it.id) }
                    if (favoritesCount > 0) add(DeckRepository.FAVORITES_ID)
                }
            }
            initialized = true
        }
    }

    val canStart = selectedDecks.isNotEmpty()

    PurpleBackground(petals = true) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(bottom = 210.dp)
            ) {
                item {
                    CgTopBar(
                        title = kind.title,
                        subtitle = "Настройка партии · ${kind.tagline}",
                        emoji = kind.emoji,
                        onBack = {
                            sounds.whoosh()
                            onBack()
                        }
                    )
                }

                if (decks.isEmpty() && favoritesCount == 0 && hardWordsCount == 0) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelSpriteFit(sprite = ToriiSprite, width = 26.dp, height = 24.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Правила",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = kind.rules,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    VSpace(16.dp)
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        DeckSelectionHeader(
                            allSelected = selectedDecks.size >= decks.size + countPseudo(
                                favoritesCount, hardWordsCount
                            ),
                            onToggleAll = {
                                sounds.click()
                                selectedDecks = if (selectedDecks.size >= decks.size + countPseudo(
                                        favoritesCount, hardWordsCount
                                    )
                                ) {
                                    emptySet()
                                } else {
                                    buildSet {
                                        decks.forEach { add(it.id) }
                                        if (favoritesCount > 0) add(DeckRepository.FAVORITES_ID)
                                        if (hardWordsCount > 0) add(DeckRepository.HARD_WORDS_ID)
                                    }
                                }
                            }
                        )
                    }
                    VSpace(10.dp)
                }

                item {
                    DeckSelectionColumn(
                        decks = decks,
                        wordCounts = wordCounts,
                        favoritesCount = favoritesCount,
                        hardWordsCount = hardWordsCount,
                        selected = selectedDecks,
                        onToggle = { id ->
                            sounds.click()
                            selectedDecks = if (selectedDecks.contains(id)) {
                                selectedDecks - id
                            } else {
                                selectedDecks + id
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    VSpace(20.dp)
                }

                item {
                    // Что показывать в игре: иероглифы, пиньинь или и то и другое
                    Box(Modifier.padding(horizontal = 16.dp)) { SectionTitle("Отображение") }
                    VSpace(10.dp)
                    GlassCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DisplayMode.entries.forEach { displayMode ->
                                SelectChip(
                                    text = displayMode.shortTitle,
                                    selected = appSettings.displayMode == displayMode,
                                    onClick = {
                                        sounds.click()
                                        settings.setDisplayMode(displayMode)
                                    }
                                )
                            }
                        }
                        VSpace(10.dp)
                        Text(
                            text = appSettings.displayMode.hint,
                            style = PixelType.caption,
                            color = TextMuted
                        )
                    }
                    VSpace(20.dp)
                }

                if (kind == GameKind.MATCH) {
                    item {
                        Box(Modifier.padding(horizontal = 16.dp)) { SectionTitle("Режим") }
                        VSpace(10.dp)
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            GameMode.entries.forEach { m ->
                                ModeRow(mode = m, selected = m == mode) {
                                    sounds.click()
                                    mode = m
                                }
                            }
                        }
                        VSpace(20.dp)
                    }
                }

                if (preset.allowPairs) {
                    item {
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            SectionTitle(preset.roundsLabel)
                        }
                        VSpace(10.dp)
                        GlassCard(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(16.dp)
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
                                    style = PixelType.caption,
                                    color = TextSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Slider(
                                value = pairs.toFloat(),
                                onValueChange = { pairs = it.roundToInt() },
                                valueRange = 3f..30f,
                                steps = 26,
                                colors = SliderDefaults.colors(
                                    thumbColor = VividPurple,
                                    activeTrackColor = VividPurple,
                                    inactiveTrackColor = CG.glass(0.14f)
                                )
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                preset.pairChoices.forEach { value ->
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
                        VSpace(20.dp)
                    }
                }

                if (preset.allowMemorize) {
                    item {
                        Box(Modifier.padding(horizontal = 16.dp)) { SectionTitle("Сколько слов показывать сразу") }
                        VSpace(10.dp)
                        ChipRow(
                            values = preset.memorizeChoices,
                            selected = memorize,
                            label = { "$it слов" },
                            onSelect = {
                                sounds.click()
                                memorize = it
                            }
                        )
                        VSpace(18.dp)
                    }
                }

                if (preset.allowMemorizeSeconds) {
                    item {
                        Box(Modifier.padding(horizontal = 16.dp)) { SectionTitle("Секунд на запоминание") }
                        VSpace(10.dp)
                        ChipRow(
                            values = preset.memorizeSecondChoices,
                            selected = memorizeSeconds,
                            label = { "$it с" },
                            onSelect = {
                                sounds.click()
                                memorizeSeconds = it
                            }
                        )
                        VSpace(18.dp)
                    }
                }

                if (!preset.allowPairs && !preset.allowTotalSeconds) {
                    item {
                        Box(Modifier.padding(horizontal = 16.dp)) { SectionTitle(preset.roundsLabel) }
                        VSpace(10.dp)
                        ChipRow(
                            values = preset.questionChoices,
                            selected = rounds,
                            label = { "$it" },
                            onSelect = {
                                sounds.click()
                                rounds = it
                            }
                        )
                        VSpace(18.dp)
                    }
                }

                if (preset.allowTotalSeconds) {
                    item {
                        Box(Modifier.padding(horizontal = 16.dp)) { SectionTitle("Сколько времени") }
                        VSpace(10.dp)
                        ChipRow(
                            values = preset.totalChoices,
                            selected = totalSeconds,
                            label = { "$it с" },
                            onSelect = {
                                sounds.click()
                                totalSeconds = it
                            }
                        )
                        VSpace(18.dp)
                    }
                }

                if (preset.allowOptions) {
                    item {
                        Box(Modifier.padding(horizontal = 16.dp)) { SectionTitle("Вариантов ответа") }
                        VSpace(10.dp)
                        ChipRow(
                            values = preset.optionChoices,
                            selected = options,
                            label = { "$it" },
                            onSelect = {
                                sounds.click()
                                options = it
                            }
                        )
                        VSpace(18.dp)
                    }
                }

                if (preset.allowSecondsPerQuestion && !preset.allowTotalSeconds) {
                    item {
                        Box(Modifier.padding(horizontal = 16.dp)) { SectionTitle("Секунд на слово") }
                        VSpace(10.dp)
                        ChipRow(
                            values = preset.secondChoices,
                            selected = secondsPerQuestion,
                            label = { if (it == 0) "без лимита" else "$it с" },
                            onSelect = {
                                sounds.click()
                                secondsPerQuestion = it
                            }
                        )
                        VSpace(18.dp)
                    }
                }

                item {
                    Box(Modifier.padding(horizontal = 16.dp)) { SectionTitle("Повторение") }
                    VSpace(10.dp)
                    GlassCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.School,
                                contentDescription = null,
                                tint = MintAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Сначала слова с низкой точностью",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Интервальное повторение: слабые и подзабытые слова попадаются чаще",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            Switch(
                                checked = srsFirst,
                                onCheckedChange = {
                                    srsFirst = it
                                    settings.setSrsFirst(it)
                                    sounds.click()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VividPurple,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = CG.glass(0.12f)
                                )
                            )
                        }
                    }
                    VSpace(14.dp)
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelTag(text = "MIN 3 СЛОВА", color = SkyAccent)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Слова выбираются случайно из выбранных папок",
                            style = PixelType.caption,
                            color = TextMuted
                        )
                    }
                    VSpace(16.dp)
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        PixelDivider()
                    }
                    VSpace(16.dp)
                }
            }

            // Нижняя панель с итогом и кнопкой старта
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, CG.scrim, CG.scrim))
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
                        text = if (selectedDecks.isEmpty()) {
                            "Выберите хотя бы одну папку"
                        } else {
                            "${selectedDecks.size} источник(ов) слов"
                        },
                        style = PixelType.caption,
                        color = if (canStart) MintAccent else RoseAccent
                    )
                    SelectedWordsHint(
                        viewModel = viewModel,
                        selection = selectedDecks.toList(),
                        kind = kind,
                        pairs = pairs,
                        preset = preset
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
                    val selection = selectedDecks.toList()
                    if (kind == GameKind.MATCH || kind == GameKind.MEMORY_GRID) {
                        onStartMatch(
                            selection,
                            pairs,
                            mode,
                            if (kind == GameKind.MEMORY_GRID) memorizeSeconds else 0
                        )
                    } else {
                        onStartQuiz(kind, selection, settingsToken(preset, rounds, options, secondsPerQuestion, totalSeconds, memorize, memorizeSeconds, srsFirst))
                    }
                }
            }

            // Пиксельный уголок сакуры — декоративная деталь экрана
            PixelSpriteFit(
                sprite = SakuraSprite,
                width = 66.dp,
                height = 54.dp,
                alpha = 0.5f,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 10.dp)
            )
        }
    }
}

@Composable
private fun SelectedWordsHint(
    viewModel: DeckViewModel,
    selection: List<Long>,
    kind: GameKind,
    pairs: Int,
    preset: QuizPreset
) {
    var count by remember { mutableIntStateOf(0) }
    LaunchedEffect(selection) {
        viewModel.countForSelection(selection) { count = it }
    }
    val label = when {
        kind.isCardGame -> cardsLabel(minOf(pairs, count) * 2)
        preset.allowTotalSeconds -> "$count слов в очереди"
        else -> wordsLabel(count)
    }
    Text(text = label, style = PixelType.caption, color = TextMuted)
}

@Composable
private fun ChipRow(
    values: List<Int>,
    selected: Int,
    label: (Int) -> String,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        values.forEach { value ->
            SelectChip(text = label(value), selected = selected == value) { onSelect(value) }
        }
    }
}

@Composable
private fun ModeRow(
    mode: GameMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) Brush.linearGradient(
                    listOf(VividPurple.copy(alpha = 0.5f), CG.glass(0.06f))
                ) else Brush.linearGradient(
                    listOf(CG.glass(0.05f), CG.glass(0.03f))
                )
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = mode.emoji, fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = mode.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(text = mode.hint, style = PixelType.caption, color = TextMuted)
        }
        if (selected) PixelTag(text = "ВЫБРАНО", color = GoldAccent)
    }
}

private fun countPseudo(favoritesCount: Int, hardWordsCount: Int): Int =
    (if (favoritesCount > 0) 1 else 0) + (if (hardWordsCount > 0) 1 else 0)

private fun presetSelections(preselect: List<Long>): Set<Long> =
    if (preselect.isEmpty()) emptySet() else preselect.toSet()

private fun settingsToken(
    preset: QuizPreset,
    rounds: Int,
    options: Int,
    secondsPerQuestion: Int,
    totalSeconds: Int,
    memorize: Int,
    memorizeSeconds: Int,
    srsFirst: Boolean
): String = SettingsCodec.encode(
    mapOf(
        SettingsCodec.KEY_QUESTIONS to rounds,
        SettingsCodec.KEY_OPTIONS to options,
        SettingsCodec.KEY_SECONDS to secondsPerQuestion,
        SettingsCodec.KEY_TOTAL to totalSeconds,
        SettingsCodec.KEY_MEMORIZE to if (preset.allowMemorize) memorize else 0,
        SettingsCodec.KEY_MEMORIZE_SECONDS to memorizeSeconds,
        SettingsCodec.KEY_SRS to if (srsFirst) 1 else 0
    )
)
