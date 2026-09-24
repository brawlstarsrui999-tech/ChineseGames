package com.chinesegames.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chinesegames.app.data.DailyStat
import com.chinesegames.app.ui.DeckViewModel
import com.chinesegames.app.ui.components.CgTopBar
import com.chinesegames.app.ui.components.EmptyState
import com.chinesegames.app.ui.components.GlassCard
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.GradientProgress
import com.chinesegames.app.ui.components.PixelCatWisdom
import com.chinesegames.app.ui.components.PixelDivider
import com.chinesegames.app.ui.components.PixelLanternRow
import com.chinesegames.app.ui.components.PandaSprite
import com.chinesegames.app.ui.components.PixelTag
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.SectionTitle
import com.chinesegames.app.ui.components.StatPill
import com.chinesegames.app.ui.components.StatRow
import com.chinesegames.app.ui.components.VSpace
import com.chinesegames.app.ui.formatPercent
import com.chinesegames.app.ui.formatTime
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Статистика: итоги, график по дням, прогресс по папкам
 * и список самых сложных слов с быстрым запуском тренировки.
 */
@Composable
fun StatsScreen(
    viewModel: DeckViewModel,
    onBack: (() -> Unit)? = null,
    onTrainHardWords: () -> Unit
) {
    val sounds = LocalSounds.current
    val gamesPlayed by viewModel.gamesPlayed.collectAsState()
    val pairsFound by viewModel.pairsFound.collectAsState()
    val accuracy by viewModel.averageAccuracy.collectAsState()
    val learnedWords by viewModel.learnedWords.collectAsState()
    val totalWords by viewModel.totalWords.collectAsState()
    val totalSeconds by viewModel.totalSeconds.collectAsState()
    val bestScore by viewModel.bestScore.collectAsState()

    val decks by viewModel.decks.collectAsState()
    val deckStats by viewModel.deckStats.collectAsState()

    val dailyFlow = remember { viewModel.dailyStats(DAYS) }
    val daily by dailyFlow.collectAsState(initial = emptyList())

    val hardFlow = remember { viewModel.hardWords(8) }
    val hardWords by hardFlow.collectAsState(initial = emptyList())

    PurpleBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            CgTopBar(
                title = "Статистика",
                subtitle = "Прогресс по дням и слабые слова",
                emoji = "📊",
                onBack = onBack?.let { handler ->
                    {
                        sounds.whoosh()
                        handler()
                    }
                }
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatPill(
                        icon = Icons.Filled.SportsEsports,
                        value = "$gamesPlayed",
                        label = "партий",
                        modifier = Modifier.weight(1f),
                        accent = SkyAccent
                    )
                    StatPill(
                        icon = Icons.Filled.Bolt,
                        value = "$pairsFound",
                        label = "пар найдено",
                        modifier = Modifier.weight(1f),
                        accent = LavenderGlow
                    )
                }
                VSpace(10.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatPill(
                        icon = Icons.Filled.School,
                        value = "$learnedWords из $totalWords",
                        label = "уверенно знаю",
                        modifier = Modifier.weight(1f),
                        accent = MintAccent
                    )
                    StatPill(
                        icon = Icons.Filled.EmojiEvents,
                        value = "$bestScore",
                        label = "рекорд очков",
                        modifier = Modifier.weight(1f),
                        accent = GoldAccent
                    )
                }

                VSpace(24.dp)
                SectionTitle("По дням")
                VSpace(10.dp)
                GlassCard(contentPadding = PaddingValues(16.dp)) {
                    if (daily.isEmpty()) {
                        Text(
                            text = "Пока нет ни одной партии — сыграйте первую, и здесь появится график.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        DailyChart(days = daily.reversed())
                        VSpace(12.dp)
                        PixelDivider()
                        VSpace(12.dp)
                        StatRow("Всего времени в играх", formatTime(totalSeconds))
                        StatRow("Средняя точность", formatPercent(accuracy), valueColor = MintAccent)
                        StatRow(
                            "Активных дней",
                            "${daily.size} из $DAYS",
                            valueColor = LavenderGlow
                        )
                    }
                }

                VSpace(24.dp)
                SectionTitle("Слабые слова")
                VSpace(10.dp)
                GlassCard(contentPadding = PaddingValues(16.dp)) {
                    if (hardWords.isEmpty()) {
                        Text(
                            text = "Как только слова начнут попадаться в играх, здесь появятся те, " +
                                "что даются хуже всего.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        hardWords.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.hanzi,
                                    fontSize = 20.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = row.translation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    if (row.pinyin.isNotBlank()) {
                                        Text(
                                            text = row.pinyin,
                                            style = PixelType.caption,
                                            color = LavenderGlow
                                        )
                                    }
                                }
                                PixelTag(
                                    text = formatPercent(row.accuracy),
                                    color = when {
                                        row.accuracy < 0.4f -> RoseAccent
                                        row.accuracy < 0.7f -> GoldAccent
                                        else -> MintAccent
                                    }
                                )
                            }
                        }
                        VSpace(12.dp)
                        GradientButton(
                            text = "Тренировать слабые слова",
                            icon = Icons.Filled.PlayArrow,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                sounds.match()
                                onTrainHardWords()
                            }
                        )
                    }
                }

                VSpace(24.dp)
                SectionTitle("Прогресс по папкам")
                VSpace(10.dp)
                if (decks.isEmpty()) {
                    EmptyState(
                        glyph = "字",
                        title = "Папок пока нет",
                        message = "Создайте папки со словами — здесь появится прогресс по каждой из них."
                    )
                } else {
                    GlassCard(contentPadding = PaddingValues(16.dp)) {
                        val statsByDeck = deckStats.associateBy { it.deckId }
                        // разделы курса сворачиваем в папку уровня («HSK 3»)
                        val childrenOf = decks.filter { it.parentId != null }.groupBy { it.parentId!! }
                        decks.filter { it.parentId == null }.forEach { deck ->
                            val own = statsByDeck[deck.id]
                            val kids = childrenOf[deck.id].orEmpty()
                            val total = (own?.total ?: 0) + kids.sumOf { statsByDeck[it.id]?.total ?: 0 }
                            val learned = (own?.learned ?: 0) + kids.sumOf { statsByDeck[it.id]?.learned ?: 0 }
                            Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = deck.emoji, fontSize = 16.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = deck.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = if (total == 0) "нет слов" else "$learned / $total",
                                        style = PixelType.caption,
                                        color = if (learned == total && total > 0) MintAccent else TextMuted
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                GradientProgress(
                                    progress = if (total == 0) 0f else learned.toFloat() / total,
                                    height = 6.dp
                                )
                            }
                        }
                    }
                }

                VSpace(26.dp)
                PixelLanternRow()
                VSpace(16.dp)
                PixelCatWisdom(
                    text = "Слабые слова — это слова, с которыми вы редко встречались",
                    modifier = Modifier.fillMaxWidth(),
                    sprite = PandaSprite
                )
                VSpace(30.dp)
            }
        }
    }
}

