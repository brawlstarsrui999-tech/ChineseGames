package com.chinesegames.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes
import com.chinesegames.app.R
import kotlin.random.Random

/**
 * Состояние Кловерушки: рядом выводится дружелюбный текст, а звучит не
 * человеческая фраза, а её собственная звериная мурр-трель/чирп.
 */
enum class MascotLine(val text: String, @RawRes val res: Int) {
    GREAT("Кловерушка радуется: «Отлично!»", R.raw.cloverushka_happy),
    GOOD("Кловерушка мурлычет: «Хороший результат!»", R.raw.cloverushka_proud),
    KEEP_UP("Кловерушка машет лапкой: «Так держать!»", R.raw.cloverushka_happy),
    PROUD("Кловерушка довольно мурлычет.", R.raw.cloverushka_proud),
    RECORD("Кловерушка прыгает от радости: новый рекорд!", R.raw.cloverushka_record),
    DONT_GIVE_UP("Кловерушка подбадривает тебя.", R.raw.cloverushka_comfort),
    TRY_AGAIN("Кловерушка нежно мурлычет: попробуем ещё раз.", R.raw.cloverushka_comfort),
    HELLO("Кловерушка приветствует тебя!", R.raw.cloverushka_hello);

    companion object {
        fun forAccuracy(accuracy: Float, stars: Int, rnd: Random = Random.Default): MascotLine = when {
            stars >= 3 && accuracy >= 0.97f -> if (rnd.nextBoolean()) GREAT else RECORD
            accuracy >= 0.9f -> if (rnd.nextBoolean()) GREAT else PROUD
            accuracy >= 0.7f -> if (rnd.nextBoolean()) GOOD else KEEP_UP
            accuracy >= 0.45f -> if (rnd.nextBoolean()) KEEP_UP else DONT_GIVE_UP
            else -> if (rnd.nextBoolean()) DONT_GIVE_UP else TRY_AGAIN
        }

        fun greeting(rnd: Random = Random.Default): MascotLine =
            listOf(HELLO, KEEP_UP, PROUD, GOOD, GREAT).random(rnd)
    }
}

/**
 * Голос Кловерушки — короткие синтезированные звериные звуки: мурр-трели,
 * радостные чирпы и мягкие переливы. Это намеренно не TTS: талисман остаётся
 * милой зверушкой, а текст реплики показывается в пузыре рядом с ней.
 */
class MascotVoice(
    context: Context,
    private val music: BackgroundMusic
) {
    private val appContext = context.applicationContext
    private var player: MediaPlayer? = null

    /** Слушатель начала/окончания звука — для анимации Кловерушки. */
    var onSpeakingChange: ((Boolean) -> Unit)? = null

    val isSpeaking: Boolean get() = player?.isPlaying == true

    fun say(line: MascotLine) {
        stop()
        val media = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                val descriptor = appContext.resources.openRawResourceFd(line.res)
                    ?: error("ресурс звука Кловерушки недоступен")
                descriptor.use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                prepare()
                setVolume(0.82f, 0.82f)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Звук Кловерушки недоступен", t)
            null
        }
        if (media == null) return

        media.setOnCompletionListener(::finish)
        media.setOnErrorListener { failed, _, _ ->
            finish(failed)
            true
        }
        player = media
        music.setDucked(true)
        onSpeakingChange?.invoke(true)
        try {
            media.start()
        } catch (_: Throwable) {
            finish(media)
        }
    }

    private fun finish(media: MediaPlayer) {
        try {
            media.release()
        } catch (_: Throwable) {
        }
        if (player === media) player = null
        music.setDucked(false)
        onSpeakingChange?.invoke(false)
    }

    fun stop() {
        player?.let { media ->
            try {
                if (media.isPlaying) media.stop()
            } catch (_: Throwable) {
            }
            finish(media)
        }
    }

    private companion object {
        const val TAG = "CloverushkaVoice"
    }
}
