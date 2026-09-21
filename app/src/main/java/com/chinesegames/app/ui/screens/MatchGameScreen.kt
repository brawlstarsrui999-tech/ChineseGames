package com.chinesegames.app.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chinesegames.app.ui.cardsLabel
import com.chinesegames.app.ui.components.CircleIconButton
import com.chinesegames.app.ui.components.ComboBurst
import com.chinesegames.app.ui.components.ConfirmDialog
import com.chinesegames.app.ui.components.ConfettiOverlay
import com.chinesegames.app.ui.components.EmptyState
import com.chinesegames.app.ui.components.FitGrid
import com.chinesegames.app.ui.components.GhostButton
import com.chinesegames.app.ui.components.GlassCard
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.GradientProgress
import com.chinesegames.app.ui.components.HudChip
import com.chinesegames.app.ui.components.MatchCardView
import com.chinesegames.app.ui.components.PixelDivider
import com.chinesegames.app.ui.components.PixelTag
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.StatRow
import com.chinesegames.app.ui.formatPercent
import com.chinesegames.app.ui.formatTime
import com.chinesegames.app.ui.game.GameMode
import com.chinesegames.app.ui.game.GameKind
import com.chinesegames.app.ui.game.MatchUiState
import com.chinesegames.app.ui.game.MatchViewModel
import com.chinesegames.app.ui.pairsLabel
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.OnAccentInk
import com.chinesegames.app.ui.theme.PixelType
import com.chinesegames.app.ui.theme.RoseAccent
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary

/**
 * Игровое поле «Найди пару» и «Мемори-сетки»: сетка карточек (всегда
 * помещается на экран), таймер, комбо, подсказки и экран победы с конфетти.
 */
@Composable
fun MatchGameScreen(
    deckIds: List<Long>,
    pairs: Int,
    mode: GameMode,
    previewSeconds: Int = 0,
    onExit: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: MatchViewModel = viewModel()
) {
    val sounds = LocalSounds.current
    val haptics = LocalHapticFeedback.current
    val state by viewModel.state.collectAsState()
    var showExitConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.matched.size) {
        if (state.matched.isNotEmpty()) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    LaunchedEffect(state.shakeKey) {
        if (state.shakeKey > 0) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    LaunchedEffect(deckIds, pairs, mode, previewSeconds) {
        viewModel.start(deckIds, pairs, mode, previewSeconds)
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

    PurpleBackground(petals = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            GameHeader(
                state = state,
                onBack = {
                    sounds.whoosh()
                    showExitConfirm = true
                },
                onHint = { viewModel.useHint() }
            )

            if (state.cards.isNotEmpty()) {
                FitGrid(
                    count = state.cards.size,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    maxColumns = if (state.cards.size <= 12) 4 else 6,
                    spacing = 6.dp
                ) { index, cellWidth, cellHeight ->
                    val card = state.cards[index]
                    MatchCardView(
                        card = card,
                        faceUp = state.hintActive ||
                            state.revealed.contains(card.id) ||
                            state.matched.contains(card.id),
                        matched = state.matched.contains(card.id),
                        selected = state.selected.contains(card.id),
                        wrong = state.wrongPair.contains(card.id),
                        shakeKey = if (state.wrongPair.contains(card.id)) state.shakeKey else 0,
                        modifier = Modifier.size(cellWidth, cellHeight),
                        onClick = { viewModel.onCardTap(card.id) }
                    )
                }
            } else {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Готовим карточки…",
                        style = PixelType.chip,
                        color = TextSecondary
                    )
                }
            }

            if (state.previewActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PixelTag(
                        text = "ЗАПОМНИТЕ ПАРЫ · ${state.previewLeft} С",
                        color = GoldAccent
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GhostButton(
                    text = "Заново",
                    icon = Icons.Filled.Refresh,
                    onClick = {
                        sounds.click()
                        viewModel.restart(false)
                    }
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = cardsLabel(state.cards.size),
                    style = PixelType.caption,
                    color = TextMuted
                )
            }
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            ComboBurst(
                text = state.comboMessage.orEmpty(),
                visible = state.comboMessage != null,
                modifier = Modifier.padding(top = 130.dp)
            )
        }

        if (state.notEnoughWords) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(CG.scrim),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    glyph = "空",
                    title = "Слишком мало слов",
                    message = "Для партии нужно минимум 3 слова в выбранных папках. " +
                        "Добавьте слова и попробуйте снова.",
                    action = {
                        GradientButton(text = "К настройкам партии") { onOpenSettings() }
                    }
                )
            }
        }

        if (state.paused && !state.finished && !state.notEnoughWords) {
            PauseOverlay(onResume = { viewModel.resume() })
        }

        if (state.finished) {
            WinOverlay(
                state = state,
                onRestartNewWords = { viewModel.restart(true) },
                onRestartSameWords = { viewModel.restart(false) },
                onSettings = onOpenSettings,
                onExit = onExit
            )
        }
    }

    if (showExitConfirm) {
        ConfirmDialog(
            title = "Выйти из партии?",
            message = "Результат этой партии не сохранится, а прогресс обнулится.",
            confirmText = "Выйти",
            onDismiss = { showExitConfirm = false },
            onConfirm = {
                viewModel.pause()
                onExit()
            }
        )
    }
}

