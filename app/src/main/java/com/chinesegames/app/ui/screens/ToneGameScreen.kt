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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
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
import com.chinesegames.app.ui.game.PinyinTones
import com.chinesegames.app.ui.game.ToneUiState
import com.chinesegames.app.ui.game.ToneViewModel
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

/**
 * «Тренажёр тонов»: иероглиф + озвучка, пиньинь скрыт. Игрок нажимает тон
 * каждого слога по порядку; когда тонов столько же, сколько слогов, — проверка.
 */
@Composable
fun ToneGameScreen(
    deckIds: List<Long>,
    questions: Int,
    secondsPerQuestion: Int,
    srsFirst: Boolean,
    onExit: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ToneViewModel = viewModel()
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
                title = GameKind.TONES.title,
                subtitle = "Слово ${state.index + 1} из ${state.tasks.size} · тоны по порядку слогов",
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

            val task = state.task
            if (task == null) {
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
                                    text = task.word.hanzi,
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
                                        text = "нажмите, чтобы послушать ещё раз",
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

                    // Слоты по числу слогов: заполняются нажатыми тонами
                    ToneSlots(state = state, onBackspace = { viewModel.backspace() })

                    if (state.checked) {
                        ToneFeedback(state)
                    } else {
                        ToneKeypad(onTone = { viewModel.tapTone(it) })
                    }

                    Spacer(Modifier.weight(1f))

                    if (!state.checked) {
                        GhostButton(
                            text = "Не знаю, покажи тоны",
                            icon = Icons.Filled.Close,
                            modifier = Modifier.fillMaxWidth()
                        ) { viewModel.reveal() }
                    } else {
                        GradientButton(
                            text = if (state.index + 1 >= state.tasks.size) "Показать итоги" else "Далее",
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
                    title = "Слишком мало слов с тонами",
                    message = "Для партии нужно минимум ${ToneViewModel.MIN_WORDS} слова, у которых " +
                        "в пиньине проставлены тоны (nǐ hǎo, а не nihao).",
                    action = { GradientButton(text = "К настройкам партии") { onOpenSettings() } }
                )
            }
        }

        if (state.finished) {
            QuizResultOverlay(
                gameTitle = GameKind.TONES.title,
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

/* ------------------------------ Слоты тонов ------------------------------ */

@Composable
private fun ToneSlots(state: ToneUiState, onBackspace: () -> Unit) {
    val task = state.task ?: return
    val total = task.syllables.size
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (total == 1) "1 слог — один тон" else "$total ${slogLabel(total)} — тоны по порядку",
                style = PixelType.caption,
                color = TextMuted
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(total) { position ->
                    val entered = state.entered.getOrNull(position)
                    val expected = task.syllables[position].tone
                    val accent = when {
                        !state.checked -> if (entered != null) VividPurple else CG.glass(0.1f)
                        entered == expected -> MintAccent
                        else -> RoseAccent
                    }
                    val shape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .size(width = 54.dp, height = 46.dp)
                            .clip(shape)
                            .background(if (entered != null || state.checked) accent.copy(alpha = 0.35f) else CG.glass(0.05f))
                            .border(
                                1.dp,
                                if (entered != null || state.checked) accent else LavenderGlow.copy(alpha = 0.3f),
                                shape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                entered == null && !state.checked -> "·"
                                entered == null -> "—"
                                entered == 0 -> "·"
                                else -> "$entered"
                            },
                            style = PixelType.hud,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
        if (!state.checked && state.entered.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CG.glass(0.08f))
                    .clickable(onClick = onBackspace),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Стереть последний тон",
                    tint = LavenderGlow,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/* ------------------------------- Кнопки тонов ------------------------------- */

@Composable
private fun ToneKeypad(onTone: (Int) -> Unit) {
    val accents = listOf(SkyAccent, MintAccent, GoldAccent, RoseAccent)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..4).forEach { tone ->
                ToneButton(
                    top = PinyinTones.toneSample(tone),
                    bottom = "$tone-й тон",
                    accent = accents[tone - 1],
                    modifier = Modifier.weight(1f),
                    onClick = { onTone(tone) }
                )
            }
        }
        ToneButton(
            top = "a  ·",
            bottom = "лёгкий тон",
            accent = LavenderGlow,
            modifier = Modifier.fillMaxWidth(),
            compact = true,
            onClick = { onTone(0) }
        )
    }
}

@Composable
private fun ToneButton(
    top: String,
    bottom: String,
    accent: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(accent.copy(alpha = 0.32f), CG.glass(0.05f)))
            )
            .border(1.dp, accent.copy(alpha = 0.55f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = if (compact) 10.dp else 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = top,
            fontSize = if (compact) 22.sp else 30.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(text = bottom, style = PixelType.caption, color = TextSecondary)
    }
}

/* ------------------------------ Обратная связь ------------------------------ */

@Composable
private fun ToneFeedback(state: ToneUiState) {
    val task = state.task ?: return
    val ok = state.isCorrect
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(if (ok) CG.successGradient else CG.dangerGradient))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (ok) "Верно!" else "Правильные тоны:",
                style = PixelType.title,
                color = Color.White
            )
            Spacer(Modifier.weight(1f))
            if (!ok) PixelTag(text = "−20 ОЧК.", color = Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = task.syllables.joinToString(" ") { it.text },
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = task.syllables.joinToString("–") { if (it.tone == 0) "·" else "${it.tone}" },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        Text(
            text = task.word.translation,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

private fun slogLabel(count: Int): String = when {
    count % 10 == 1 && count % 100 != 11 -> "слог"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "слога"
    else -> "слогов"
}
