package com.chinesegames.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chinesegames.app.data.Word
import com.chinesegames.app.ui.components.AnswerState
import com.chinesegames.app.ui.components.AnswerTile
import com.chinesegames.app.ui.components.ConfirmDialog
import com.chinesegames.app.ui.components.EmptyState
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.HudChip
import com.chinesegames.app.ui.components.HudCounter
import com.chinesegames.app.ui.components.PixelLanternRow
import com.chinesegames.app.ui.components.PixelSparkBurst
import com.chinesegames.app.ui.components.PixelTag
import com.chinesegames.app.ui.components.PromptPanel
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.QuizHeader
import com.chinesegames.app.ui.components.QuizResultOverlay
import com.chinesegames.app.ui.components.SkipButton
import com.chinesegames.app.ui.formatTime
import com.chinesegames.app.ui.game.AnswerKind
import com.chinesegames.app.ui.game.GameKind
import com.chinesegames.app.ui.game.PromptKind
import com.chinesegames.app.ui.game.QuizConfig
import com.chinesegames.app.ui.game.QuizPhase
import com.chinesegames.app.ui.game.QuizQuestion
import com.chinesegames.app.ui.game.QuizUiState
import com.chinesegames.app.ui.game.QuizViewModel
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.PixelType
import com.chinesegames.app.ui.theme.RoseAccent
import com.chinesegames.app.ui.theme.SkyAccent
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary
import com.chinesegames.app.ui.theme.VividPurple
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Один экран на все «вопросные» игры: пузыри, падающие слова, спринт,
 * аудио-квиз и memory-цепочку. Отличается только «сцена» и её анимация.
 */
@Composable
fun QuizGameScreen(
    config: QuizConfig,
    onExit: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: QuizViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var showExitConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(config.kind, config.deckIds, config.questions, config.options) {
        viewModel.start(config)
    }

    // Секунды партии (для отчёта в статистике)
    LaunchedEffect(state.finished) {
        while (!state.finished) {
            delay(1_000)
            viewModel.tickSeconds()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.pause()
                Lifecycle.Event.ON_RESUME -> viewModel.resume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PurpleBackground(petals = config.kind != GameKind.BUBBLE) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            QuizHeader(
                title = state.config.kind.title,
                subtitle = subtitle(state),
                progress = state.progress,
                onBack = { showExitConfirm = true }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (state.config.totalSeconds > 0) {
                        HudChip(
                            icon = Icons.Filled.Timer,
                            text = formatTime(state.totalLeft),
                            accent = if (state.totalLeft <= 10) RoseAccent else SkyAccent
                        )
                    } else if (state.config.secondsPerQuestion > 0) {
                        HudChip(
                            icon = Icons.Filled.Timer,
                            text = "${state.timeLeft}",
                            accent = if (state.timeLeft <= 2) RoseAccent else SkyAccent
                        )
                    }
                    HudChip(
                        icon = Icons.Filled.LocalFireDepartment,
                        text = "×${state.combo}",
                        accent = if (state.combo >= 2) GoldAccent else TextMuted
                    )
                }
            }

            when (state.phase) {
                QuizPhase.MEMORIZE -> MemorizeStage(state)
                QuizPhase.QUESTION -> QuestionStage(
                    state = state,
                    onAnswer = { viewModel.answer(it) },
                    onSkip = { viewModel.skip() },
                    onRepeatAudio = { viewModel.repeatAudio() }
                )

                else -> Box(Modifier.weight(1f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkipButton(text = "ПРОПУСТИТЬ", enabled = state.phase == QuizPhase.QUESTION) {
                    viewModel.skip()
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${state.score} очк.",
                    style = PixelType.hud,
                    color = GoldAccent
                )
            }
        }

        if (state.notEnoughWords) {
            Box(
                Modifier.fillMaxSize().background(CG.scrim),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    glyph = "空",
                    title = "Слишком мало слов",
                    message = "Для партии нужно минимум ${QuizViewModel.MIN_WORDS} слова в выбранных папках.",
                    action = {
                        GradientButton(text = "К настройкам партии") { onOpenSettings() }
                    }
                )
            }
        }

        if (state.finished) {
            QuizResultOverlay(
                gameTitle = state.config.kind.title,
                score = state.score,
                correct = state.correct,
                asked = state.correct + state.wrong,
                mistakes = state.wrong,
                accuracy = state.accuracy,
                bestCombo = state.bestCombo,
                seconds = state.seconds,
                stars = state.stars,
                finished = state.finished,
                onRestart = { viewModel.restart(true) },
                onRestartSameWords = { viewModel.restart(false) },
                onSettings = onOpenSettings,
                onExit = onExit
            )
        }
    }

    if (showExitConfirm) {
        ConfirmDialog(
            title = "Выйти из партии?",
            message = "Результат не сохранится, прогресс обнулится.",
            confirmText = "Выйти",
            onDismiss = { showExitConfirm = false },
            onConfirm = {
                viewModel.pause()
                onExit()
            }
        )
    }
}

