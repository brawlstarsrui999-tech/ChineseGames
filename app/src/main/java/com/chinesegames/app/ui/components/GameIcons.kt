package com.chinesegames.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.South
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.chinesegames.app.ui.game.GameGroup
import com.chinesegames.app.ui.game.GameKind
import com.chinesegames.app.ui.theme.CgPaletteState

/* --------------------------------------------------------------------------
 *  Векторные иконки и «мягкие» цвета разделов.
 *
 *  Иконки — Material Symbols, а не эмодзи: они одинаково выглядят на любом
 *  устройстве и красятся в цвет раздела. Цвета подобраны пастельные,
 *  чтобы сочетаться и с ночной неоновой темой, и с дневной пастельной.
 * -------------------------------------------------------------------------- */

val GameKind.icon: ImageVector
    get() = when (this) {
        GameKind.MATCH -> Icons.Filled.Style
        GameKind.MEMORY_GRID -> Icons.Filled.GridView
        GameKind.MEMORY_CHAIN -> Icons.Filled.Psychology
        GameKind.BUBBLE -> Icons.Filled.WaterDrop
        GameKind.FALLING -> Icons.Filled.South
        GameKind.SPRINT -> Icons.Filled.Bolt
        GameKind.AUDIO_QUIZ -> Icons.Filled.Headphones
        GameKind.PINYIN -> Icons.Filled.Keyboard
        GameKind.TONES -> Icons.Filled.GraphicEq
        GameKind.HANDS_FREE -> Icons.Filled.Headset
    }

val GameGroup.icon: ImageVector
    get() = when (this) {
        GameGroup.MEMORY -> Icons.Filled.Psychology
        GameGroup.SPEED -> Icons.Filled.Bolt
        GameGroup.LISTENING -> Icons.Filled.Headphones
        GameGroup.WRITING -> Icons.Filled.Keyboard
        GameGroup.REVIEW -> Icons.Filled.Loop
    }

/** Основной цвет раздела: спокойный, но заметный на фоне. */
val GameGroup.accent: Color
    get() = when (this) {
        GameGroup.MEMORY -> if (CgPaletteState.night) Color(0xFFB39DFF) else Color(0xFF7C5CE0)
        GameGroup.SPEED -> if (CgPaletteState.night) Color(0xFFFFCB6B) else Color(0xFFC77A0A)
        GameGroup.LISTENING -> if (CgPaletteState.night) Color(0xFF7FD1FF) else Color(0xFF1E78C8)
        GameGroup.WRITING -> if (CgPaletteState.night) Color(0xFFFFA8D5) else Color(0xFFD6407F)
        GameGroup.REVIEW -> if (CgPaletteState.night) Color(0xFF9FE8C8) else Color(0xFF15803D)
    }

/** Второй цвет градиента раздела — чуть темнее/насыщеннее основного. */
val GameGroup.accentDeep: Color
    get() = when (this) {
        GameGroup.MEMORY -> Color(0xFF8B5CF6)
        GameGroup.SPEED -> Color(0xFFF59E0B)
        GameGroup.LISTENING -> Color(0xFF38BDF8)
        GameGroup.WRITING -> Color(0xFFF472B6)
        GameGroup.REVIEW -> Color(0xFF34D399)
    }

val GameGroup.softGradient: List<Color>
    get() = listOf(accent, accentDeep)

/** Полупрозрачная подложка карточки раздела — читается в обеих темах. */
val GameGroup.softSurface: Color
    get() = accent.copy(alpha = if (CgPaletteState.night) 0.16f else 0.14f)

val GameGroup.softBorder: Color
    get() = accent.copy(alpha = if (CgPaletteState.night) 0.40f else 0.45f)

/* ------------------------------ Навигация -------------------------------- */

enum class MainTab(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val accent: Color
) {
    HOME("menu", "Главная", Icons.Filled.Home, Color(0xFFB39DFF)),
    DECKS("decks", "Словарь", Icons.AutoMirrored.Filled.MenuBook, Color(0xFF7FD1FF)),
    GAMES("games", "Игры", Icons.Filled.SportsEsports, Color(0xFFFFCB6B)),
    STUDY("study", "Курс", Icons.Filled.School, Color(0xFF6EE7B7)),
    STATS("stats", "Прогресс", Icons.Filled.Insights, Color(0xFFFFA8D5)),
    SETTINGS("settings", "Ещё", Icons.Filled.Settings, Color(0xFFA5B4FC));

    companion object {
        fun fromRoute(route: String?): MainTab? = entries.firstOrNull { it.route == route }
    }
}

/** Цвет раздела «Поэтапное изучение» — мятный, спокойный. */
val StudyAccent: Color
    get() = if (CgPaletteState.night) Color(0xFF7FE3C0) else Color(0xFF0E9F77)

val StudyAccentDeep: Color
    get() = Color(0xFF34D399)

val StudyGradient: List<Color>
    get() = listOf(StudyAccent, StudyAccentDeep)

/** Цвет «предложений» внутри поэтапного изучения. */
val SentenceAccent: Color
    get() = if (CgPaletteState.night) Color(0xFFFFC48A) else Color(0xFFC2620C)

val SentenceAccentDeep: Color
    get() = Color(0xFFF97316)

val SentenceGradient: List<Color>
    get() = listOf(SentenceAccent, SentenceAccentDeep)
