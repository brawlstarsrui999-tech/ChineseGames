package com.chinesegames.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chinesegames.app.ui.game.CardKind
import com.chinesegames.app.ui.game.MatchCard
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.FuchsiaGlow
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.PixelType
import com.chinesegames.app.ui.theme.RoseAccent
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.VividPurple

/**
 * Игровая карточка: рубашка с пиксельным лепестком, 3D-переворот,
 * тряска при ошибке и «зелёная» отметка найденной пары.
 *
 * Размер задаётся снаружи (сетка [FitGrid] подбирает его так, чтобы всё
 * поместилось на экране), а шрифты и иконки масштабируются под ячейку.
 */
@Composable
fun MatchCardView(
    card: MatchCard,
    faceUp: Boolean,
    matched: Boolean,
    selected: Boolean,
    wrong: Boolean,
    shakeKey: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (faceUp) 180f else 0f,
        animationSpec = tween(
            durationMillis = if (faceUp) 340 else 240,
            easing = FastOutSlowInEasing
        ),
        label = "cardFlip"
    )

    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeKey > 0 && wrong) {
            shake.snapTo(0f)
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 460
                    -12f at 60 using LinearEasing
                    10f at 150
                    -7f at 240
                    5f at 330
                    0f at 460
                }
            )
        }
    }

    val matchScale by animateFloatAsState(
        targetValue = if (matched) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f),
        label = "matchScale"
    )

    val shape = RoundedCornerShape(12.dp)
    val borderColor = when {
        matched -> MintAccent.copy(alpha = 0.9f)
        wrong -> RoseAccent.copy(alpha = 0.95f)
        selected -> LavenderGlow.copy(alpha = 0.95f)
        else -> CG.glass(0.14f)
    }

    BoxWithConstraints(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 16f * density
                translationX = shake.value
                scaleX = matchScale
                scaleY = matchScale
                alpha = if (matched) 0.72f else 1f
            }
            .clip(shape)
            .background(
                if (faceUp) {
                    Brush.verticalGradient(CG.cardFace)
                } else {
                    Brush.linearGradient(CG.cardBack)
                }
            )
            .border(if (selected || matched || wrong) 2.dp else 1.dp, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        val cellHeight = maxHeight.value
        val hanziSize = (cellHeight * 0.38f).coerceIn(12f, 38f)
        val pinyinSize = (cellHeight * 0.17f).coerceIn(8f, 14f)
        val translationSize = (cellHeight * 0.22f).coerceIn(9f, 18f)

        if (rotation <= 90f) {
            CardBackFace(card.kind, cellHeight)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f },
                contentAlignment = Alignment.Center
            ) {
                when (card.kind) {
                    CardKind.HANZI -> Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = card.main,
                            fontSize = hanziSize.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!card.sub.isNullOrBlank()) {
                            Text(
                                text = card.sub,
                                fontSize = pinyinSize.sp,
                                color = LavenderGlow,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }

                    CardKind.RUSSIAN -> {
                        val length = card.main.length
                        val size = when {
                            length <= 10 -> translationSize
                            length <= 20 -> translationSize * 0.85f
                            else -> translationSize * 0.72f
                        }
                        Text(
                            text = card.main,
                            fontSize = size.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }

                    CardKind.AUDIO -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🔊", fontSize = (cellHeight * 0.34f).coerceIn(14f, 30f).sp)
                        if (cellHeight > 74f) {
                            Text(
                                text = "ещё раз",
                                fontSize = pinyinSize.sp,
                                color = GoldAccent,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        if (matched) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size((cellHeight * 0.22f).coerceIn(13f, 22f).dp)
                    .clip(CircleShape)
                    .background(MintAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color(0xFF05261A),
                    modifier = Modifier.size((cellHeight * 0.14f).coerceIn(8f, 14f).dp)
                )
            }
        }
    }
}

@Composable
private fun CardBackFace(kind: CardKind, cellHeight: Float) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            listOf(0.42f, 0.30f, 0.18f).forEach { k ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = size.minDimension * k,
                    center = center
                )
            }
        }
        // Пиксельный лепесток в углу — «анимешная» деталь рубашки
        PixelSpriteFit(
            sprite = PetalSprite,
            width = (cellHeight * 0.3f).coerceIn(14f, 26f).dp,
            height = (cellHeight * 0.3f).coerceIn(14f, 26f).dp,
            alpha = 0.55f,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(3.dp)
        )
        Text(
            text = if (kind == CardKind.AUDIO) "🎧" else "汉",
            fontSize = (cellHeight * (if (kind == CardKind.AUDIO) 0.34f else 0.38f))
                .coerceIn(13f, 34f).sp,
            color = Color.White.copy(alpha = if (kind == CardKind.AUDIO) 0.7f else 0.30f)
        )
    }
}

/** Мини-легенда под игровым полем. */
@Composable
fun CardsLegend(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(FuchsiaGlow)
        )
        Text(
            text = " иероглиф + пиньинь",
            style = PixelType.caption,
            color = LavenderGlow
        )
        Text(
            text = "   ·   ",
            style = PixelType.caption,
            color = VividPurple
        )
        Box(
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(MintAccent)
        )
        Text(
            text = " перевод",
            style = PixelType.caption,
            color = MintAccent
        )
    }
}
