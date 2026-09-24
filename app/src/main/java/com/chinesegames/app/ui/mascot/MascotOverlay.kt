package com.chinesegames.app.ui.mascot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chinesegames.app.audio.MascotLine
import com.chinesegames.app.audio.MascotVoice
import com.chinesegames.app.billing.PurchaseManager
import com.chinesegames.app.data.GameEvents
import com.chinesegames.app.data.MascotCorner
import com.chinesegames.app.data.Product
import com.chinesegames.app.data.SettingsStore
import com.chinesegames.app.ui.components.MascotHappySprite
import com.chinesegames.app.ui.components.MascotSprite
import com.chinesegames.app.ui.components.PixelSpriteView
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.PixelType
import com.chinesegames.app.ui.theme.TextPrimary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Чиби-талисман: девочка с голубыми волосами, которая живёт в углу экрана.
 *
 *  * хвалит голосом по итогам партии («Отлично!», «Хороший результат, малыш!»…)
 *    и здоровается по касанию — реплики записаны, запасной вариант — русский TTS;
 *  * перетаскивается пальцем и «прилипает» к ближайшему углу; угол запоминается;
 *  * показывается только после покупки «Чиби-талисман» и пока включён в настройках.
 *
 * Лежит поверх всего интерфейса (см. MainActivity), поэтому не мешает навигации:
 * нижние углы приподняты над панелью вкладок.
 */
@Composable
fun MascotOverlay(
    settings: SettingsStore,
    purchases: PurchaseManager,
    voice: MascotVoice,
    modifier: Modifier = Modifier
) {
    val owned by purchases.owned.collectAsState()
    val prefs by settings.state.collectAsState()
    if (Product.MASCOT !in owned || !prefs.mascotEnabled) return

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val statusBars = WindowInsets.statusBars.asPaddingValues()
    val navBars = WindowInsets.navigationBars.asPaddingValues()
    val scope = rememberCoroutineScope()

    var speaking by remember { mutableStateOf(false) }
    var bubble by remember { mutableStateOf<String?>(null) }
    var bubbleJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(voice) {
        voice.onSpeakingChange = { speaking = it }
        onDispose {
            voice.onSpeakingChange = null
            voice.stop()
        }
    }

    fun say(line: MascotLine) {
        voice.say(line)
        bubble = line.text
        bubbleJob?.cancel()
        bubbleJob = scope.launch {
            delay(3200)
            bubble = null
        }
    }

    // Итоги партий приходят через общий поток событий.
    LaunchedEffect(Unit) {
        GameEvents.finished.collect { outcome ->
            delay(650) // даём экрану победы появиться
            say(MascotLine.forAccuracy(outcome.accuracy, outcome.stars))
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val spriteHeight = 76.dp
        val spriteWidth = spriteHeight * MascotSprite.aspect
        val sideMargin = 10.dp
        val topMargin = statusBars.calculateTopPadding() + 8.dp
        val bottomMargin = navBars.calculateBottomPadding() + BOTTOM_BAR_CLEARANCE

        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val spriteWidthPx = with(density) { spriteWidth.toPx() }
        val spriteHeightPx = with(density) { spriteHeight.toPx() }

        fun cornerOffset(corner: MascotCorner): Offset {
            val x = if (corner.isEnd) {
                widthPx - spriteWidthPx - with(density) { sideMargin.toPx() }
            } else {
                with(density) { sideMargin.toPx() }
            }
            val y = if (corner.isTop) {
                with(density) { topMargin.toPx() }
            } else {
                heightPx - spriteHeightPx - with(density) { bottomMargin.toPx() }
            }
            return Offset(x, y)
        }

        val position = remember(widthPx, heightPx) {
            Animatable(cornerOffset(prefs.mascotCorner), Offset.VectorConverter)
        }
        var dragging by remember { mutableStateOf(false) }

        // Смена угла из настроек — плавно переезжаем.
        LaunchedEffect(prefs.mascotCorner, widthPx, heightPx) {
            if (!dragging) position.animateTo(cornerOffset(prefs.mascotCorner), spring(dampingRatio = 0.7f))
        }

        val breath = rememberInfiniteTransition(label = "mascotBreath")
        val bob by breath.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
            label = "bob"
        )

        val corner = prefs.mascotCorner
        val bubbleAbove = !corner.isTop

        Box(
            modifier = Modifier
                .offset { IntOffset(position.value.x.roundToInt(), position.value.y.roundToInt()) }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { say(MascotLine.greeting()) })
                }
                .pointerInput(widthPx, heightPx) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            dragging = false
                            val center = position.value + Offset(spriteWidthPx / 2f, spriteHeightPx / 2f)
                            val top = center.y < heightPx / 2f
                            val end = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) {
                                center.x < widthPx / 2f
                            } else {
                                center.x > widthPx / 2f
                            }
                            val target = MascotCorner.of(top = top, end = end)
                            scope.launch { position.animateTo(cornerOffset(target), spring(dampingRatio = 0.65f)) }
                            if (target != prefs.mascotCorner) settings.setMascotCorner(target)
                        },
                        onDragCancel = { dragging = false }
                    ) { change, dragAmount ->
                        change.consume()
                        val next = position.value + dragAmount
                        scope.launch {
                            position.snapTo(
                                Offset(
                                    next.x.coerceIn(0f, (widthPx - spriteWidthPx).coerceAtLeast(0f)),
                                    next.y.coerceIn(0f, (heightPx - spriteHeightPx).coerceAtLeast(0f))
                                )
                            )
                        }
                    }
                }
        ) {
            Column(horizontalAlignment = if (corner.isEnd) Alignment.End else Alignment.Start) {
                if (bubbleAbove) {
                    SpeechBubble(text = bubble, visible = bubble != null)
                }
                Box(
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .graphicsLayer {
                            translationY = -bob * 4.dp.toPx()
                            val pulse = if (speaking) 1f + 0.04f * bob else 1f
                            scaleX = pulse
                            scaleY = pulse
                        }
                ) {
                    PixelSpriteView(
                        sprite = if (speaking) MascotHappySprite else MascotSprite,
                        size = spriteHeight
                    )
                }
                if (!bubbleAbove) {
                    SpeechBubble(text = bubble, visible = bubble != null)
                }
            }
        }
    }
}

@Composable
private fun SpeechBubble(text: String?, visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f)
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 200.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(CG.surface))
                .border(1.dp, GoldAccent.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = text.orEmpty(),
                style = PixelType.caption,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Высота нижней панели вкладок плюс небольшой зазор. */
private val BOTTOM_BAR_CLEARANCE = 92.dp
