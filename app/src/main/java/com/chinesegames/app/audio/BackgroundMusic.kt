package com.chinesegames.app.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.chinesegames.app.R
import com.chinesegames.app.data.StylePack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Тихая фоновая музыка: бесшовно зацикленные темы (ночь/день).
 *
 * Музыка специально сделана тише звуковых эффектов и озвучки иероглифов:
 * сами треки сведены на −20 dBFS, а сверху есть регулятор громкости.
 * Пока звучит иероглиф (TTS), музыка приглушается — голос всегда слышно.
 *
 * Настройки живут в SharedPreferences и общие с [com.chinesegames.app.data.SettingsStore].
 */
class BackgroundMusic(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var player: MediaPlayer? = null
    private var fadeJob: Job? = null

    /** Какой трек сейчас нужен: ночная тема или дневная. */
    private var dayTrack = false
    private var prepared = false

    /** Стилевой набор: у «дракона» и «аниме» свои треки. */
    private var pack: StylePack = StylePack.CLASSIC

    /** Своя музыка пользователя (content-URI) — если куплена и выбрана. */
    private var customUri: Uri? = null

    /** Если свой трек не открылся — играем встроенный и сообщаем об этом. */
    var onCustomTrackFailed: (() -> Unit)? = null

    /** Приложение на переднем плане? */
    private var foreground = false

    /** Идёт озвучка иероглифа — музыку приглушаем. */
    private var ducked = false

    /** Громкость музыки 0..1 (настройка пользователя). */
    var volume: Float
        get() = prefs.getFloat(KEY_VOLUME, DEFAULT_VOLUME).coerceIn(0f, 1f)
        set(value) {
            val v = value.coerceIn(0f, 1f)
            prefs.edit().putFloat(KEY_VOLUME, v).apply()
            applyVolume(animated = true)
        }

    /** Музыка включена? */
    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLED, value).apply()
            if (value) {
                start()
            } else {
                fadeJob?.cancel()
                player?.let { if (it.isPlaying) it.pause() }
            }
        }

    /** Вызывается, когда приложение уходит в фон. */
    fun onPause() {
        foreground = false
        fadeJob?.cancel()
        player?.let { if (it.isPlaying) it.pause() }
    }

    /** Вызывается при возвращении на передний план. */
    fun onResume() {
        foreground = true
        start()
    }

    /** Переключение темы: ночная или дневная музыка. */
    fun setDayTrack(isDay: Boolean) {
        if (dayTrack == isDay) return
        dayTrack = isDay
        if (player != null) {
            release()
            if (foreground) start()
        }
    }

    /**
     * Смена источника: стилевой набор и/или свой трек пользователя.
     * Перезапускает музыку только если источник действительно изменился.
     */
    fun setSource(pack: StylePack, customUri: String?) {
        val uri = customUri?.takeIf { it.isNotBlank() }?.let { raw ->
            try {
                Uri.parse(raw)
            } catch (_: Throwable) {
                null
            }
        }
        if (this.pack == pack && this.customUri == uri) return
        this.pack = pack
        this.customUri = uri
        if (player != null) {
            release()
            if (foreground) start()
        }
    }

    /** Приглушение на время озвучки иероглифа (ducking). */
    fun setDucked(value: Boolean) {
        if (ducked == value) return
        ducked = value
        applyVolume(animated = true)
    }

    private fun start() {
        if (!enabled || !foreground) return
        val current = player
        if (current == null) {
            createPlayer()?.let { created ->
                player = created
                created.setVolume(0f, 0f)
                try {
                    created.start()
                } catch (t: Throwable) {
                    Log.w(TAG, "Не удалось запустить музыку", t)
                }
                applyVolume(animated = true)
            }
            return
        }
        if (!current.isPlaying) {
            try {
                current.start()
                applyVolume(animated = true)
            } catch (t: Throwable) {
                Log.w(TAG, "Не удалось продолжить музыку", t)
            }
        }
    }

    private fun createPlayer(): MediaPlayer? {
        customUri?.let { uri ->
            createFromUri(uri)?.let { return it }
            onCustomTrackFailed?.invoke()
        }
        val res = when (pack) {
            StylePack.CHINA -> R.raw.bgm_china
            StylePack.ANIME -> R.raw.bgm_anime
            StylePack.CLASSIC -> if (dayTrack) R.raw.bgm_day else R.raw.bgm_night
        }
        try {
            val media = newPlayer()
            val descriptor = appContext.resources.openRawResourceFd(res)
                ?: error("ресурс недоступен")
            descriptor.use {
                media.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            media.isLooping = true
            media.setVolume(0f, 0f)
            media.prepare()
            prepared = true
            return media
        } catch (t: Throwable) {
            Log.w(TAG, "Музыка недоступна", t)
            return try {
                MediaPlayer.create(appContext, res)?.also {
                    it.isLooping = true
                    it.setVolume(0f, 0f)
                    prepared = true
                }
            } catch (_: Throwable) {
                null
            }
        }
    }

    /** Свой трек пользователя: content-URI из SAF (разрешение сохранено при выборе). */
    private fun createFromUri(uri: Uri): MediaPlayer? = try {
        val media = newPlayer()
        media.setDataSource(appContext, uri)
        media.isLooping = true
        media.setVolume(0f, 0f)
        media.prepare()
        prepared = true
        media
    } catch (t: Throwable) {
        Log.w(TAG, "Свой трек недоступен: $uri", t)
        null
    }

    private fun newPlayer(): MediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
    }

    private fun targetVolume(): Float {
        if (!enabled || !prepared) return 0f
        val base = volume * MAX_MIX_VOLUME
        return if (ducked) base * DUCK_FACTOR else base
    }

    private fun applyVolume(animated: Boolean) {
        val media = player ?: return
        val target = targetVolume().coerceIn(0f, 1f)
        fadeJob?.cancel()
        if (!animated) {
            try {
                media.setVolume(target, target)
            } catch (_: Throwable) {
            }
            lastVolume = target
            return
        }
        fadeJob = scope.launch {
            val from = lastVolume
            val steps = 12
            for (i in 1..steps) {
                if (!isActive) break
                val v = from + (target - from) * (i.toFloat() / steps)
                try {
                    media.setVolume(v, v)
                } catch (_: Throwable) {
                    break
                }
                delay(28)
            }
            lastVolume = target
            try {
                media.setVolume(target, target)
            } catch (_: Throwable) {
            }
        }
    }

    private var lastVolume = 0f

    fun release() {
        fadeJob?.cancel()
        player?.let { media ->
            try {
                if (media.isPlaying) media.stop()
            } catch (_: Throwable) {
            }
            try {
                media.release()
            } catch (_: Throwable) {
            }
        }
        player = null
        prepared = false
        lastVolume = 0f
    }

    private companion object {
        const val TAG = "BackgroundMusic"

        /** Файл настроек и ключи — как в [com.chinesegames.app.data.SettingsStore]. */
        const val PREFS = "chinese_games_prefs"
        const val KEY_ENABLED = "music_enabled"
        const val KEY_VOLUME = "music_volume"

        /** Даже на максимуме музыка не громче озвучки. */
        const val MAX_MIX_VOLUME = 0.85f
        const val DEFAULT_VOLUME = 0.45f

        /** Во время иероглифа музыка уходит на треть громкости. */
        const val DUCK_FACTOR = 0.32f
    }
}
