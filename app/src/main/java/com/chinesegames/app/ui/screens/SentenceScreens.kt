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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chinesegames.app.data.HskSentenceTopicData
import com.chinesegames.app.ui.components.CgTopBar
import com.chinesegames.app.ui.components.CircleIconButton
import com.chinesegames.app.ui.components.GhostButton
import com.chinesegames.app.ui.components.GlassCard
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.GradientProgress
import com.chinesegames.app.ui.components.HudChip
import com.chinesegames.app.ui.components.PixelTag
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.SentenceAccent
import com.chinesegames.app.ui.components.SentenceGradient
import com.chinesegames.app.ui.components.StudyGradient
import com.chinesegames.app.ui.components.VSpace
import com.chinesegames.app.ui.study.SentenceGameKind
import com.chinesegames.app.ui.study.SentenceGameState
import com.chinesegames.app.ui.study.SentenceGameViewModel
import com.chinesegames.app.ui.study.StudyViewModel
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.LocalSpeaker
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.PixelType
import com.chinesegames.app.ui.theme.RoseAccent
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary

/* ============================ Разделы предложений ============================ */

/** Уровень: разделы с предложениями (4 игры в каждом). */
@Composable
fun SentenceLevelScreen(
    level: Int,
    viewModel: StudyViewModel,
    onBack: () -> Unit,
    onOpenTopic: (String) -> Unit
) {
    val sounds = LocalSounds.current
    val state by viewModel.state.collectAsState()
    val topics = state.sentenceTopics[level].orEmpty()

    LaunchedEffect(level) { viewModel.openSentences() }

    PurpleBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            CgTopBar(
                title = "Предложения HSK $level",
                subtitle = "Разделы фраз · 4 игры в каждом",
                icon = Icons.AutoMirrored.Filled.Chat,
                iconTint = SentenceAccent,
                onBack = {
                    sounds.whoosh()
                    onBack()
                }
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Четыре игры: собрать китайскую фразу, собрать перевод, " +
                        "понять фразу на слух и найти нужную озвучку.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                VSpace(14.dp)

                if (topics.isEmpty()) {
                    Text(
                        text = "Загружаем предложения…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                } else {
                    topics.forEach { topic ->
                        SentenceTopicCard(
                            topic = topic,
                            progress = viewModel.sentenceTopicProgress(topic),
                            onClick = {
                                sounds.click()
                                onOpenTopic(topic.id)
                            }
                        )
                        VSpace(12.dp)
                    }
                }
                VSpace(24.dp)
            }
        }
    }
}

@Composable
private fun SentenceTopicCard(
    topic: HskSentenceTopicData,
    progress: Float,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = topic.emoji.ifBlank { "💬" }, fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${topic.sentences.size} предложений · 4 игры",
                    style = PixelType.caption,
                    color = TextSecondary
                )
            }
            Text(
                text = "${(progress * 100).toInt()}%",
                style = PixelType.chip,
                color = SentenceAccent
            )
        }
        VSpace(10.dp)
        GradientProgress(progress = progress, colors = StudyGradient, height = 6.dp)
    }
}

/** Раздел предложений: четыре игры. */
@Composable
fun SentenceTopicScreen(
    level: Int,
    topicId: String,
    viewModel: StudyViewModel,
    onBack: () -> Unit,
    onOpenGame: (String) -> Unit
) {
    val sounds = LocalSounds.current
    val state by viewModel.state.collectAsState()
    val topic = state.sentenceTopics[level]?.firstOrNull { it.id == topicId }
    val progress = topic?.let { viewModel.sentenceTopicProgress(it) } ?: 0f
    val mask = topic?.let { state.sentenceProgress[it.key]?.passedMask } ?: 0

    LaunchedEffect(level) { viewModel.openSentences() }

    PurpleBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            CgTopBar(
                title = topic?.title ?: "Предложения",
                subtitle = "HSK $level",
                emoji = topic?.emoji,
                onBack = {
                    sounds.whoosh()
                    onBack()
                }
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                GlassCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Прогресс раздела",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = PixelType.chip,
                            color = SentenceAccent
                        )
                    }
                    VSpace(10.dp)
                    GradientProgress(progress = progress, colors = StudyGradient, height = 8.dp)
                }

                VSpace(16.dp)

                SentenceGameKind.entries.forEach { game ->
                    val passed = mask and game.bit != 0
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(14.dp),
                        onClick = {
                            sounds.click()
                            onOpenGame(game.id)
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(
                                        Brush.linearGradient(
                                            if (passed) CG.successGradient else StudyGradient
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (passed) {
                                        Icons.Filled.CheckCircle
                                    } else {
                                        Icons.Filled.PlayArrow
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = game.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = game.description,
                                    style = PixelType.caption,
                                    color = TextSecondary
                                )
                            }
                            if (passed) PixelTag("ПРОЙДЕНО", color = MintAccent)
                        }
                    }
                    VSpace(10.dp)
                }

                VSpace(24.dp)
            }
        }
    }
}

