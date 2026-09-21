package com.chinesegames.app.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import com.chinesegames.app.R
import kotlin.math.abs

/**
 * Все звуки приложения — короткие синтезированные сэмплы из res/raw
 * (click, flip, match, combo, error, win, star, whoosh).
 * Проигрываются через SoundPool: без задержек и без нагрузки на UI.
 */
class GameSounds(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("chinese_games_prefs", Context.MODE_PRIVATE)

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val sampleIds = HashMap<Int, Int>()
    private val ready = HashSet<Int>()

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) ready.add(sampleId)
        }
        listOf(
            R.raw.click, R.raw.flip, R.raw.match,
            R.raw.combo, R.raw.error, R.raw.win,
            R.raw.star, R.raw.whoosh
        ).forEach { res ->
            sampleIds[res] = try {
                pool.load(context.applicationContext, res, 1)
            } catch (t: Throwable) {
                0
            }
        }
    }

    /** Звук включён? Настройка сохраняется между запусками. */
    var muted: Boolean
        get() = prefs.getBoolean(KEY_MUTED, false)
        set(value) = prefs.edit().putBoolean(KEY_MUTED, value).apply()

    private fun play(res: Int, volume: Float, rate: Float = 1f) {
        if (muted) return
        val id = sampleIds[res] ?: return
        if (id == 0 || !ready.contains(id)) return
        pool.play(id, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    /** Мягкий щелчок интерфейса. */
    fun click() = play(R.raw.click, 0.45f)

    /** Переворот карточки. */
    fun flip() = play(R.raw.flip, 0.7f)

    /** Пара найдена. */
    fun match() = play(R.raw.match, 0.85f)

    /** Комбо: с каждым уровнем тон чуть выше. */
    fun combo(level: Int) = play(R.raw.combo, 0.85f, 1f + (level.coerceIn(1, 6) - 1) * 0.07f)

    /** Ошибка. */
    fun error() = play(R.raw.error, 0.65f)

    /** Победная фанфара. */
    fun win() = play(R.raw.win, 0.9f)

    /** Звёздочка на экране победы. */
    fun star(index: Int) = play(R.raw.star, 0.8f, 1f + index * 0.14f)

    /** Переход между экранами. */
    fun whoosh() = play(R.raw.whoosh, 0.4f)

    /** Тик обратного отсчёта/таймера. */
    fun tick() = play(R.raw.click, 0.3f, 1.35f)

    fun release() {
        try {
            pool.release()
        } catch (_: Throwable) {
        }
    }

    private companion object {
        const val KEY_MUTED = "sound_muted"
    }
}

/** Мелочь для читаемости: «примерно равно» в статистике. */
internal fun Float.almostEquals(other: Float) = abs(this - other) < 0.0001f
