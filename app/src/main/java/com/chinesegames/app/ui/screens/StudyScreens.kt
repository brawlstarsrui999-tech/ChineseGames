package com.chinesegames.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chinesegames.app.data.HskCourse
import com.chinesegames.app.data.HskGroupData
import com.chinesegames.app.data.HskTopicData
import com.chinesegames.app.data.HskWordData
import com.chinesegames.app.ui.components.CgTopBar
import com.chinesegames.app.ui.components.CircleIconButton
import com.chinesegames.app.ui.components.GlassCard
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.GradientProgress
import com.chinesegames.app.ui.components.PixelTag
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.SelectChip
import com.chinesegames.app.ui.components.StudyAccent
import com.chinesegames.app.ui.components.StudyGradient
import com.chinesegames.app.ui.components.VSpace
import com.chinesegames.app.ui.study.HskGameKind
import com.chinesegames.app.ui.study.StudyViewModel
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.LocalSpeaker
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.PixelType
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary

/** Что выбрано вверху раздела: слова или предложения. */
private enum class StudyTab { WORDS, SENTENCES }

/* ============================== Раздел целиком ============================== */

/**
 * «Поэтапное изучение»: сверху переключатель «Изучение слов» и
 * «Изучение предложений», ниже — уровни HSK 1…6 с прогрессом и замками.
 */
@Composable
fun StudyHomeScreen(
    viewModel: StudyViewModel,
    onOpenLevel: (Int) -> Unit,
    onOpenSentenceLevel: (Int) -> Unit
) {
    val sounds = LocalSounds.current
    val state by viewModel.state.collectAsState()
    var tab by remember { mutableStateOf(StudyTab.WORDS) }

    PurpleBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            CgTopBar(
                title = "Поэтапное изучение",
                subtitle = "HSK 1 → HSK 6 · группы по 5 слов",
                icon = Icons.Filled.School,
                iconTint = StudyAccent
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectChip(
                        text = "Изучение слов",
                        selected = tab == StudyTab.WORDS,
                        onClick = {
                            sounds.click()
                            tab = StudyTab.WORDS
                        }
                    )
                    SelectChip(
                        text = "Изучение предложений",
                        selected = tab == StudyTab.SENTENCES,
                        onClick = {
                            sounds.click()
                            tab = StudyTab.SENTENCES
                        }
                    )
                }

                VSpace(14.dp)

                if (tab == StudyTab.WORDS) {
                    Text(
                        text = "Учите 5 слов: знакомство с иероглифами, пиньинем и звучанием, " +
                            "а затем 6 игр без ошибок. Пройденные слова попадают в папку «Выученное».",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    VSpace(14.dp)
                    (1..HskCourse.LEVELS).forEach { level ->
                        val levelData = state.levels[level]
                        val unlocked = viewModel.isLevelUnlocked(level)
                        val progress = levelData?.let { viewModel.levelProgress(it) } ?: 0f
                        val learned = levelData?.let { viewModel.learnedWords(it) } ?: 0
                        val total = levelData?.words?.size ?: HskCourse.cumulativeWords(level)
                        val examPassed = state.exams[level]?.passed == true

                        LevelCard(
                            level = level,
                            unlocked = unlocked,
                            lockReason = viewModel.lockReason(level),
                            progress = progress,
                            learned = learned,
                            total = total,
                            examPassed = examPassed,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (unlocked) {
                                    sounds.click()
                                    onOpenLevel(level)
                                } else {
                                    sounds.error()
                                }
                            }
                        )
                        VSpace(12.dp)
                    }
                } else {
                    Text(
                        text = "Предложения открываются, когда все слова уровня выучены: " +
                            "4 игры — сборка фразы, перевод, понимание на слух и выбор озвучки.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    VSpace(14.dp)
                    (1..HskCourse.LEVELS).forEach { level ->
                        val levelData = state.levels[level]
                        val wordsDone = levelData?.let { viewModel.isSentencesUnlocked(it.level) } == true
                        val topicsCount = state.sentenceTopics[level]?.size ?: 0
                        val progress = viewModel.sentencesProgress(level)

                        SentenceLevelCard(
                            level = level,
                            unlocked = wordsDone,
                            topicsCount = topicsCount,
                            progress = progress,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (wordsDone) {
                                    sounds.click()
                                    onOpenSentenceLevel(level)
                                } else {
                                    sounds.error()
                                }
                            }
                        )
                        VSpace(12.dp)
                    }
                }

                VSpace(24.dp)
            }
        }
    }
}