/* ------------------------------- Шапка ------------------------------- */

@Composable
private fun GameHeader(
    state: MatchUiState,
    onBack: () -> Unit,
    onHint: () -> Unit
) {
    val sounds = LocalSounds.current
    val title = if (state.mode == GameMode.MEMORY) GameKind.MEMORY_GRID.title else GameKind.MATCH.title

    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                onClick = onBack
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.mode.title} · ${state.pairsFound} из ${state.pairsTotal}",
                    style = PixelType.caption,
                    color = TextMuted
                )
            }
            HudChip(Icons.Filled.Timer, formatTime(state.seconds), LavenderGlow)
            Spacer(Modifier.width(6.dp))
            HudChip(Icons.Filled.Close, "${state.mistakes}", RoseAccent)
        }

        Spacer(Modifier.height(8.dp))
        GradientProgress(progress = state.progress, height = 7.dp)
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.combo >= 2) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(CG.goldGradient))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = OnAccentInk,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "×${state.combo}",
                        style = PixelType.chip,
                        color = OnAccentInk
                    )
                }
            }
            Spacer(Modifier.weight(1f))

            if (state.mode != GameMode.MEMORY) {
                GhostButton(
                    text = "Подсказка · ${state.hintsLeft}",
                    icon = Icons.Filled.Lightbulb,
                    enabled = state.hintsLeft > 0 && !state.hintActive && !state.finished,
                    onClick = {
                        sounds.click()
                        onHint()
                    }
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = "${state.score} очк.",
                style = PixelType.hud,
                color = GoldAccent
            )
        }
    }
}

/* ------------------------------- Пауза ------------------------------- */

@Composable
private fun PauseOverlay(onResume: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CG.scrim),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "⏸", fontSize = 46.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Пауза",
                style = PixelType.title,
                color = TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Таймер остановлен — карточки вас подождут",
                style = PixelType.chip,
                color = TextSecondary
            )
            Spacer(Modifier.height(22.dp))
            GradientButton(text = "Продолжить") { onResume() }
        }
    }
}

/* ---------------------------- Экран победы ---------------------------- */

@Composable
private fun WinOverlay(
    state: MatchUiState,
    onRestartNewWords: () -> Unit,
    onRestartSameWords: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit
) {
    val sounds = LocalSounds.current
    var visibleStars by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.finished) {
        if (state.finished) {
            visibleStars = 0
            repeat(state.stars) { index ->
                kotlinx.coroutines.delay(430)
                visibleStars = index + 1
                sounds.star(index)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CG.scrimStrong),
        contentAlignment = Alignment.Center
    ) {
        ConfettiOverlay(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.verticalGradient(CG.panel))
                .border(1.dp, Brush.linearGradient(CG.cardBorder), RoundedCornerShape(30.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🏆", fontSize = 42.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Победа!",
                style = MaterialTheme.typography.displaySmall.copy(
                    brush = Brush.horizontalGradient(listOf(GoldAccent, Color.White, LavenderGlow))
                ),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${state.mode.title} · ${pairsLabel(state.pairsTotal)} за ${formatTime(state.seconds)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(3) { index ->
                    val filled = index < visibleStars
                    val size by animateDpAsState(
                        targetValue = if (filled) 46.dp else 38.dp,
                        animationSpec = spring(dampingRatio = 0.45f, stiffness = 600f),
                        label = "starSize"
                    )
                    Icon(
                        imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = if (filled) GoldAccent else TextMuted.copy(alpha = 0.45f),
                        modifier = Modifier
                            .size(size)
                            .padding(horizontal = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            GlassCard(contentPadding = PaddingValues(16.dp)) {
                StatRow("Очки", "${state.score}", valueColor = GoldAccent)
                StatRow("Найдено пар", "${state.pairsFound} из ${state.pairsTotal}")
                StatRow("Ошибок", "${state.mistakes}", valueColor = if (state.mistakes == 0) MintAccent else RoseAccent)
                StatRow("Точность", formatPercent(state.accuracy), valueColor = MintAccent)
                StatRow("Лучшее комбо", "×${state.bestCombo}", valueColor = LavenderGlow)
                StatRow("Время", formatTime(state.seconds))
            }

            Spacer(Modifier.height(16.dp))
            PixelDivider()
            Spacer(Modifier.height(16.dp))

            GradientButton(
                text = "Ещё раз · новые слова",
                icon = Icons.Filled.Refresh,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                sounds.click()
                onRestartNewWords()
            }
            Spacer(Modifier.height(9.dp))
            GhostButton(
                text = "Повторить эти же слова",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    sounds.click()
                    onRestartSameWords()
                }
            )
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                GhostButton(
                    text = "Настройки",
                    icon = Icons.Filled.Tune,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        sounds.click()
                        onSettings()
                    }
                )
                GhostButton(
                    text = "В меню",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        sounds.click()
                        onExit()
                    }
                )
            }
        }
    }
}
