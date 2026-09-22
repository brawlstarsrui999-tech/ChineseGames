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
            R.raw.star, R.raw.whoosh, R.raw.pop
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

    /*
     * Громкости намеренно невысокие: сэмплы сами по себе сделаны мягкими
     * (см. tools/generate_sounds.py), а здесь мы ещё и приглушаем их,
     * чтобы эффекты не «резали уши» при частом повторении.
     */

    /** Мягкий щелчок интерфейса. */
    fun click() = play(R.raw.click, 0.22f)

    /** Переворот карточки. */
    fun flip() = play(R.raw.flip, 0.36f)

    /** Пара найдена. */
    fun match() = play(R.raw.match, 0.5f)

    /** Комбо: с каждым уровнем тон чуть выше. */
    fun combo(level: Int) = play(R.raw.combo, 0.45f, 1f + (level.coerceIn(1, 6) - 1) * 0.05f)

    /** Ошибка. */
    fun error() = play(R.raw.error, 0.32f)

    /** Победная фанфара. */
    fun win() = play(R.raw.win, 0.55f)

    /** Звёздочка на экране победы. */
    fun star(index: Int) = play(R.raw.star, 0.4f, 1f + index * 0.1f)

    /** Переход между экранами. */
    fun whoosh() = play(R.raw.whoosh, 0.2f)

    /** Лопающийся пузырь (игра «Bubble pop»). */
    fun pop() = play(R.raw.pop, 0.4f)

    /** Тик обратного отсчёта/таймера. */
    fun tick() = play(R.raw.click, 0.14f, 1.2f)

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