private fun subtitle(state: QuizUiState): String {
    val kind = state.config.kind
    return when {
        state.phase == QuizPhase.MEMORIZE -> "Запоминайте слова: ${state.memorizeLeft} с"
        kind == GameKind.SPRINT -> "Вопрос ${state.index + 1} · серия ×${state.combo}"
        kind == GameKind.AUDIO_QUIZ -> "Слушайте и выбирайте иероглиф"
        state.config.secondsPerQuestion > 0 -> "Вопрос ${state.index + 1} · ${state.timeLeft} с"
        else -> "Вопрос ${state.index + 1} из ${state.questions.size}"
    }
}

/* --------------------------- Фаза запоминания --------------------------- */

@Composable
private fun ColumnScope.MemorizeStage(state: QuizUiState) {
    val progress = rememberProgress(state.index, state.config.memorizeSeconds)

    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PixelLanternRow()
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Запомните эти слова",
                style = PixelType.title,
                color = TextPrimary
            )
            Text(
                text = "Осталось ${state.memorizeLeft} с",
                style = PixelType.caption,
                color = TextMuted
            )
            Spacer(Modifier.height(6.dp))

            state.memorize.forEach { word ->
                MemorizeCard(word)
            }

            Spacer(Modifier.height(10.dp))
            HudChip(
                icon = Icons.Filled.Timer,
                text = "${state.memorizeLeft}",
                accent = GoldAccent
            )
        }
        // Полоска оставшегося времени
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(color = CG.glass(0.1f))
                drawRect(
                    brush = Brush.horizontalGradient(listOf(SkyAccent, LavenderGlow)),
                    size = androidx.compose.ui.geometry.Size(size.width * progress, size.height)
                )
            }
        }
    }
}

