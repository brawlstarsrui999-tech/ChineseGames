package com.chinesegames.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Тема оформления приложения. */
enum class ThemeMode(val title: String, val emoji: String) {
    NIGHT("Ночная", "🌙"),
    DAY("Дневная", "🌤");

    val isNight: Boolean get() = this == NIGHT
}

/**
 * Что показывать в играх: иероглиф и пиньинь, только иероглифы или только пиньинь.
 * Так можно учить китайский «без костылей»: кто-то запоминает сами 汉字,
 * а кто-то сначала тренирует слух и пиньинь. По умолчанию показывается всё.
 */
enum class DisplayMode(
    val title: String,
    val shortTitle: String,
    val hint: String
) {
    BOTH(
        title = "Иероглифы и пиньинь",
        shortTitle = "汉字 + pīnyīn",
        hint = "Слово показывается целиком: иероглиф и пиньинь под ним"
    ),
    HANZI_ONLY(
        title = "Только иероглифы",
        shortTitle = "汉字",
        hint = "Пиньинь спрятан — тренируем чтение самих иероглифов"
    ),
    PINYIN_ONLY(
        title = "Только пиньинь",
        shortTitle = "pīnyīn",
        hint = "Иероглифы спрятаны — работаем со звуком и пиньинем"
    );

    companion object {
        fun fromName(raw: String?): DisplayMode =
            entries.firstOrNull { it.name == raw } ?: BOTH
    }
}

/** Все настройки приложения, которые пользователь может менять. */
data class AppSettings(
    val theme: ThemeMode = ThemeMode.NIGHT,
    /** Тихая фоновая музыка. */
    val musicEnabled: Boolean = true,
    /** Громкость музыки 0..1 (по умолчанию заметно тише озвучки). */
    val musicVolume: Float = 0.45f,
    /** Звуковые эффекты игр. */
    val soundEnabled: Boolean = true,
    /** «Сначала слова с низкой точностью» при раздаче карточек. */
    val srsFirst: Boolean = true,
    /** Показывать иероглиф, пиньинь или что-то одно. */
    val displayMode: DisplayMode = DisplayMode.BOTH,
    /** Озвучивать слово после каждого ответа (верного и неверного). */
    val speakWords: Boolean = true
)

/**
 * Настройки в SharedPreferences + [StateFlow], чтобы интерфейс сразу
 * реагировал на переключатели. Файл и ключ звука те же, что у [com.chinesegames.app.audio.GameSounds],
 * поэтому кнопка звука работает одинаково из любого места.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    val settings: AppSettings get() = _state.value

    private fun read() = AppSettings(
        theme = if (prefs.getBoolean(KEY_DAY_THEME, false)) ThemeMode.DAY else ThemeMode.NIGHT,
        musicEnabled = prefs.getBoolean(KEY_MUSIC_ON, true),
        musicVolume = prefs.getFloat(KEY_MUSIC_VOLUME, 0.45f).coerceIn(0f, 1f),
        soundEnabled = !prefs.getBoolean(KEY_SOUND_MUTED, false),
        srsFirst = prefs.getBoolean(KEY_SRS_FIRST, true),
        displayMode = DisplayMode.fromName(prefs.getString(KEY_DISPLAY_MODE, DisplayMode.BOTH.name)),
        speakWords = prefs.getBoolean(KEY_SPEAK_WORDS, true)
    )

    private fun update(block: (AppSettings) -> AppSettings) {
        _state.value = block(_state.value)
    }

    fun setTheme(mode: ThemeMode) {
        prefs.edit().putBoolean(KEY_DAY_THEME, mode == ThemeMode.DAY).apply()
        update { it.copy(theme = mode) }
    }

    fun toggleTheme(): ThemeMode {
        val next = if (_state.value.theme.isNight) ThemeMode.DAY else ThemeMode.NIGHT
        setTheme(next)
        return next
    }

    fun setMusicEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MUSIC_ON, enabled).apply()
        update { it.copy(musicEnabled = enabled) }
    }

    fun setMusicVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        prefs.edit().putFloat(KEY_MUSIC_VOLUME, v).apply()
        update { it.copy(musicVolume = v) }
    }

    /** Звук идёт через тот же ключ, что читает [com.chinesegames.app.audio.GameSounds]. */
    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_MUTED, !enabled).apply()
        update { it.copy(soundEnabled = enabled) }
    }

    fun setSrsFirst(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SRS_FIRST, enabled).apply()
        update { it.copy(srsFirst = enabled) }
    }

    fun setDisplayMode(mode: DisplayMode) {
        prefs.edit().putString(KEY_DISPLAY_MODE, mode.name).apply()
        update { it.copy(displayMode = mode) }
    }

    fun setSpeakWords(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SPEAK_WORDS, enabled).apply()
        update { it.copy(speakWords = enabled) }
    }

    companion object {
        const val PREFS = "chinese_games_prefs"
        const val KEY_SOUND_MUTED = "sound_muted"
        private const val KEY_DAY_THEME = "day_theme"
        private const val KEY_MUSIC_ON = "music_enabled"
        private const val KEY_MUSIC_VOLUME = "music_volume"
        private const val KEY_SRS_FIRST = "srs_first"
        private const val KEY_DISPLAY_MODE = "display_mode"
        private const val KEY_SPEAK_WORDS = "speak_words"
    }
}
