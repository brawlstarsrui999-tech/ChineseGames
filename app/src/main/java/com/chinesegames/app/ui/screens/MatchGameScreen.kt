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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.chinesegames.app.ui.components.CardsLegend
import com.chinesegames.app.ui.components.CircleIconButton
import com.chinesegames.app.ui.components.ComboBurst
import com.chinesegames.app.ui.components.ConfirmDialog
import com.chinesegames.app.ui.components.ConfettiOverlay
import com.chinesegames.app.ui.components.EmptyState
import com.chinesegames.app.ui.components.GhostButton
import com.chinesegames.app.ui.components.GlassCard
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.GradientProgress
import com.chinesegames.app.ui.components.MatchCardView
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.StatRow
import com.chinesegames.app.ui.formatPercent
import com.chinesegames.app.ui.formatTime
import com.chinesegames.app.ui.game.GameMode
import com.chinesegames.app.ui.game.MatchUiState
import com.chinesegames.app.ui.game.MatchViewModel
import com.chinesegames.app.ui.pairsLabel
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.RoseAccent
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary
import com.chinesegames.app.ui.theme.VividPurple
import kotlinx.coroutines.delay

/**
 * Игровое поле «Найди пару»: сетка карточек, таймер, комбо, подсказки
 * и экран победы с конфетти.
 */
@Composable
fun MatchGameScreen(
    deckIds: List<Long>,
    pairs: Int,
    mode: GameMode,
    onExit: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: MatchViewModel = viewModel()
) {
    val sounds = LocalSounds.current
    val haptics = LocalHapticFeedback.current
    val state by viewModel.state.collectAsState()
    var showExitConfirm by remember { mutableStateOf(false) }

    // Вибрация: нашлась пара — сильнее, ошибка — мягкий отклик
    LaunchedEffect(state.matched.size) {
        if (state.matched.isNotEmpty()) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    LaunchedEffect(state.shakeKey) {
        if (state.shakeKey > 0) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    LaunchedEffect(deckIds, pairs, mode) {
        viewModel.start(deckIds, pairs, mode)
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
            GameHeader(
                state = state,
                onBack = {
                    sounds.whoosh()
                    showExitConfirm = true
                },
                onHint = {
                    viewModel.useHint()
                }
            )

            val columns = when {
                state.pairsTotal <= 6 -> 2
                state.pairsTotal <= 12 -> 3
                else -> 4
            }
            val compact = columns >= 4

            if (state.cards.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(items = state.cards, key = { it.id }) { card ->
                        MatchCardView(
                            card = card,
                            faceUp = state.hintActive ||
                                state.revealed.contains(card.id) ||
                                state.matched.contains(card.id),
                            matched = state.matched.contains(card.id),
                            selected = state.selected.contains(card.id),
                            wrong = state.wrongPair.contains(card.id),
                            shakeKey = if (state.wrongPair.contains(card.id)) state.shakeKey else 0,
                            compact = compact,
                            onClick = { viewModel.onCardTap(card.id) }
                        )
                    }
                }
            } else {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Готовим карточки…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
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
                if (state.cards.isNotEmpty()) {
                    Text(
                        text = cardsLabel(state.cards.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }

        // Всплывающее «Комбо ×3  +150»
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
                    .background(Color(0xE60B0618)),
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

    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                onClick = onBack
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Найди пару",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.mode.title} · ${state.pairsFound} из ${state.pairsTotal}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            HeaderChip(Icons.Filled.Timer, formatTime(state.seconds), LavenderGlow)
            Spacer(Modifier.width(6.dp))
            HeaderChip(Icons.Filled.Close, "${state.mistakes}", RoseAccent)
        }

        Spacer(Modifier.height(10.dp))
        GradientProgress(progress = state.progress, height = 8.dp)
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.combo >= 2) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(CG.goldGradient))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFF4A2500),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Комбо ×${state.combo}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF4A2500),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.weight(1f))

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
            Text(
                text = "${state.score} очк.",
                style = MaterialTheme.typography.labelMedium,
                color = GoldAccent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HeaderChip(icon: ImageVector, text: String, accent: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/* ------------------------------- Пауза ------------------------------- */

@Composable
private fun PauseOverlay(onResume: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60B0618)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "⏸", fontSize = 46.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Пауза",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Таймер остановлен — карточки вас подождут",
                style = MaterialTheme.typography.bodySmall,
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
                delay(430)
                visibleStars = index + 1
                sounds.star(index)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE0B0618)),
        contentAlignment = Alignment.Center
    ) {
        ConfettiOverlay(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF2C1758), Color(0xFF140A2B))))
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

            Spacer(Modifier.height(16.dp))

            GlassCard(contentPadding = PaddingValues(16.dp)) {
                StatRow("Очки", "${state.score}", valueColor = GoldAccent)
                StatRow("Найдено пар", "${state.pairsFound} из ${state.pairsTotal}")
                StatRow("Ошибок", "${state.mistakes}", valueColor = if (state.mistakes == 0) MintAccent else RoseAccent)
                StatRow("Точность", formatPercent(state.accuracy), valueColor = MintAccent)
                StatRow("Лучшее комбо", "×${state.bestCombo}", valueColor = LavenderGlow)
                StatRow("Время", formatTime(state.seconds))
            }

            Spacer(Modifier.height(18.dp))

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
