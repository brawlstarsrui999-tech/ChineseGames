package com.chinesegames.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes
import com.chinesegames.app.R
import kotlin.random.Random

/** Реплика талисмана: текст (для пузыря и запасной озвучки) и записанный голос. */
enum class MascotLine(val text: String, @RawRes val res: Int) {
    GREAT("Отлично!", R.raw.mascot_great),
    GOOD("Хороший результат, малыш!", R.raw.mascot_good),
    KEEP_UP("Ты молодец, так держать!", R.raw.mascot_keepup),
    PROUD("Я тобой горжусь!", R.raw.mascot_proud),
    RECORD("Ух ты, новый рекорд!", R.raw.mascot_record),
    DONT_GIVE_UP("Не сдавайся, у тебя всё получится!", R.raw.mascot_dontgiveup),
    TRY_AGAIN("Ничего страшного, попробуй ещё раз.", R.raw.mascot_again),
    HELLO("Привет! Давай поучим китайский?", R.raw.mascot_hello);

    companion object {
        /** Что сказать по итогам партии. */
        fun forAccuracy(accuracy: Float, stars: Int, rnd: Random = Random.Default): MascotLine = when {
            stars >= 3 && accuracy >= 0.97f -> if (rnd.nextBoolean()) GREAT else RECORD
            accuracy >= 0.9f -> if (rnd.nextBoolean()) GREAT else PROUD
            accuracy >= 0.7f -> if (rnd.nextBoolean()) GOOD else KEEP_UP
            accuracy >= 0.45f -> if (rnd.nextBoolean()) KEEP_UP else DONT_GIVE_UP
            else -> if (rnd.nextBoolean()) DONT_GIVE_UP else TRY_AGAIN
        }

        /** Реплика на касание. */
        fun greeting(rnd: Random = Random.Default): MascotLine =
            listOf(HELLO, KEEP_UP, PROUD, GOOD, GREAT).random(rnd)
    }
}

/**
 * Голос чиби-талисмана: записанные фразы из res/raw (сгенерированы нейросетевым
 * голосом, см. README), с запасным вариантом — русский системный TTS.
 * Пока талисман говорит, музыка приглушается, как и при озвучке иероглифов.
 */
class MascotVoice(
    context: Context,
    private val speaker: ChineseSpeaker,
    private val music: BackgroundMusic
) {
    private val appContext = context.applicationContext
    private var player: MediaPlayer? = null

    /** Слушатель начала/окончания реплики — для анимации талисмана. */
    var onSpeakingChange: ((Boolean) -> Unit)? = null

    val isSpeaking: Boolean get() = player?.isPlaying == true

    fun say(line: MascotLine) {
        stop()
        val media = try {
            MediaPlayer().apply {
                // Атрибуты нужно задать ДО prepare(), поэтому не используем
                // MediaPlayer.create() (она возвращает уже подготовленный объект).
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                val descriptor = appContext.resources.openRawResourceFd(line.res)
                    ?: error("ресурс реплики недоступен")
                descriptor.use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                prepare()
                setVolume(1f, 1f)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Голос талисмана недоступен", t)
            null
        }
        if (media == null) {
            // Запасной вариант: системный русский голос.
            speaker.speakRussian(line.text)
            return
        }
        media.setOnCompletionListener {
            finish(it)
        }
        media.setOnErrorListener { mp, _, _ ->
            finish(mp)
            true
        }
        player = media
        music.setDucked(true)
        onSpeakingChange?.invoke(true)
        try {
            media.start()
        } catch (t: Throwable) {
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
        const val TAG = "MascotVoice"
    }
}