/* ============================== Игра с фразами ============================== */

/**
 * Игра с предложениями: собираем фразу из плиток или выбираем нужную озвучку.
 */
@Composable
fun SentenceGameScreen(
    level: Int,
    topicId: String,
    gameId: String,
    onExit: () -> Unit,
    viewModel: SentenceGameViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(level, topicId, gameId) {
        viewModel.start(level, topicId, gameId)
    }

    PurpleBackground(petals = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            SentenceHeader(
                title = state.title,
                subtitle = state.subtitle,
                progress = state.progress,
                correct = state.correct,
                mistakes = state.mistakes,
                onBack = onExit,
                onRepeat = { viewModel.repeatAudio() }
            )

            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Готовим предложения…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }

                state.notReady -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "В разделе мало предложений",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(12.dp))
                        GhostButton(text = "Назад", onClick = onExit)
                    }
                }

                state.finished -> SentenceResult(
                    state = state,
                    onRestart = { viewModel.start(level, topicId, gameId) },
                    onExit = onExit
                )

                else -> SentenceQuestion(
                    state = state,
                    onTapTile = { viewModel.tapTile(it) },
                    onClear = { viewModel.clearTiles() },
                    onCheck = { viewModel.checkTiles() },
                    onChooseAudio = { viewModel.chooseAudio(it) },
                    onPlayOption = { viewModel.playOption(it) },
                    onRepeat = { viewModel.repeatAudio() },
                    onSkip = { viewModel.skip() }
                )
            }
        }
    }
}