@Composable
private fun LevelCard(
    level: Int,
    unlocked: Boolean,
    lockReason: String?,
    progress: Float,
    learned: Int,
    total: Int,
    examPassed: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            if (unlocked) StudyGradient else listOf(CG.glass(0.12f), CG.glass(0.06f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (unlocked) Icons.Filled.School else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (unlocked) Color.White else TextMuted,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "HSK $level",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    if (examPassed) {
                        Spacer(Modifier.width(8.dp))
                        PixelTag("ЭКЗАМЕН СДАН", color = MintAccent)
                    }
                }
                Text(
                    text = HskCourse.levelDescription(level),
                    style = PixelType.caption,
                    color = TextSecondary
                )
            }
        }

        VSpace(12.dp)
        GradientProgress(progress = progress, colors = StudyGradient, height = 8.dp)
        VSpace(8.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (unlocked) {
                    "Выучено $learned из $total слов"
                } else {
                    lockReason ?: "Закрыто"
                },
                style = PixelType.caption,
                color = if (unlocked) TextSecondary else TextMuted,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = PixelType.chip,
                color = StudyAccent
            )
        }
    }
}

@Composable
private fun SentenceLevelCard(
    level: Int,
    unlocked: Boolean,
    topicsCount: Int,
    progress: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            if (unlocked) StudyGradient else listOf(CG.glass(0.12f), CG.glass(0.06f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (unlocked) Icons.AutoMirrored.Filled.VolumeUp else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (unlocked) Color.White else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Предложения HSK $level",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (unlocked) {
                        "Разделов: $topicsCount · 4 игры в каждом"
                    } else {
                        "Сначала выучите все слова HSK $level"
                    },
                    style = PixelType.caption,
                    color = if (unlocked) TextSecondary else TextMuted
                )
            }
            if (unlocked) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = PixelType.chip,
                    color = StudyAccent
                )
            }
        }
    }
}

/* ================================ Уровень HSK =============================== */

/** Экран уровня: экзамен сверху, ниже — разделы со словами. */
@Composable
fun HskLevelScreen(
    level: Int,
    viewModel: StudyViewModel,
    onBack: () -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenExam: () -> Unit
) {
    val sounds = LocalSounds.current
    val state by viewModel.state.collectAsState()
    val levelData = state.levels[level]

    LaunchedEffect(level) { viewModel.openLevel(level) }

    PurpleBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            CgTopBar(
                title = "HSK $level",
                subtitle = HskCourse.levelDescription(level),
                icon = Icons.AutoMirrored.Filled.MenuBook,
                iconTint = StudyAccent,
                onBack = {
                    sounds.whoosh()
                    onBack()
                }
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                // Экзамен: открывает следующий уровень
                val exam = state.exams[level]
                GlassCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (exam?.passed == true) Icons.Filled.Verified else Icons.Filled.School,
                            contentDescription = null,
                            tint = if (exam?.passed == true) MintAccent else GoldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Экзамен HSK $level",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (exam?.passed == true) {
                                    "Сдан · верно ${exam.bestCorrect} из ${exam.asked}"
                                } else {
                                    "${HskCourse.EXAM_QUESTIONS} вопросов · проход 80% · открывает HSK ${level + 1}"
                                },
                                style = PixelType.caption,
                                color = TextSecondary
                            )
                        }
                    }
                    VSpace(12.dp)
                    GradientButton(
                        text = if (exam?.passed == true) "Пройти ещё раз" else "Пройти экзамен",
                        colors = StudyGradient,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        sounds.click()
                        onOpenExam()
                    }
                }

                VSpace(20.dp)

                val progress = levelData?.let { viewModel.levelProgress(it) } ?: 0f
                val learned = levelData?.let { viewModel.learnedWords(it) } ?: 0
                GlassCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Прогресс уровня",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = PixelType.chip,
                            color = StudyAccent
                        )
                    }
                    VSpace(10.dp)
                    GradientProgress(progress = progress, colors = StudyGradient, height = 8.dp)
                    VSpace(8.dp)
                    Text(
                        text = "Выучено слов: $learned из ${levelData?.words?.size ?: HskCourse.cumulativeWords(level)}",
                        style = PixelType.caption,
                        color = TextMuted
                    )
                }

                VSpace(20.dp)

                if (levelData == null) {
                    Text(
                        text = "Загружаем материал…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                } else {
                    levelData.topics.forEach { topic ->
                        TopicCard(
                            topic = topic,
                            progress = viewModel.topicProgress(topic),
                            learnedGroups = topic.groups.count { group ->
                                state.groups[group.key]?.learned == true
                            },
                            onClick = {
                                sounds.click()
                                onOpenTopic(topic.id)
                            }
                        )
                        VSpace(12.dp)
                    }
                }

                VSpace(24.dp)
            }
        }
    }
}

