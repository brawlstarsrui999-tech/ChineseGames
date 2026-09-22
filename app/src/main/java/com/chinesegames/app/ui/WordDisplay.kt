package com.chinesegames.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.chinesegames.app.data.AppSettings
import com.chinesegames.app.data.DisplayMode
import com.chinesegames.app.data.Word
import com.chinesegames.app.ui.theme.LocalSettings

/**
 * Как показывать слово при выбранном режиме отображения.
 *
 * Режим выбирается в настройках или прямо на экране настройки партии:
 * «汉字 + pīnyīn» (по умолчанию), «только иероглифы» и «только пиньинь» —
 * так можно учить китайский без подсказок, которые мешают запоминать.
 */
object WordDisplay {

    /** Основной текст карточки или варианта ответа. */
    fun main(word: Word, mode: DisplayMode): String =
        main(word.hanzi, word.pinyin, mode)

    /** Основной текст, когда иероглиф и пиньинь лежат отдельно. */
    fun main(hanzi: String, pinyin: String, mode: DisplayMode): String = when {
        mode == DisplayMode.PINYIN_ONLY && pinyin.isNotBlank() -> pinyin
        else -> hanzi
    }

    /** Подпись под основным текстом (пиньинь под иероглифом). */
    fun sub(word: Word, mode: DisplayMode): String? = sub(word.hanzi, word.pinyin, mode)

    fun sub(hanzi: String, pinyin: String, mode: DisplayMode): String? = when {
        mode != DisplayMode.BOTH -> null
        pinyin.isBlank() -> null
        pinyin == hanzi -> null
        else -> pinyin
    }

    /** Показывать ли пиньинь рядом с иероглифом. */
    fun showsPinyin(mode: DisplayMode): Boolean = mode == DisplayMode.BOTH

    /** Показывать ли сам иероглиф. */
    fun showsHanzi(mode: DisplayMode): Boolean = mode != DisplayMode.PINYIN_ONLY
}

/**
 * Текущие настройки приложения внутри композиции. Именно `collectAsState`,
 * а не `settings.settings`: экран сразу перерисовывается, когда пользователь
 * переключил режим отображения или озвучку.
 */
@Composable
fun rememberAppSettings(): AppSettings {
    val store = LocalSettings.current
    val settings by store.state.collectAsState()
    return settings
}