@Composable
private fun SentenceHeader(
    title: String,
    subtitle: String,
    progress: Float,
    correct: Int,
    mistakes: Int,
    onBack: () -> Unit,
    onRepeat: () -> Unit
) {
    val sounds = LocalSounds.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад"
            ) {
                sounds.whoosh()
                onBack()
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(text = subtitle, style = PixelType.caption, color = TextMuted)
            }
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Повторить звук",
                tint = SentenceAccent
            ) { onRepeat() }
        }
        Spacer(Modifier.height(10.dp))
        GradientProgress(progress = progress, colors = StudyGradient, height = 7.dp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudChip(Icons.Filled.CheckCircle, "верно $correct", MintAccent, Modifier.weight(1f))
            HudChip(Icons.Filled.Close, "ошибки $mistakes", RoseAccent, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SentenceQuestion(
    state: SentenceGameState,
    onTapTile: (Int) -> Unit,
    onClear: () -> Unit,
    onCheck: () -> Unit,
    onChooseAudio: (Int) -> Unit,
    onPlayOption: (Int) -> Unit,
    onRepeat: () -> Unit,
    onSkip: () -> Unit
) {
    val speaker = LocalSpeaker.current
    val sentence = state.sentence ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // Что дано в вопросе
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(CG.cardGradient))
                .padding(vertical = 20.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (state.game) {
                    SentenceGameKind.RU_TO_CN -> {
                        Text(
                            text = sentence.translation,
                            fontSize = 20.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "соберите фразу по-китайски",
                            style = PixelType.caption,
                            color = TextMuted
                        )
                    }

                    SentenceGameKind.CN_TO_RU -> {
                        Text(
                            text = sentence.hanzi,
                            fontSize = 24.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = sentence.pinyin,
                            style = PixelType.chip,
                            color = LavenderGlow,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "соберите перевод",
                            style = PixelType.caption,
                            color = TextMuted
                        )
                    }

                    SentenceGameKind.AUDIO_TO_RU -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Brush.linearGradient(SentenceGradient))
                                .clickable(enabled = !state.revealed, onClick = onRepeat),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Headphones,
                                contentDescription = "Прослушать ещё раз",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (state.ttsAvailable) {
                                "слушайте и соберите перевод"
                            } else {
                                "нет голоса TTS — вот фраза: " + sentence.pinyin
                            },
                            style = PixelType.caption,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        PixelTag("ЕЩЁ РАЗ", color = SentenceAccent)
                    }

                    SentenceGameKind.RU_TO_AUDIO -> {
                        Text(
                            text = sentence.translation,
                            fontSize = 20.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "найдите нужную озвучку",
                            style = PixelType.caption,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        if (state.game == SentenceGameKind.RU_TO_AUDIO) {
            // Выбор озвучки: четыре кнопки со звуком
            state.options.forEachIndexed { index, option ->
                val isCorrectOption = index == state.correctOption
                val background = when {
                    !state.revealed -> Brush.linearGradient(CG.cardGradient)
                    isCorrectOption -> Brush.linearGradient(CG.successGradient)
                    index == state.chosen -> Brush.linearGradient(CG.dangerGradient)
                    else -> Brush.linearGradient(listOf(CG.glass(0.05f), CG.glass(0.03f)))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(background)
                        .clickable(enabled = !state.revealed) { onChooseAudio(index) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleIconButton(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Прослушать вариант",
                        tint = SentenceAccent,
                        modifier = Modifier.size(34.dp)
                    ) { onPlayOption(index) }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Вариант ${index + 1}",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.revealed && isCorrectOption) {
                        Text(
                            text = option.hanzi,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // Ответ: собранные плитки
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CG.track)
                    .border(1.dp, SentenceAccent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Ваш ответ",
                    style = PixelType.caption,
                    color = TextMuted
                )
                Spacer(Modifier.height(8.dp))
                if (state.picked.isEmpty()) {
                    Text(
                        text = "нажимайте на слова ниже",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                } else {
                    state.pickedWords.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { word ->
                                Tile(word = word, accent = SentenceAccent) {}
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            // Плитки со словами
            state.tiles.withIndex().toList().chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (tileIndex, word) ->
                        val used = state.picked.contains(tileIndex)
                        Tile(
                            word = word,
                            accent = if (used) TextMuted else LavenderGlow,
                            enabled = !used,
                            modifier = Modifier.weight(1f)
                        ) { onTapTile(tileIndex) }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (state.revealed) {
                Text(
                    text = "Правильно: " + state.target.joinToString(
                        separator = if (state.game == SentenceGameKind.RU_TO_CN) "" else " "
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MintAccent
                )
                Text(
                    text = sentence.hanzi + "  " + sentence.pinyin,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = sentence.translation,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Row {
                    GhostButton(text = "Прослушать", onClick = { speaker.speak(sentence.hanzi) })
                }
            } else {
                GradientButton(
                    text = "Проверить",
                    colors = StudyGradient,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.picked.isNotEmpty(),
                    onClick = onCheck
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GhostButton(
                        text = "Очистить",
                        modifier = Modifier.weight(1f),
                        onClick = onClear
                    )
                    GhostButton(
                        text = "Не знаю",
                        modifier = Modifier.weight(1f),
                        onClick = onSkip
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Tile(
    word: String,
    accent: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CG.glass(if (enabled) 0.10f else 0.04f))
            .border(1.dp, accent.copy(alpha = if (enabled) 0.5f else 0.2f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = word,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) TextPrimary else TextMuted,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun SentenceResult(
    state: SentenceGameState,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    val sounds = LocalSounds.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(CG.panel))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (state.passed) Icons.Filled.CheckCircle else Icons.Filled.Close,
                contentDescription = null,
                tint = if (state.passed) MintAccent else RoseAccent,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (state.passed) "Игра пройдена!" else "Были ошибки",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (state.passed) {
                    "Раздел засчитан — осталось пройти остальные игры"
                } else {
                    "Игра засчитывается только без ошибок — попробуйте ещё раз"
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HudChip(Icons.Filled.CheckCircle, "верно ${state.correct}", MintAccent)
                HudChip(Icons.Filled.Close, "ошибки ${state.mistakes}", RoseAccent)
                HudChip(Icons.Filled.EmojiEvents, "фраз ${state.total}", GoldAccent)
            }
            Spacer(Modifier.height(18.dp))
            GradientButton(
                text = "Ещё раз",
                colors = StudyGradient,
                modifier = Modifier.fillMaxWidth()
            ) {
                sounds.click()
                onRestart()
            }
            Spacer(Modifier.height(10.dp))
            GhostButton(
                text = "Готово",
                modifier = Modifier.fillMaxWidth(),
                onClick = onExit
            )
        }
    }
}
