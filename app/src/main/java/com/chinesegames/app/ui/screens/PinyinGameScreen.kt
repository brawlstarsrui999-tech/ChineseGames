package com.chinesegames.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chinesegames.app.ui.components.ConfirmDialog
import com.chinesegames.app.ui.components.EmptyState
import com.chinesegames.app.ui.components.GhostButton
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.HudChip
import com.chinesegames.app.ui.components.PixelSparkBurst
import com.chinesegames.app.ui.components.PixelTag
import com.chinesegames.app.ui.components.PromptPanel
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.QuizHeader
import com.chinesegames.app.ui.components.QuizResultOverlay
import com.chinesegames.app.ui.game.GameKind
import com.chinesegames.app.ui.game.PinyinUiState
import com.chinesegames.app.ui.game.PinyinViewModel
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.PixelType
import com.chinesegames.app.ui.theme.RoseAccent
import com.chinesegames.app.ui.theme.SkyAccent
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary
import com.chinesegames.app.ui.theme.VividPurple

/** Тоны для кнопок-подсказок: печатать их на телефоне неудобно. */
private const val TONE_CHARS = "āáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜ"

/**
 * Игра «Ввод пиньиня»: показывается иероглиф, нужно напечатать его пиньинь.
 * Тоны необязательны — сравнение идёт по «скелету» слога.
 */
@Composable
fun PinyinGameScreen(
    deckIds: List<Long>,
    questions: Int,
    secondsPerQuestion: Int,
    srsFirst: Boolean,
    onExit: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: PinyinViewModel = viewModel()
) {
    val sounds = LocalSounds.current
    val state by viewModel.state.collectAsState()
    var showExitConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(deckIds, questions, secondsPerQuestion, srsFirst) {
        viewModel.start(deckIds, questions, secondsPerQuestion, srsFirst)
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

    PurpleBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            QuizHeader(
                title = GameKind.PINYIN.title,
                subtitle = "Слово ${state.index + 1} из ${state.words.size} · тоны можно не писать",
                progress = state.progress,
                onBack = { showExitConfirm = true }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (state.secondsPerQuestion > 0) {
                        HudChip(
                            icon = Icons.Filled.Timer,
                            text = "${state.timeLeft}",
                            accent = if (state.timeLeft <= 3) RoseAccent else SkyAccent
                        )
                    }
                    HudChip(
                        icon = Icons.Filled.LocalFireDepartment,
                        text = "×${state.combo}",
                        accent = if (state.combo >= 2) GoldAccent else TextMuted
                    )
                }
            }

            val word = state.word
            if (word == null) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Готовим слова…", style = PixelType.chip, color = TextSecondary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(Modifier.height(4.dp))

                    Box(contentAlignment = Alignment.Center) {
                        PromptPanel(
                            accent = VividPurple,
                            onClick = { viewModel.speakCurrent() }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = word.hanzi,
                                    fontSize = 62.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Озвучить",
                                        tint = LavenderGlow,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "нажмите, чтобы услышать",
                                        style = PixelType.caption,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                        PixelSparkBurst(
                            burstKey = if (state.isCorrect) state.correct else 0,
                            modifier = Modifier.size(150.dp)
                        )
                    }

                    PinyinInputField(
                        value = state.input,
                        enabled = !state.checked,
                        correct = state.checked && state.isCorrect,
                        wrong = state.checked && !state.isCorrect,
                        onValueChange = { viewModel.updateInput(it) },
                        onSubmit = { viewModel.submit() }
                    )

                    if (state.checked) {
                        AnswerFeedback(state)
                    } else {
                        ToneKeyboard(onInsert = { char ->
                            sounds.tick()
                            viewModel.updateInput(state.input + char)
                        })
                    }

                    Spacer(Modifier.weight(1f))

                    if (!state.checked) {
                        GradientButton(
                            text = "Проверить",
                            icon = Icons.Filled.Check,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.input.isNotBlank(),
                            contentPadding = PaddingValues(vertical = 15.dp)
                        ) { viewModel.submit() }
                        GhostButton(
                            text = "Не знаю, покажи ответ",
                            icon = Icons.Filled.Close,
                            modifier = Modifier.fillMaxWidth()
                        ) { viewModel.reveal() }
                    } else {
                        GradientButton(
                            text = if (state.index + 1 >= state.words.size) "Показать итоги" else "Далее",
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 15.dp)
                        ) { viewModel.next() }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "верно ${state.correct} · ошибок ${state.wrong}",
                    style = PixelType.caption,
                    color = TextMuted
                )
                Spacer(Modifier.weight(1f))
                Text(text = "${state.score} очк.", style = PixelType.hud, color = GoldAccent)
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
                    message = "Для партии нужно минимум ${PinyinViewModel.MIN_WORDS} слова в выбранных папках.",
                    action = { GradientButton(text = "К настройкам партии") { onOpenSettings() } }
                )
            }
        }

        if (state.finished) {
            QuizResultOverlay(
                gameTitle = GameKind.PINYIN.title,
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

/* ------------------------------ Поле ввода ------------------------------ */

@Composable
private fun PinyinInputField(
    value: String,
    enabled: Boolean,
    correct: Boolean,
    wrong: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val borderColor = when {
        correct -> MintAccent
        wrong -> RoseAccent
        else -> LavenderGlow.copy(alpha = 0.45f)
    }
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CG.glass(0.06f))
            .border(if (correct || wrong) 2.dp else 1.dp, borderColor, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = "пиньинь, например nihao или nǐ hǎo",
                    style = PixelType.chip,
                    color = TextMuted
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = PixelType.hud.copy(color = TextPrimary),
                cursorBrush = SolidColor(VividPurple),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }
        if (correct) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MintAccent,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    LaunchedEffect(enabled) {
        if (enabled) runCatching { focusRequester.requestFocus() }
    }
}

/* --------------------------- Обратная связь --------------------------- */

@Composable
private fun AnswerFeedback(state: PinyinUiState) {
    val word = state.word ?: return
    val ok = state.isCorrect
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    if (ok) CG.successGradient else CG.dangerGradient
                )
            )
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (ok) "Верно!" else "Правильный пиньинь:",
                style = PixelType.title,
                color = Color.White
            )
            Spacer(Modifier.weight(1f))
            if (!ok) {
                PixelTag(text = "−20 ОЧК.", color = Color.White)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = word.pinyin.ifBlank { "—" },
            fontSize = 24.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = word.translation,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

/** Быстрые кнопки с тонами: удобно вставлять «ǎ» и «ǜ». */
@Composable
private fun ToneKeyboard(onInsert: (String) -> Unit) {
    Column {
        Text(
            text = "Быстрые тоны (необязательно)",
            style = PixelType.caption,
            color = TextMuted
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TONE_CHARS.forEach { char ->
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CG.glass(0.07f))
                        .border(1.dp, LavenderGlow.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .clickable { onInsert(char.toString()) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = char.toString(), style = PixelType.chip, color = TextPrimary)
                }
            }
        }
    }
}