private const val DAYS = 14

/** Столбики «сколько минут и слов за день». */
@Composable
private fun DailyChart(days: List<DailyStat>, modifier: Modifier = Modifier) {
    val maxSeconds = (days.maxOfOrNull { it.seconds } ?: 1).coerceAtLeast(1)
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM", Locale("ru")) }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val count = days.size.coerceAtLeast(1)
            val gap = size.width * 0.012f
            val barWidth = (size.width - gap * (count - 1)) / count
            days.forEachIndexed { index, day ->
                val ratio = day.seconds.toFloat() / maxSeconds
                val barHeight = (size.height - 26f) * ratio.coerceAtLeast(0.04f)
                val x = index * (barWidth + gap)
                val y = size.height - 20f - barHeight
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(VividPurple, LavenderGlow)),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth * 0.35f)
                )
                if (day.answers > 0) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.55f),
                        radius = 2.2f,
                        center = Offset(x + barWidth / 2f, y - 5f)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val first = days.firstOrNull()
            val last = days.lastOrNull()
            Text(
                text = first?.let { dayLabel(it.day, formatter) }.orEmpty(),
                style = PixelType.caption,
                color = TextMuted
            )
            Text(
                text = "минуты по дням",
                style = PixelType.caption,
                color = TextMuted
            )
            Text(
                text = last?.let { dayLabel(it.day, formatter) }.orEmpty(),
                style = PixelType.caption,
                color = TextMuted
            )
        }
    }
}

private fun dayLabel(day: String, formatter: DateTimeFormatter): String = runCatching {
    LocalDate.parse(day).format(formatter)
}.getOrDefault(day)