@Composable
private fun TopicCard(
    topic: HskTopicData,
    progress: Float,
    learnedGroups: Int,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = topic.emoji.ifBlank { "📖" }, fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${topic.wordCount} слов · ${topic.groups.size} " +
                        pluralGroups(topic.groups.size) + " · выучено $learnedGroups",
                    style = PixelType.caption,
                    color = TextSecondary
                )
            }
            Text(
                text = "${(progress * 100).toInt()}%",
                style = PixelType.chip,
                color = StudyAccent
            )
        }
        VSpace(10.dp)
        GradientProgress(progress = progress, colors = StudyGradient, height = 6.dp)
    }
}

private fun pluralGroups(count: Int): String = when {
    count % 10 == 1 && count % 100 != 11 -> "группа"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "группы"
    else -> "групп"
}

/* ================================= Раздел =================================== */

/** Раздел уровня: список групп по 5 слов. */
@Composable
fun HskTopicScreen(
    level: Int,
    topicId: String,
    viewModel: StudyViewModel,
    onBack: () -> Unit,
    onOpenGroup: (Int) -> Unit
) {
    val sounds = LocalSounds.current
    val state by viewModel.state.collectAsState()
    val topic = state.levels[level]?.topic(topicId)

    LaunchedEffect(level) { viewModel.openLevel(level) }

    PurpleBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            CgTopBar(
                title = topic?.title ?: "Раздел",
                subtitle = "HSK $level · группы по 5 слов",
                emoji = topic?.emoji,
                onBack = {
                    sounds.whoosh()
                    onBack()
                }
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (topic == null) {
                    Text(
                        text = "Загружаем материал…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                } else {
                    topic.groups.forEach { group ->
                        val progress = state.groups[group.key]
                        GroupCard(
                            group = group,
                            passedGames = progress?.passedGames ?: 0,
                            learned = progress?.learned == true,
                            onClick = {
                                sounds.click()
                                onOpenGroup(group.index)
                            }
                        )
                        VSpace(10.dp)
                    }
                }
                VSpace(24.dp)
            }
        }
    }
}

@Composable
private fun GroupCard(
    group: HskGroupData,
    passedGames: Int,
    learned: Boolean,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Группа ${group.number}",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (learned) {
                        Spacer(Modifier.width(8.dp))
                        PixelTag("ВЫУЧЕНО", color = MintAccent)
                    }
                }
                Text(
                    text = group.words.joinToString(" · ") { it.hanzi },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Text(
                text = "$passedGames/${HskCourse.GAMES_PER_GROUP}",
                style = PixelType.chip,
                color = if (learned) MintAccent else StudyAccent
            )
        }
        VSpace(10.dp)
        GradientProgress(
            progress = passedGames.toFloat() / HskCourse.GAMES_PER_GROUP,
            colors = if (learned) CG.successGradient else StudyGradient,
            height = 6.dp
        )
    }
}

