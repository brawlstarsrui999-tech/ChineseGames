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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chinesegames.app.data.Word
import com.chinesegames.app.ui.DeckViewModel
import com.chinesegames.app.ui.components.CgTopBar
import com.chinesegames.app.ui.components.EmptyState
import com.chinesegames.app.ui.components.GlassCard
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.PixelTag
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.game.GameKind
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.LocalSpeaker
import com.chinesegames.app.ui.theme.OnAccentInk
import com.chinesegames.app.ui.theme.PixelType
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary
import com.chinesegames.app.ui.wordsLabel

/**
 * Папка «Избранное»: слова со звёздочкой из всех папок.
 * Отсюда можно сразу запустить игру только по ним.
 */
@Composable
fun FavoritesScreen(
    viewModel: DeckViewModel,
    onBack: () -> Unit,
    onPlay: (game: GameKind) -> Unit
) {
    val sounds = LocalSounds.current
    val speaker = LocalSpeaker.current

    val flow = remember { viewModel.favoriteWords() }
    val words by flow.collectAsState(initial = emptyList())
    val decks by viewModel.decks.collectAsState()

    val deckNames = remember(decks) { decks.associate { it.id to it.name } }

    PurpleBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            CgTopBar(
                title = "Избранное",
                subtitle = wordsLabel(words.size),
                emoji = "⭐",
                onBack = {
                    sounds.whoosh()
                    onBack()
                }
            )

            if (words.isEmpty()) {
                EmptyState(
                    glyph = "⭐",
                    title = "Пока пусто",
                    message = "Нажмите звёздочку у слова в любой папке — оно появится здесь, " +
                        "и по этим словам можно будет играть отдельно."
                )
            } else {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        GradientButton(
                            text = "Найди пару",
                            icon = Icons.Filled.SportsEsports,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            sounds.click()
                            onPlay(GameKind.MATCH)
                        }
                        GradientButton(
                            text = "Спринт",
                            icon = Icons.Filled.PlayArrow,
                            modifier = Modifier.weight(1f),
                            colors = CG.audioGradient,
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            sounds.click()
                            onPlay(GameKind.SPRINT)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(words, key = { it.id }) { word ->
                        FavoriteRow(
                            word = word,
                            deckName = deckNames[word.deckId].orEmpty(),
                            onSpeak = { speaker.speak(word.hanzi) },
                            onRemove = {
                                sounds.error()
                                viewModel.toggleFavorite(word.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteRow(
    word: Word,
    deckName: String,
    onSpeak: () -> Unit,
    onRemove: () -> Unit
) {
    GlassCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = word.hanzi,
                        fontSize = 26.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    if (word.pinyin.isNotBlank()) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = word.pinyin,
                            style = PixelType.chip,
                            color = LavenderGlow,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = word.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                if (deckName.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    PixelTag(text = deckName.uppercase(), color = GoldAccent)
                }
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(CG.glass(0.07f))
                    .clickable(onClick = onSpeak),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Озвучить",
                    tint = LavenderGlow,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(CG.goldGradient))
                    .border(1.dp, Color.Transparent, CircleShape)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Убрать из избранного",
                    tint = OnAccentInk,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** Подсказка, что «Избранное» участвует в играх как отдельная папка. */
@Composable
fun FavoritesHint(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CG.glass(0.05f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.StarBorder, contentDescription = null, tint = TextMuted)
        Spacer(Modifier.width(10.dp))
        Text(
            text = "В настройке партии «Избранное» и «Сложные слова» — отдельные источники слов",
            style = PixelType.caption,
            color = TextMuted
        )
    }
}
