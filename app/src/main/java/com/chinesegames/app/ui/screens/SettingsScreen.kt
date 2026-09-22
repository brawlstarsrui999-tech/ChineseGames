package com.chinesegames.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chinesegames.app.ui.DeckViewModel
import com.chinesegames.app.ui.components.CgTopBar
import com.chinesegames.app.ui.components.GlassCard
import com.chinesegames.app.ui.components.PixelCatWisdom
import com.chinesegames.app.ui.components.PixelDivider
import com.chinesegames.app.ui.components.PixelLanternRow
import com.chinesegames.app.ui.components.PixelTag
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.SectionTitle
import com.chinesegames.app.ui.components.SelectChip
import com.chinesegames.app.ui.components.VSpace
import com.chinesegames.app.ui.decksLabel
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LavenderGlow
import com.chinesegames.app.ui.theme.LocalMusic
import com.chinesegames.app.ui.theme.LocalSettings
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.LocalSpeaker
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.PixelType
import com.chinesegames.app.ui.theme.RoseAccent
import com.chinesegames.app.ui.theme.SkyAccent
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary
import com.chinesegames.app.ui.theme.VividPurple
import com.chinesegames.app.ui.wordsLabel
import com.chinesegames.app.data.CsvImportSummary
import com.chinesegames.app.data.DisplayMode
import com.chinesegames.app.data.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Настройки: тема, музыка и звук, интервальное повторение,
 * а также экспорт/импорт словаря в CSV.
 */
@Composable
fun SettingsScreen(
    viewModel: DeckViewModel,
    onBack: (() -> Unit)? = null
) {
    val sounds = LocalSounds.current
    val music = LocalMusic.current
    val speaker = LocalSpeaker.current
    val settingsStore = LocalSettings.current
    val settings by settingsStore.state.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val decks by viewModel.decks.collectAsState()
    val totalWords by viewModel.totalWords.collectAsState()

    var csvMessage by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.exportCsv { csv ->
            scope.launch {
                val ok = withContext(Dispatchers.IO) { writeText(context, uri, csv) }
                csvMessage = if (ok) {
                    "Словарь выгружен: $totalWords ${if (totalWords % 10 == 1) "слово" else "слов"}"
                } else {
                    "Не удалось записать файл"
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) { readText(context, uri) }
            if (text.isNullOrBlank()) {
                csvMessage = "Файл пустой или недоступен"
            } else {
                viewModel.importCsv(text) { summary: CsvImportSummary ->
                    csvMessage = "Импорт: +${summary.wordsAdded} слов, " +
                        "${summary.decksCreated} новых папок, пропущено ${summary.wordsSkipped}"
                    sounds.match()
                }
            }
        }
    }

    PurpleBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            CgTopBar(
                title = "Настройки",
                subtitle = "Тема, музыка, словарь",
                emoji = "⚙️",
                onBack = onBack?.let { handler ->
                    {
                        sounds.whoosh()
                        handler()
                    }
                }
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                SectionTitle("Тема оформления")
                VSpace(10.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        ThemeCard(
                            mode = mode,
                            selected = settings.theme == mode,
                            modifier = Modifier.weight(1f)
                        ) {
                            sounds.click()
                            settingsStore.setTheme(mode)
                            music.setDayTrack(mode == ThemeMode.DAY)
                        }
                    }
                }

                VSpace(24.dp)
                SectionTitle("Звук и музыка")
                VSpace(10.dp)

                GlassCard(contentPadding = PaddingValues(16.dp)) {
                    ToggleRow(
                        icon = if (settings.soundEnabled) {
                            Icons.AutoMirrored.Filled.VolumeUp
                        } else {
                            Icons.AutoMirrored.Filled.VolumeOff
                        },
                        title = "Звуки игр",
                        subtitle = "Щелчки, совпадения, комбо",
                        checked = settings.soundEnabled,
                        accent = LavenderGlow
                    ) {
                        settingsStore.setSoundEnabled(it)
                        sounds.muted = !it
                        if (it) sounds.click()
                    }

                    Spacer(Modifier.height(10.dp))
                    PixelDivider()
                    Spacer(Modifier.height(10.dp))

                    ToggleRow(
                        icon = if (settings.musicEnabled) Icons.Filled.MusicNote else Icons.Filled.MusicOff,
                        title = "Фоновая музыка",
                        subtitle = "Тихая, не громче озвучки иероглифов",
                        checked = settings.musicEnabled,
                        accent = SkyAccent
                    ) {
                        settingsStore.setMusicEnabled(it)
                        music.enabled = it
                        if (it) sounds.click()
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Громкость музыки",
                            style = PixelType.chip,
                            color = TextSecondary
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${(settings.musicVolume * 100).toInt()}%",
                            style = PixelType.chip,
                            color = SkyAccent
                        )
                    }
                    Slider(
                        value = settings.musicVolume,
                        onValueChange = {
                            settingsStore.setMusicVolume(it)
                            music.volume = it
                        },
                        valueRange = 0f..1f,
                        enabled = settings.musicEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = SkyAccent,
                            activeTrackColor = SkyAccent,
                            inactiveTrackColor = CG.glass(0.14f)
                        )
                    )
                    Text(
                        text = "Пока звучит иероглиф, музыка приглушается автоматически",
                        style = PixelType.caption,
                        color = TextMuted
                    )
                }

                VSpace(24.dp)
                SectionTitle("Повторение слов")
                VSpace(10.dp)
                GlassCard(contentPadding = PaddingValues(16.dp)) {
                    ToggleRow(
                        icon = Icons.Filled.School,
                        title = "Сначала слова с низкой точностью",
                        subtitle = "Интервальное повторение: слабые слова раздаются первыми",
                        checked = settings.srsFirst,
                        accent = MintAccent
                    ) {
                        settingsStore.setSrsFirst(it)
                        sounds.click()
                    }
                }

                VSpace(24.dp)
                SectionTitle("Отображение слов")
                VSpace(10.dp)
                GlassCard(contentPadding = PaddingValues(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DisplayMode.entries.forEach { mode ->
                            SelectChip(
                                text = mode.shortTitle,
                                selected = settings.displayMode == mode,
                                onClick = {
                                    settingsStore.setDisplayMode(mode)
                                    sounds.click()
                                }
                            )
                        }
                    }
                    VSpace(10.dp)
                    Text(
                        text = settings.displayMode.hint,
                        style = PixelType.caption,
                        color = TextMuted
                    )
                    VSpace(14.dp)
                    ToggleRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = "Озвучка слов после ответа",
                        subtitle = "После верного и неверного ответа слово произносится вслух",
                        checked = settings.speakWords,
                        accent = SkyAccent
                    ) {
                        settingsStore.setSpeakWords(it)
                        sounds.click()
                    }
                }

                VSpace(24.dp)
                SectionTitle("Словарь в CSV")
                VSpace(10.dp)
                GlassCard(contentPadding = PaddingValues(16.dp)) {
                    Text(
                        text = "Экспорт сохраняет все папки и слова в файл CSV " +
                            "(разделитель «;», UTF-8 — открывается в Excel и Google Таблицах). " +
                            "Импорт добавляет слова, создавая папки по названиям и пропуская дубликаты.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    VSpace(14.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        GradientLikeButton(
                            text = "Экспорт",
                            icon = Icons.Filled.FileDownload,
                            modifier = Modifier.weight(1f)
                        ) {
                            sounds.click()
                            exportLauncher.launch("chinese-games-dictionary.csv")
                        }
                        GradientLikeButton(
                            text = "Импорт",
                            icon = Icons.Filled.FileUpload,
                            modifier = Modifier.weight(1f),
                            accent = SkyAccent
                        ) {
                            sounds.click()
                            importLauncher.launch(arrayOf("text/*", "text/csv", "text/plain"))
                        }
                    }
                    csvMessage?.let { message ->
                        VSpace(12.dp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PixelTag(text = "CSV", color = GoldAccent)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = message,
                                style = PixelType.caption,
                                color = TextSecondary
                            )
                        }
                    }
                    VSpace(12.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Сейчас в словаре: ${wordsLabel(totalWords)} в ${decksLabel(decks.size)}",
                            style = PixelType.caption,
                            color = TextMuted
                        )
                    }
                }

                VSpace(24.dp)
                SectionTitle("Озвучка")
                VSpace(10.dp)
                GlassCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelTag(
                            text = if (speaker.isAvailable) "TTS ГОТОВ" else "НЕТ КИТАЙСКОГО ГОЛОСА",
                            color = if (speaker.isAvailable) MintAccent else RoseAccent
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (speaker.isAvailable) {
                                "Иероглифы произносятся системным голосом"
                            } else {
                                "Поставьте китайский голос в настройках синтеза речи"
                            },
                            style = PixelType.caption,
                            color = TextSecondary
                        )
                    }
                }

                VSpace(24.dp)
                PixelLanternRow()
                VSpace(16.dp)
                PixelCatWisdom(
                    text = "Совет: повторяйте по 5 минут, но каждый день",
                    modifier = Modifier.fillMaxWidth()
                )
                VSpace(30.dp)
            }
        }
    }
}

