package com.chinesegames.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.chinesegames.app.ui.components.AnswerState
import com.chinesegames.app.ui.components.AnswerTile
import com.chinesegames.app.ui.components.CircleIconButton
import com.chinesegames.app.ui.components.GhostButton
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.GradientProgress
import com.chinesegames.app.ui.components.HudChip
import com.chinesegames.app.ui.components.PixelTag
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.StudyAccent
import com.chinesegames.app.ui.components.StudyGradient
import com.chinesegames.app.ui.study.HskGameKind
import com.chinesegames.app.ui.study.HskGameState
import com.chinesegames.app.ui.study.HskGameViewModel
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.PixelType
import com.chinesegames.app.ui.theme.RoseAccent
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Экран одной игры курса (или экзамена по уровню): вопрос, варианты,
 * проверка и итог. Игра засчитывается только без ошибок.
 */
@Composable
fun HskGameScreen(
    level: Int,
    topicId: String,
    groupIndex: Int,
    gameId: String,
    isExam: Boolean,
    onExit: () -> Unit,
    viewModel: HskGameViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(level, topicId, groupIndex, gameId, isExam) {
        if (isExam) {
            viewModel.startExam(level)
        } else {
            viewModel.startGroup(level, topicId, groupIndex, gameId)
        }
    }

    PurpleBackground(petals = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            GameHeader(
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
                        text = "Готовим слова…",
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
                            text = "Не хватает слов",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "В группе должно быть хотя бы 3 слова",
                            style = PixelType.caption,
                            color = TextMuted
                        )
                        Spacer(Modifier.height(14.dp))
                        GhostButton(text = "Назад", onClick = onExit)
                    }
                }

                state.finished -> ResultPanel(
                    state = state,
                    onRestart = {
                        if (isExam) viewModel.startExam(level)
                        else viewModel.startGroup(level, topicId, groupIndex, gameId)
                    },
                    onExit = onExit
                )

                else -> QuestionPanel(
                    state = state,
                    onAnswer = { viewModel.answer(it) },
                    onInput = { viewModel.updateInput(it) },
                    onSubmit = { viewModel.submitTyped() },
                    onRepeat = { viewModel.repeatAudio() },
                    onSkip = { viewModel.skip() }
                )
            }
        }
    }
}

@Composable
private fun GameHeader(
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
                tint = StudyAccent
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
private fun QuestionPanel(
    state: HskGameState,
    onAnswer: (Int) -> Unit,
    onInput: (String) -> Unit,
    onSubmit: () -> Unit,
    onRepeat: () -> Unit,
    onSkip: () -> Unit
) {
    val question = state.question ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // Вопрос
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(CG.cardGradient))
                .padding(vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.game == HskGameKind.AUDIO_TO_HANZI) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(StudyGradient))
                            .clickable(enabled = !state.revealed, onClick = onRepeat),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Headphones,
                            contentDescription = "Прослушать ещё раз",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (state.ttsAvailable) {
                            "Слушайте и выбирайте иероглиф"
                        } else {
                            "Голос TTS не найден — вот пиньинь: ${question.word.pinyin}"
                        },
                        style = PixelType.caption,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    PixelTag("ЕЩЁ РАЗ", color = StudyAccent)
                } else {
                    Text(
                        text = question.prompt,
                        fontSize = if (question.prompt.length > 8) 24.sp else 40.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    if (!question.promptSub.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = question.promptSub,
                            style = PixelType.chip,
                            color = LavenderGlow
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.game.promptLabel + " → ответ",
                        style = PixelType.caption,
                        color = TextMuted
                    )
                }
            }
        }

        // Ответ
        if (question.typing) {
            OutlinedTextField(
                value = state.input,
                onValueChange = onInput,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.revealed,
                placeholder = { Text("пиньинь без тонов, например: nihao") },
                label = { Text("Пиньинь") }
            )
            if (state.revealed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Верно: " + question.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.lastCorrect) MintAccent else RoseAccent,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = question.word.hanzi + " · " + question.word.translation,
                        style = PixelType.caption,
                        color = TextSecondary
                    )
                }
            } else {
                GradientButton(
                    text = "Проверить",
                    colors = StudyGradient,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.input.isNotBlank(),
                    onClick = onSubmit
                )
            }
        } else {
            question.options.forEachIndexed { index, option ->
                AnswerTile(
                    text = option,
                    sub = null,
                    state = when {
                        !state.revealed -> AnswerState.IDLE
                        index == question.correctIndex -> AnswerState.CORRECT
                        index == state.chosen -> AnswerState.WRONG
                        else -> AnswerState.DIMMED
                    },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onAnswer(index) }
                )
            }
            if (state.revealed) {
                Text(
                    text = question.word.hanzi + " · " + question.word.pinyin + " · " +
                        question.word.translation,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (!state.revealed) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                GhostButton(text = "Не знаю", onClick = onSkip)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ResultPanel(
    state: HskGameState,
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
                text = when {
                    state.isExam && state.passed -> "Экзамен сдан!"
                    state.isExam -> "Пока не сдано"
                    state.passed -> "Игра пройдена!"
                    else -> "Были ошибки"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    state.isExam && state.passed ->
                        "Открыт следующий уровень HSK"
                    state.isExam ->
                        "Нужно 80% верных ответов — попробуйте ещё раз"
                    state.passed ->
                        "Без ошибок: игра засчитана в прогресс группы"
                    else ->
                        "Игра засчитывается только без ошибок — повторите"
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HudChip(Icons.Filled.CheckCircle, "верно ${state.correct}", MintAccent)
                HudChip(Icons.Filled.Close, "ошибки ${state.mistakes}", RoseAccent)
                HudChip(Icons.Filled.EmojiEvents, "очки ${state.score}", GoldAccent)
            }

            if (state.wordsSaved > 0) {
                Spacer(Modifier.height(12.dp))
                PixelTag("СЛОВА ДОБАВЛЕНЫ В «ВЫУЧЕННОЕ»", color = MintAccent)
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