@Composable
private fun MemorizeCard(word: Word) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(CG.cardGradient))
            .border(1.dp, LavenderGlow.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = word.hanzi,
            fontSize = 30.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            if (word.pinyin.isNotBlank()) {
                Text(text = word.pinyin, style = PixelType.chip, color = LavenderGlow)
            }
            Text(
                text = word.translation,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/* ------------------------------ Вопрос ------------------------------ */

@Composable
private fun ColumnScope.QuestionStage(
    state: QuizUiState,
    onAnswer: (Int) -> Unit,
    onSkip: () -> Unit,
    onRepeatAudio: () -> Unit
) {
    val question = state.question ?: return
    when (state.config.kind) {
        GameKind.BUBBLE -> BubbleStage(state, question, onAnswer)
        GameKind.FALLING -> FallingStage(state, question, onAnswer)
        GameKind.AUDIO_QUIZ -> AudioStage(state, question, onAnswer, onRepeatAudio)
        GameKind.SPRINT -> SprintStage(state, question, onAnswer)
        else -> ChoiceStage(state, question, onAnswer, onSkip)
    }
}

/** Русская подпись варианта ответа. */
private fun optionText(question: QuizQuestion, index: Int, kind: AnswerKind): String {
    val word = question.options.getOrNull(index) ?: return ""
    return when (kind) {
        AnswerKind.TRANSLATION -> word.translation
        AnswerKind.HANZI -> word.hanzi
    }
}

private fun optionSub(question: QuizQuestion, index: Int, kind: AnswerKind): String? {
    val word = question.options.getOrNull(index) ?: return null
    return if (kind == AnswerKind.HANZI && word.pinyin.isNotBlank()) word.pinyin else null
}

private fun answerState(state: QuizUiState, index: Int): AnswerState = when {
    !state.revealed -> AnswerState.IDLE
    index == state.question?.correctIndex -> AnswerState.CORRECT
    index == state.chosen -> AnswerState.WRONG
    else -> AnswerState.DIMMED
}

/** Классические плитки ответов — спринт, memory-цепочка, запасной вариант. */
@Composable
private fun ColumnScope.ChoiceStage(
    state: QuizUiState,
    question: QuizQuestion,
    onAnswer: (Int) -> Unit,
    onSkip: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state.sparkKey) {
        if (state.sparkKey > 0) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        PromptPanel(accent = VividPurple) {
            PromptContent(state, question, onRepeatAudio = {})
        }
        Spacer(Modifier.height(6.dp))
        question.options.forEachIndexed { index, _ ->
            AnswerTile(
                text = optionText(question, index, state.config.answer),
                sub = optionSub(question, index, state.config.answer),
                state = answerState(state, index),
                modifier = Modifier.fillMaxWidth(),
                big = false,
                onClick = { onAnswer(index) }
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.message.orEmpty(),
                style = PixelType.chip,
                color = if (state.message == "Верно!") MintAccent else RoseAccent
            )
            Spacer(Modifier.weight(1f))
            PixelSparkBurst(
                burstKey = state.sparkKey,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

/** Что показываем в вопросе: иероглиф, перевод или кнопку прослушивания. */
@Composable
private fun PromptContent(
    state: QuizUiState,
    question: QuizQuestion,
    onRepeatAudio: () -> Unit
) {
    val word = question.word
    when (state.config.prompt) {
        PromptKind.HANZI -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = word.hanzi,
                fontSize = 44.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            if (word.pinyin.isNotBlank()) {
                Text(text = word.pinyin, style = PixelType.chip, color = LavenderGlow)
            }
        }

        PromptKind.TRANSLATION -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = word.translation,
                fontSize = 26.sp,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "выберите иероглиф",
                style = PixelType.caption,
                color = TextMuted
            )
        }

        PromptKind.AUDIO -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(CG.audioGradient))
                    .clickable { onRepeatAudio() }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Повторить",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "ЕЩЁ РАЗ",
                    style = PixelType.title,
                    color = Color.White
                )
            }
            if (!state.ttsAvailable) {
                Spacer(Modifier.height(8.dp))
                PixelTag(text = "НЕТ ГОЛОСА TTS — ВОТ ПИНЬИНЬ", color = RoseAccent)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = word.pinyin.ifBlank { "—" },
                    style = PixelType.hud,
                    color = LavenderGlow
                )
            } else {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "слушайте произношение",
                    style = PixelType.caption,
                    color = TextMuted
                )
            }
        }
    }
}

/* ----------------------------- Bubble pop ----------------------------- */

@Composable
private fun ColumnScope.BubbleStage(
    state: QuizUiState,
    question: QuizQuestion,
    onAnswer: (Int) -> Unit
) {
    val progress = rememberProgress(state.index, state.config.secondsPerQuestion)

    Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
        PromptPanel(
            modifier = Modifier.padding(horizontal = 16.dp),
            accent = SkyAccent
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = question.word.hanzi,
                    fontSize = 40.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                if (question.word.pinyin.isNotBlank()) {
                    Text(text = question.word.pinyin, style = PixelType.chip, color = LavenderGlow)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "ЛОПНИ ПУЗЫРЬ С ВЕРНЫМ ПЕРЕВОДОМ",
                    style = PixelType.caption,
                    color = TextMuted
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            val areaWidth = maxWidth
            val areaHeight = maxHeight
            val bubbleWidth = minOf(areaWidth * 0.46f, 165.dp)
            val count = question.options.size
            question.options.forEachIndexed { index, _ ->
                val spread = if (count <= 1) 0.5f else index.toFloat() / (count - 1)
                val lane = 0.04f + spread * 0.62f
                val startY = 1.02f + (index % 3) * 0.09f
                val travel = startY + 0.12f
                val y = startY - progress * travel
                val wobble = sin(progress * 7f + index * 1.7f) * 0.02f
                Bubble(
                    text = optionText(question, index, state.config.answer),
                    state = answerState(state, index),
                    modifier = Modifier
                        .offset(x = areaWidth * (lane + wobble), y = areaHeight * y)
                        .width(bubbleWidth),
                    onClick = { onAnswer(index) }
                )
            }
        }
    }
}