@Composable
private fun ThemeCard(
    mode: ThemeMode,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val gradient = when (mode) {
        ThemeMode.NIGHT -> listOf(Color(0xFF2B1657), Color(0xFF6D28D9))
        ThemeMode.DAY -> listOf(Color(0xFFE7F0FF), Color(0xFFFFDCEC))
    }
    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(gradient))
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) GoldAccent else CG.glass(0.2f),
                shape
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (mode == ThemeMode.NIGHT) {
            Icon(
                imageVector = Icons.Filled.DarkMode,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.LightMode,
                contentDescription = null,
                tint = Color(0xFF7C3AED),
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = mode.title,
            style = PixelType.chip,
            color = if (mode == ThemeMode.DAY) Color(0xFF3B2465) else Color.White
        )
        Text(
            text = mode.emoji,
            fontSize = 16.sp,
            color = if (mode == ThemeMode.DAY) Color(0xFF3B2465) else Color.White
        )
    }
}

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = subtitle, style = PixelType.caption, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = VividPurple,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = CG.glass(0.12f)
            )
        )
    }
}

@Composable
private fun GradientLikeButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = VividPurple,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.7f))))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = PixelType.chip, color = Color.White)
    }
}

/* ---------------------------- Работа с файлами ---------------------------- */

private fun writeText(context: Context, uri: Uri, text: String): Boolean = try {
    context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
        stream.write(text.toByteArray(Charsets.UTF_8))
        stream.flush()
    }
    true
} catch (_: Throwable) {
    false
}

private fun readText(context: Context, uri: Uri): String? = try {
    context.contentResolver.openInputStream(uri)?.use { stream ->
        stream.bufferedReader(Charsets.UTF_8).readText()
    }
} catch (_: Throwable) {
    null
}