/* ============================== Группа: урок ================================ */

/**
 * Урок группы: сначала показываем 5 слов (иероглиф, пиньинь, перевод, звук),
 * ниже — 6 игр. Группа засчитывается, когда все игры пройдены без ошибок.
 */
@Composable
fun HskGroupScreen(
    level: Int,
    topicId: String,
    groupIndex: Int,
    viewModel: StudyViewModel,
    onBack: () -> Unit,
    onOpenGame: (String) -> Unit
) {
    val sounds = LocalSounds.current
    val speaker = LocalSpeaker.current
    val state by viewModel.state.collectAsState()
    val group = state.levels[level]?.group(topicId, groupIndex)
    val progress = group?.let { state.groups[it.key] }

    LaunchedEffect(level) { viewModel.openLevel(level) }

    PurpleBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            CgTopBar(
                title = "Группа ${(groupIndex + 1)}",
                subtitle = group?.topicTitle ?: "HSK $level",
                icon = Icons.Filled.Style,
                iconTint = StudyAccent,
                onBack = {
                    sounds.whoosh()
                    onBack()
                }
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                if (group == null) {
                    Text(
                        text = "Загружаем материал…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                } else {
                    val passedGames = progress?.passedGames ?: 0

                    GlassCard(contentPadding = PaddingValues(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Шаг 1 · Знакомство со словами",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$passedGames/${HskCourse.GAMES_PER_GROUP} игр",
                                style = PixelType.chip,
                                color = StudyAccent
                            )
                        }
                        VSpace(10.dp)
                        GradientProgress(
                            progress = passedGames.toFloat() / HskCourse.GAMES_PER_GROUP,
                            colors = StudyGradient,
                            height = 8.dp
                        )
                    }

                    VSpace(14.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        group.words.forEach { word ->
                            WordCard(word = word, onSpeak = {
                                sounds.click()
                                speaker.speak(word.hanzi)
                            })
                        }
                    }

                    VSpace(20.dp)

                    if (progress?.learned == true) {
                        GlassCard(contentPadding = PaddingValues(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MintAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Группа выучена! Слова добавлены в папку «Выученное»",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        VSpace(16.dp)
                    }

                    Text(
                        text = "Шаг 2 · Шесть игр (без ошибок)",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    VSpace(10.dp)

                    HskGameKind.entries.forEach { game ->
                        val passed = (progress?.passedMask ?: 0) and game.bit != 0
                        GameRow(
                            title = game.title,
                            description = game.description,
                            passed = passed,
                            onClick = {
                                sounds.click()
                                onOpenGame(game.id)
                            }
                        )
                        VSpace(8.dp)
                    }
                }

                VSpace(24.dp)
            }
        }
    }
}

@Composable
private fun WordCard(word: HskWordData, onSpeak: () -> Unit) {
    GlassCard(
        modifier = Modifier.width(140.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = word.hanzi,
                fontSize = 28.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Прослушать",
                tint = StudyAccent,
                modifier = Modifier.size(32.dp)
            ) { onSpeak() }
        }
        VSpace(6.dp)
        Text(
            text = word.pinyin,
            style = PixelType.chip,
            color = LavenderGlow
        )
        VSpace(4.dp)
        Text(
            text = word.translation,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 3
        )
    }
}

@Composable
private fun GameRow(
    title: String,
    description: String,
    passed: Boolean,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.linearGradient(
                            if (passed) CG.successGradient else StudyGradient
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (passed) Icons.Filled.CheckCircle else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = PixelType.caption,
                    color = TextSecondary
                )
            }
            if (passed) {
                PixelTag("ПРОЙДЕНО", color = MintAccent)
            }
        }
    }
}
