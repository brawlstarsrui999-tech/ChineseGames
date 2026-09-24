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

/** Размер интерфейсного текста. Системный масштаб Android применяется дополнительно. */
enum class AppFontSize(val title: String, val scale: Float) {
    SMALL("Мелкий", 0.88f),
    MEDIUM("Средний", 1.00f),
    LARGE("Крупный", 1.18f);

    companion object {
        fun fromName(raw: String?): AppFontSize =
            entries.firstOrNull { it.name == raw } ?: MEDIUM
    }
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
    /** Три удобных размера для всего текста интерфейса. */
    val fontSize: AppFontSize = AppFontSize.MEDIUM,
    /** Показан ли стартовый гид на этом устройстве. */
    val onboardingCompleted: Boolean = false,
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
    val speakWords: Boolean = true,
    /** Цветовой стиль (фиолетовый бесплатный, остальные — покупка «Цветные стили»). */
    val colorStyle: ColorStyle = ColorStyle.PURPLE,
    /** Стилевой набор: классика, китайский дракон или розовый клевер. */
    val stylePack: StylePack = StylePack.CLASSIC,
    /** Своя фоновая музыка: content-URI выбранного файла (покупка «Своя музыка»). */
    val customMusicUri: String? = null,
    /** Название выбранного пользователем трека — только для показа в настройках. */
    val customMusicTitle: String? = null,
    /** Кловерушка-талисман показывается на экране (после покупки). */
    val mascotEnabled: Boolean = true,
    /** Угол, в котором сидит талисман. */
    val mascotCorner: MascotCorner = MascotCorner.BOTTOM_END
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
        fontSize = AppFontSize.fromName(prefs.getString(KEY_FONT_SIZE, null)),
        onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false),
        musicEnabled = prefs.getBoolean(KEY_MUSIC_ON, true),
        musicVolume = prefs.getFloat(KEY_MUSIC_VOLUME, 0.45f).coerceIn(0f, 1f),
        soundEnabled = !prefs.getBoolean(KEY_SOUND_MUTED, false),
        srsFirst = prefs.getBoolean(KEY_SRS_FIRST, true),
        displayMode = DisplayMode.fromName(prefs.getString(KEY_DISPLAY_MODE, DisplayMode.BOTH.name)),
        speakWords = prefs.getBoolean(KEY_SPEAK_WORDS, true),
        colorStyle = ColorStyle.fromName(prefs.getString(KEY_COLOR_STYLE, null)),
        stylePack = StylePack.fromName(prefs.getString(KEY_STYLE_PACK, null)),
        customMusicUri = prefs.getString(KEY_CUSTOM_MUSIC_URI, null)?.takeIf { it.isNotBlank() },
        customMusicTitle = prefs.getString(KEY_CUSTOM_MUSIC_TITLE, null)?.takeIf { it.isNotBlank() },
        mascotEnabled = prefs.getBoolean(KEY_MASCOT_ON, true),
        mascotCorner = MascotCorner.fromName(prefs.getString(KEY_MASCOT_CORNER, null))
    )

    private fun update(block: (AppSettings) -> AppSettings) {
        _state.value = block(_state.value)
    }

    fun setTheme(mode: ThemeMode) {
        prefs.edit().putBoolean(KEY_DAY_THEME, mode == ThemeMode.DAY).apply()
        update { it.copy(theme = mode) }
    }

    fun setFontSize(size: AppFontSize) {
        prefs.edit().putString(KEY_FONT_SIZE, size.name).apply()
        update { it.copy(fontSize = size) }
    }

    /** Завершить стартовое знакомство или показать его повторно из настроек. */
    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        update { it.copy(onboardingCompleted = completed) }
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

    /* ----------------------------- Украшения ----------------------------- */

    fun setColorStyle(style: ColorStyle) {
        prefs.edit().putString(KEY_COLOR_STYLE, style.name).apply()
        update { it.copy(colorStyle = style) }
    }

    /**
     * Выбор стилевого набора. Вместе с набором включается его «родной» цвет
     * (дракон — красный, клевер — розовый); классика возвращает фиолетовый.
     */
    fun setStylePack(pack: StylePack) {
        prefs.edit()
            .putString(KEY_STYLE_PACK, pack.name)
            .putString(KEY_COLOR_STYLE, pack.defaultColor.name)
            .apply()
        update { it.copy(stylePack = pack, colorStyle = pack.defaultColor) }
    }

    fun setCustomMusic(uri: String?, title: String?) {
        prefs.edit()
            .putString(KEY_CUSTOM_MUSIC_URI, uri ?: "")
            .putString(KEY_CUSTOM_MUSIC_TITLE, title ?: "")
            .apply()
        update { it.copy(customMusicUri = uri?.takeIf { u -> u.isNotBlank() }, customMusicTitle = title) }
    }

    fun setMascotEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MASCOT_ON, enabled).apply()
        update { it.copy(mascotEnabled = enabled) }
    }

    fun setMascotCorner(corner: MascotCorner) {
        prefs.edit().putString(KEY_MASCOT_CORNER, corner.name).apply()
        update { it.copy(mascotCorner = corner) }
    }

    /**
     * Снимок настроек для облачной синхронизации (см. `auth/CloudSync`).
     * Сохраняются только «вкусовые» настройки — их удобно переносить между устройствами.
     */
    fun exportForSync(): Map<String, Any?> = with(settings) {
        mapOf(
            "theme" to theme.name,
            "fontSize" to fontSize.name,
            "musicEnabled" to musicEnabled,
            "musicVolume" to musicVolume,
            "soundEnabled" to soundEnabled,
            "srsFirst" to srsFirst,
            "displayMode" to displayMode.name,
            "speakWords" to speakWords,
            "colorStyle" to colorStyle.name,
            "stylePack" to stylePack.name,
            "mascotEnabled" to mascotEnabled,
            "mascotCorner" to mascotCorner.name
        )
    }

    /** Применение настроек из облака (неизвестные ключи игнорируются). */
    fun importFromSync(values: Map<String, Any?>) {
        (values["theme"] as? String)?.let { name ->
            ThemeMode.entries.firstOrNull { it.name == name }?.let(::setTheme)
        }
        (values["fontSize"] as? String)?.let { setFontSize(AppFontSize.fromName(it)) }
        (values["musicEnabled"] as? Boolean)?.let(::setMusicEnabled)
        (values["musicVolume"] as? Number)?.let { setMusicVolume(it.toFloat()) }
        (values["soundEnabled"] as? Boolean)?.let(::setSoundEnabled)
        (values["srsFirst"] as? Boolean)?.let(::setSrsFirst)
        (values["displayMode"] as? String)?.let { setDisplayMode(DisplayMode.fromName(it)) }
        (values["speakWords"] as? Boolean)?.let(::setSpeakWords)
        (values["stylePack"] as? String)?.let { setStylePack(StylePack.fromName(it)) }
        (values["colorStyle"] as? String)?.let { setColorStyle(ColorStyle.fromName(it)) }
        (values["mascotEnabled"] as? Boolean)?.let(::setMascotEnabled)
        (values["mascotCorner"] as? String)?.let { setMascotCorner(MascotCorner.fromName(it)) }
    }

    companion object {
        const val PREFS = "chinese_games_prefs"
        const val KEY_SOUND_MUTED = "sound_muted"
        private const val KEY_DAY_THEME = "day_theme"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_MUSIC_ON = "music_enabled"
        private const val KEY_MUSIC_VOLUME = "music_volume"
        private const val KEY_SRS_FIRST = "srs_first"
        private const val KEY_DISPLAY_MODE = "display_mode"
        private const val KEY_SPEAK_WORDS = "speak_words"
        private const val KEY_COLOR_STYLE = "color_style"
        private const val KEY_STYLE_PACK = "style_pack"
        private const val KEY_CUSTOM_MUSIC_URI = "custom_music_uri"
        private const val KEY_CUSTOM_MUSIC_TITLE = "custom_music_title"
        private const val KEY_MASCOT_ON = "mascot_enabled"
        private const val KEY_MASCOT_CORNER = "mascot_corner"
    }
}
