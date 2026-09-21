package com.chinesegames.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chinesegames.app.ui.formatPercent
import com.chinesegames.app.ui.formatTime
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
import kotlinx.coroutines.delay

/** Состояние варианта ответа — от него зависит и цвет, и анимация. */
enum class AnswerState { IDLE, CORRECT, WRONG, DIMMED }

/** Пиксельный чип для шапки игры: иконка + значение. */
@Composable
fun HudChip(
    icon: ImageVector,
    text: String,
    accent: Color = LavenderGlow,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CG.glass(0.08f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = PixelType.chip,
            color = TextPrimary
        )
    }
}

/** Крупный пиксельный счётчик (очки, серия, время). */
@Composable
fun HudCounter(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CG.glass(0.07f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, style = PixelType.hud, color = accent)
        Text(text = label, style = PixelType.caption, color = TextMuted)
    }
}

/** Шапка игрового экрана: выход, название, счётчики и прогресс. */
@Composable
fun QuizHeader(
    title: String,
    subtitle: String,
    progress: Float,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    right: @Composable () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                onClick = onBack
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(text = subtitle, style = PixelType.caption, color = TextMuted)
            }
            right()
        }
        Spacer(Modifier.height(10.dp))
        GradientProgress(progress = progress, height = 7.dp)
    }
}

/** Плитка-вариант ответа. */
@Composable
fun AnswerTile(
    text: String,
    sub: String?,
    state: AnswerState,
    modifier: Modifier = Modifier,
    big: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = if (pressed) 0.97f else 1f

    val shape = RoundedCornerShape(if (big) 22.dp else 16.dp)
    val background = when (state) {
        AnswerState.CORRECT -> Brush.linearGradient(CG.successGradient)
        AnswerState.WRONG -> Brush.linearGradient(CG.dangerGradient)
        AnswerState.DIMMED -> Brush.linearGradient(
            listOf(CG.glass(0.04f), CG.glass(0.02f))
        )

        AnswerState.IDLE -> Brush.linearGradient(
            listOf(CG.glass(0.10f), CG.glass(0.05f))
        )
    }
    val border = when (state) {
        AnswerState.CORRECT -> MintAccent
        AnswerState.WRONG -> RoseAccent
        AnswerState.DIMMED -> CG.glass(0.06f)
        AnswerState.IDLE -> LavenderGlow.copy(alpha = 0.35f)
    }

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(background)
            .border(if (state == AnswerState.CORRECT || state == AnswerState.WRONG) 2.dp else 1.dp, border, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val length = text.length
            val base = when {
                big -> 26f
                length <= 8 -> 19f
                length <= 16 -> 16f
                length <= 26 -> 14f
                else -> 12f
            }
            Text(
                text = text,
                fontSize = base.sp,
                color = if (state == AnswerState.CORRECT || state == AnswerState.WRONG) Color.White else TextPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!sub.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = sub,
                    style = PixelType.caption,
                    color = if (state == AnswerState.CORRECT || state == AnswerState.WRONG) {
                        Color.White.copy(alpha = 0.85f)
                    } else {
                        LavenderGlow
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Экран итогов партии: звёзды, статистика и кнопки. */
@Composable
fun QuizResultOverlay(
    gameTitle: String,
    score: Int,
    correct: Int,
    asked: Int,
    mistakes: Int,
    accuracy: Float,
    bestCombo: Int,
    seconds: Int,
    stars: Int,
    finished: Boolean,
    onRestart: () -> Unit,
    onRestartSameWords: (() -> Unit)? = null,
    onSettings: () -> Unit,
    onExit: () -> Unit
) {
    val sounds = LocalSounds.current
    var visibleStars by remember { mutableIntStateOf(0) }

    LaunchedEffect(finished) {
        if (finished) {
            visibleStars = 0
            repeat(stars) { index ->
                delay(420)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelSpriteFit(
                    sprite = if (accuracy >= 0.8f) ToriiSprite else LanternSprite,
                    width = 34.dp,
                    height = 36.dp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (accuracy >= 0.8f) "Отлично!" else "Партия окончена",
                    style = MaterialTheme.typography.displaySmall.copy(
                        brush = Brush.horizontalGradient(listOf(GoldAccent, Color.White, LavenderGlow))
                    ),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "$gameTitle · ${formatTime(seconds)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(3) { index ->
                    val filled = index < visibleStars
                    val iconSize by animateDpAsState(
                        targetValue = if (filled) 44.dp else 36.dp,
                        animationSpec = spring(dampingRatio = 0.45f, stiffness = 600f),
                        label = "starSize"
                    )
                    Icon(
                        imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = if (filled) GoldAccent else TextMuted.copy(alpha = 0.45f),
                        modifier = Modifier
                            .size(iconSize)
                            .padding(horizontal = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            GlassCard(contentPadding = PaddingValues(16.dp)) {
                StatRow("Очки", "$score", valueColor = GoldAccent)
                StatRow("Верных ответов", "$correct из $asked")
                StatRow("Ошибок", "$mistakes", valueColor = if (mistakes == 0) MintAccent else RoseAccent)
                StatRow("Точность", formatPercent(accuracy), valueColor = MintAccent)
                StatRow("Лучшая серия", "×$bestCombo", valueColor = LavenderGlow)
                StatRow("Время", formatTime(seconds))
            }

            Spacer(Modifier.height(18.dp))

            GradientButton(
                text = "Ещё раз · новые слова",
                icon = Icons.Filled.Refresh,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                sounds.click()
                onRestart()
            }
            if (onRestartSameWords != null) {
                Spacer(Modifier.height(9.dp))
                GhostButton(
                    text = "Повторить эти же слова",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        sounds.click()
                        onRestartSameWords()
                    }
                )
            }
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

/** Счётчик «сколько карточек ещё не открыто» — для мемори-игр. */
@Composable
fun HudBadgeRow(
    items: List<Pair<ImageVector, String>>,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (icon, text) ->
            HudChip(icon = icon, text = text, accent = SkyAccent)
        }
    }
}

/** Крупная плашка с подсказкой-вопросом (например, 汉字 или перевод). */
@Composable
fun PromptPanel(
    modifier: Modifier = Modifier,
    accent: Color = VividPurple,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(CG.cardGradient))
            .border(1.5.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(26.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Маленькая иконка-подсказка «не знаю» для игр на скорость. */
@Composable
fun SkipButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .clip(RoundedCornerShape(10.dp))
            .background(CG.glass(0.06f))
            .border(1.dp, TextMuted.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(text = text, style = PixelType.chip, color = TextMuted)
    }
}

/** Строка «игра пройдена» с иконками наград — используется в итогах. */
@Composable
fun RewardRow(score: Int, best: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HudChip(Icons.Filled.EmojiEvents, "рекорд $best", GoldAccent, Modifier.weight(1f))
        HudChip(Icons.Filled.LocalFireDepartment, "комбо", RoseAccent, Modifier.weight(1f))
        HudChip(Icons.Filled.Timer, "$score", SkyAccent, Modifier.weight(1f))
    }
}
