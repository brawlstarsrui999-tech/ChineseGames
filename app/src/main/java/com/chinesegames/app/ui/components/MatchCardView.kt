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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.chinesegames.app.ui.theme.FuchsiaGlow
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.RoseAccent
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.VividPurple

/**
 * Игровая карточка: рубашка с иероглифом-водяным знаком, 3D-переворот,
 * тряска при ошибке и «зелёная» отметка найденной пары.
 */
@Composable
fun MatchCardView(
    card: MatchCard,
    faceUp: Boolean,
    matched: Boolean,
    selected: Boolean,
    wrong: Boolean,
    shakeKey: Int,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (faceUp) 180f else 0f,
        animationSpec = tween(
            durationMillis = if (faceUp) 380 else 280,
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
                    -16f at 60 using LinearEasing
                    13f at 150
                    -9f at 240
                    6f at 330
                    0f at 460
                }
            )
        }
    }

    val matchScale by animateFloatAsState(
        targetValue = if (matched) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f),
        label = "matchScale"
    )

    val shape = RoundedCornerShape(18.dp)
    val borderColor = when {
        matched -> MintAccent.copy(alpha = 0.9f)
        wrong -> RoseAccent.copy(alpha = 0.95f)
        selected -> LavenderGlow.copy(alpha = 0.95f)
        else -> Color.White.copy(alpha = 0.12f)
    }

    Box(
        modifier = modifier
            .aspectRatio(if (compact) 0.82f else 0.74f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 18f * density
                translationX = shake.value
                scaleX = matchScale
                scaleY = matchScale
                alpha = if (matched) 0.75f else 1f
            }
            .clip(shape)
            .background(
                if (faceUp) {
                    Brush.verticalGradient(
                        listOf(Color(0xFF2B1657), Color(0xFF1A0E38))
                    )
                } else {
                    Brush.linearGradient(
                        listOf(Color(0xFF7C3AED), Color(0xFF4C1D95), Color(0xFF2E1065))
                    )
                }
            )
            .border(if (selected || matched || wrong) 2.dp else 1.dp, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (rotation <= 90f) {
            CardBackFace(card.kind)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f },
                contentAlignment = Alignment.Center
            ) {
                CardFrontFace(card, compact)
            }
        }

        if (matched) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MintAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color(0xFF05261A),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun CardBackFace(kind: CardKind) {
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
        Text(
            text = if (kind == CardKind.AUDIO) "🎧" else "汉",
            fontSize = if (kind == CardKind.AUDIO) 30.sp else 34.sp,
            color = Color.White.copy(alpha = if (kind == CardKind.AUDIO) 0.65f else 0.25f)
        )
    }
}

@Composable
private fun CardFrontFace(card: MatchCard, compact: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (card.kind) {
            CardKind.HANZI -> {
                Text(
                    text = card.main,
                    fontSize = if (compact) 26.sp else 34.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!card.sub.isNullOrBlank()) {
                    Text(
                        text = card.sub,
                        fontSize = if (compact) 11.sp else 13.sp,
                        color = LavenderGlow,
                        textAlign = TextAlign.Center
                    )
                }
            }

            CardKind.RUSSIAN -> {
                val length = card.main.length
                val fontSize = when {
                    length <= 10 -> if (compact) 14.sp else 16.sp
                    length <= 22 -> if (compact) 12.sp else 14.sp
                    else -> if (compact) 11.sp else 12.sp
                }
                Text(
                    text = card.main,
                    fontSize = fontSize,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            CardKind.AUDIO -> {
                Text(text = "🔊", fontSize = 30.sp)
                Text(
                    text = if (compact) "ещё раз" else "нажми ещё раз",
                    fontSize = 10.sp,
                    color = GoldAccent,
                    textAlign = TextAlign.Center
                )
            }
        }
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
            style = MaterialTheme.typography.labelSmall,
            color = LavenderGlow
        )
        Text(
            text = "   ·   ",
            style = MaterialTheme.typography.labelSmall,
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
            style = MaterialTheme.typography.labelSmall,
            color = MintAccent
        )
    }
}