@Composable
private fun Bubble(
    text: String,
    state: AnswerState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val pulse by animateFloatAsState(
        targetValue = if (state == AnswerState.IDLE) 1f else 1.04f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "bubblePulse"
    )
    val colors = when (state) {
        AnswerState.CORRECT -> listOf(MintAccent.copy(alpha = 0.9f), MintAccent.copy(alpha = 0.55f))
        AnswerState.WRONG -> listOf(RoseAccent.copy(alpha = 0.9f), RoseAccent.copy(alpha = 0.55f))
        AnswerState.DIMMED -> listOf(CG.glass(0.08f), CG.glass(0.04f))
        AnswerState.IDLE -> listOf(SkyAccent.copy(alpha = 0.55f), VividPurple.copy(alpha = 0.45f))
    }
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
            .clip(CircleShape)
            .background(Brush.radialGradient(colors))
            .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                fontSize = if (text.length <= 14) 15.sp else 12.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/* --------------------------- Падающие слова --------------------------- */

@Composable
private fun ColumnScope.FallingStage(
    state: QuizUiState,
    question: QuizQuestion,
    onAnswer: (Int) -> Unit
) {
    val progress = rememberProgress(state.index, state.config.secondsPerQuestion)

    Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            val fallDistance = (maxHeight - 120.dp).coerceAtLeast(40.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = fallDistance * progress)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(CG.cardFace))
                    .border(1.5.dp, LavenderGlow.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = question.word.hanzi,
                    fontSize = 40.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                if (question.word.pinyin.isNotBlank()) {
                    Text(text = question.word.pinyin, style = PixelType.chip, color = LavenderGlow)
                }
            }

            PixelLanternRow(
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            question.options.forEachIndexed { index, _ ->
                AnswerTile(
                    text = optionText(question, index, state.config.answer),
                    sub = optionSub(question, index, state.config.answer),
                    state = answerState(state, index),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onAnswer(index) }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun ColumnScope.SprintStage(
    state: QuizUiState,
    question: QuizQuestion,
    onAnswer: (Int) -> Unit
) {
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HudCounter(
                label = "ВЕРНО",
                value = "${state.correct}",
                accent = MintAccent,
                modifier = Modifier.weight(1f)
            )
            HudCounter(
                label = "ОШИБКИ",
                value = "${state.wrong}",
                accent = RoseAccent,
                modifier = Modifier.weight(1f)
            )
            HudCounter(
                label = "СЕРИЯ",
                value = "×${state.combo}",
                accent = GoldAccent,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(2.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PromptPanel(accent = VividPurple) {
                Text(
                    text = question.word.hanzi,
                    fontSize = 52.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            PixelSparkBurst(
                burstKey = state.sparkKey,
                modifier = Modifier.size(120.dp)
            )
        }

        question.options.forEachIndexed { index, _ ->
            AnswerTile(
                text = optionText(question, index, state.config.answer),
                sub = optionSub(question, index, state.config.answer),
                state = answerState(state, index),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                onClick = { onAnswer(index) }
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

/* ----------------------------- Аудио-квиз ----------------------------- */

@Composable
private fun ColumnScope.AudioStage(
    state: QuizUiState,
    question: QuizQuestion,
    onAnswer: (Int) -> Unit,
    onRepeatAudio: () -> Unit
) {
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PromptPanel(accent = SkyAccent) {
            PromptContent(state, question, onRepeatAudio = onRepeatAudio)
        }
        question.options.forEachIndexed { index, _ ->
            AnswerTile(
                text = optionText(question, index, state.config.answer),
                sub = optionSub(question, index, state.config.answer),
                state = answerState(state, index),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                big = true,
                onClick = { onAnswer(index) }
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

/* ------------------------------ Мелочи ------------------------------ */

/** Прогресс текущего вопроса 0..1 — плавно, кадр за кадром. */
@Composable
private fun rememberProgress(key: Any?, seconds: Int): Float {
    val duration = (seconds.coerceAtLeast(1) * 1000).toFloat()
    val animated = remember(key) { Animatable(0f) }
    LaunchedEffect(key, seconds) {
        animated.snapTo(0f)
        animated.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = duration.toInt(), easing = LinearEasing)
        )
    }
    return animated.value
}
