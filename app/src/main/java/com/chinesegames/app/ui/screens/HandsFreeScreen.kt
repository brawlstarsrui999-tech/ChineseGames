package com.chinesegames.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chinesegames.app.data.Word
import com.chinesegames.app.ui.WordDisplay
import com.chinesegames.app.ui.components.CgTopBar
import com.chinesegames.app.ui.components.EmptyState
import com.chinesegames.app.ui.components.GhostButton
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.GradientProgress
import com.chinesegames.app.ui.components.PixelTag
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.SelectChip
import com.chinesegames.app.ui.game.GameKind
import com.chinesegames.app.ui.game.HandsFreeViewModel
import com.chinesegames.app.ui.rememberAppSettings
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.PixelType
import com.chinesegames.app.ui.theme.SkyAccent
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary
import com.chinesegames.app.ui.theme.VividPurple

/** Интервалы, которые можно выбрать прямо на экране. */
private val INTERVALS = listOf(4, 6, 8, 10, 15)

/**
 * Режим «Без рук»: карточка сама переворачивается каждые N секунд,
 * слово озвучивается по-китайски, перевод — по-русски. Идёт по кругу,
 * экран не гаснет. Никаких очков — это спокойное повторение.
 */
@Composable
fun HandsFreeScreen(
    deckIds: List<Long>,
    intervalSeconds: Int,
    srsFirst: Boolean,
    onExit: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HandsFreeViewModel = viewModel()
) {
    val sounds = LocalSounds.current
    val state by viewModel.state.collectAsState()
    val appSettings = rememberAppSettings()

    LaunchedEffect(deckIds, intervalSeconds, srsFirst) {
        viewModel.start(deckIds, intervalSeconds, srsFirst)
    }

    // экран не гаснет, пока режим открыт
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PurpleBackground(petals = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            CgTopBar(
                title = GameKind.HANDS_FREE.title,
                subtitle = when {
                    state.loading -> "Готовим слова…"
                    state.playing -> "Каждые ${state.intervalSeconds} с · слов в круге: ${state.words.size}"
                    else -> "Пауза · слов в круге: ${state.words.size}"
                },
                emoji = GameKind.HANDS_FREE.emoji,
                onBack = {
                    sounds.whoosh()
                    viewModel.pause()
                    onExit()
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))

                val word = state.word
                if (word != null) {
                    FlipCard(
                        word = word,
                        flipped = state.flipped,
                        showPinyin = WordDisplay.showsPinyin(appSettings.displayMode),
                        onClick = { viewModel.repeatSpeech() }
                    )
                    Spacer(Modifier.height(10.dp))
                    GradientProgress(
                        progress = state.phaseProgress,
                        height = 6.dp,
                        colors = if (state.flipped) listOf(MintAccent, SkyAccent) else CG.primaryGradient
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelTag(
                            text = if (state.flipped) "ПЕРЕВОД" else "СЛОВО",
                            color = if (state.flipped) MintAccent else LavenderGlow
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "показано ${state.shown} · круг ${state.rounds + 1}",
                            style = PixelType.caption,
                            color = TextMuted,
                            modifier = Modifier.weight(1f)
                        )
                        if (!state.russianVoice) {
                            Text(
                                text = "нет русского голоса TTS",
                                style = PixelType.caption,
                                color = GoldAccent
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Готовим слова…", style = PixelType.chip, color = TextSecondary)
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Интервал меняется на лету
                Text(
                    text = "Секунд на карточку",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (INTERVALS + state.intervalSeconds).distinct().sorted().forEach { value ->
                        SelectChip(
                            text = "$value с",
                            selected = state.intervalSeconds == value,
                            onClick = { viewModel.setInterval(value) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Первую половину времени — иероглиф и китайская озвучка, " +
                        "вторую — перевод по-русски. Нажмите на карточку, чтобы повторить озвучку.",
                    style = PixelType.caption,
                    color = TextMuted,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(22.dp))

                // Управление
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GradientButton(
                        text = if (state.playing) "Пауза" else "Продолжить",
                        icon = if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        modifier = Modifier.weight(1f),
                        enabled = state.word != null,
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) { viewModel.togglePlaying() }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(CG.glass(0.08f))
                            .border(1.dp, LavenderGlow.copy(alpha = 0.35f), CircleShape)
                            .clickable(enabled = state.word != null) { viewModel.skip() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Следующее слово",
                            tint = LavenderGlow
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                GhostButton(
                    text = "Выключить режим",
                    icon = Icons.Filled.Stop,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    sounds.whoosh()
                    viewModel.pause()
                    onExit()
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        if (state.notEnoughWords) {
            Box(
                Modifier.fillMaxSize().background(CG.scrim),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    glyph = "空",
                    title = "В выбранных папках нет слов",
                    message = "Выберите папку со словами — например, раздел курса HSK или свою папку.",
                    action = { GradientButton(text = "К настройкам режима") { onOpenSettings() } }
                )
            }
        }
    }
}

/* ------------------------------- Карточка ------------------------------- */

@Composable
private fun FlipCard(
    word: Word,
    flipped: Boolean,
    showPinyin: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(520),
        label = "flip"
    )
    val showBack = rotation > 90f
    val shape = RoundedCornerShape(28.dp)
    val accent = if (showBack) MintAccent else VividPurple

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 250.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.32f), CG.glass(0.05f), accent.copy(alpha = 0.16f))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.55f), shape)
            .clickable(onClick = onClick)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // обратную сторону зеркалим назад, чтобы текст читался
        Column(
            modifier = Modifier.graphicsLayer { rotationY = if (showBack) 180f else 0f },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!showBack) {
                Text(
                    text = word.hanzi,
                    fontSize = if (word.hanzi.length > 4) 44.sp else 64.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                if (showPinyin && word.pinyin.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = word.pinyin,
                        style = MaterialTheme.typography.titleLarge,
                        color = LavenderGlow,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = word.translation,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (word.pinyin.isNotBlank()) "${word.hanzi} · ${word.pinyin}" else word.hanzi,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (showBack) "звучит перевод" else "звучит по-китайски",
                    style = PixelType.caption,
                    color = TextMuted
                )
            }
        }
    }
}
