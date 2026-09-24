package com.chinesegames.app.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Озвучка иероглифов через системный TTS (например, Google Речевые сервисы
 * с установленным китайским голосом).
 *
 * Если китайского голоса в системе нет — приложение просто работает без озвучки,
 * ничего не падает.
 */
class ChineseSpeaker(context: Context) {

    private var engine: TextToSpeech? = null
    private var initStatus = TextToSpeech.ERROR

    @Volatile
    private var languageReady = false

    /**
     * Уведомление о начале/конце речи: нужно, чтобы фоновая музыка
     * приглушалась, пока звучит иероглиф.
     */
    var onSpeechStateChange: ((Boolean) -> Unit)? = null

    init {
        try {
            engine = TextToSpeech(context.applicationContext) { status ->
                initStatus = status
                if (status == TextToSpeech.SUCCESS) {
                    configureLanguage()
                    installListener()
                }
            }
        } catch (_: Throwable) {
            engine = null
        }
    }

    private fun installListener() {
        try {
            engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = notifySpeech(true)

                override fun onDone(utteranceId: String?) = notifySpeech(false)

                @Deprecated("Оставлено для старых движков TTS")
                override fun onError(utteranceId: String?) = notifySpeech(false)

                override fun onError(utteranceId: String?, errorCode: Int) = notifySpeech(false)

                override fun onStop(utteranceId: String?, interrupted: Boolean) = notifySpeech(false)
            })
        } catch (_: Throwable) {
        }
    }

    private fun notifySpeech(active: Boolean) {
        try {
            onSpeechStateChange?.invoke(active)
        } catch (_: Throwable) {
        }
    }

    private fun configureLanguage() {
        val tts = engine ?: return
        languageReady = try {
            val result = tts.setLanguage(Locale.SIMPLIFIED_CHINESE)
            result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        } catch (_: Throwable) {
            false
        }
    }

    /** Готов ли движок (заодно лениво донастраиваем язык). */
    fun ensureReady(): Boolean {
        if (languageReady) return true
        if (initStatus != TextToSpeech.SUCCESS) return false
        configureLanguage()
        return languageReady
    }

    val isAvailable: Boolean get() = ensureReady()

    /** Произнести иероглиф. */
    fun speak(text: String, rate: Float = 1f) {
        if (text.isBlank()) return
        if (!ensureReady()) return
        try {
            engine?.setSpeechRate(rate.coerceIn(0.5f, 1.5f))
            engine?.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "cg-${text.hashCode()}")
            notifySpeech(true)
        } catch (_: Throwable) {
        }
    }

    /* ------------------------- Русская озвучка ------------------------- */

    @Volatile
    private var russianReady: Boolean? = null

    /** Есть ли в системе русский голос (проверяем один раз, лениво). */
    val isRussianAvailable: Boolean
        get() {
            russianReady?.let { return it }
            if (initStatus != TextToSpeech.SUCCESS) return false
            val tts = engine ?: return false
            val ready = try {
                val result = tts.isLanguageAvailable(RUSSIAN)
                result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            } catch (_: Throwable) {
                false
            }
            russianReady = ready
            return ready
        }

    /**
     * Произнести перевод по-русски (режим «Без рук»). Язык выставляется на время
     * одной фразы и сразу возвращается на китайский: TTS запоминает язык в момент
     * постановки фразы в очередь, поэтому следующие иероглифы звучат правильно.
     */
    fun speakRussian(text: String, rate: Float = 1f, queue: Boolean = false) {
        if (text.isBlank() || !isRussianAvailable) return
        val tts = engine ?: return
        try {
            tts.setSpeechRate(rate.coerceIn(0.5f, 1.5f))
            tts.setLanguage(RUSSIAN)
            tts.speak(
                text,
                if (queue) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH,
                Bundle(),
                "cg-ru-${text.hashCode()}"
            )
            tts.setLanguage(Locale.SIMPLIFIED_CHINESE)
            notifySpeech(true)
        } catch (_: Throwable) {
            languageReady = false
        }
    }

    /** Иероглиф, пауза, затем перевод — одной очередью. */
    fun speakPair(chinese: String, russian: String, rate: Float = 1f, gapMillis: Long = 500L) {
        if (chinese.isBlank()) {
            speakRussian(russian, rate)
            return
        }
        if (!ensureReady()) return
        val tts = engine ?: return
        try {
            tts.setSpeechRate(rate.coerceIn(0.5f, 1.5f))
            tts.speak(chinese, TextToSpeech.QUEUE_FLUSH, Bundle(), "cg-${chinese.hashCode()}")
            if (russian.isNotBlank() && isRussianAvailable) {
                tts.playSilentUtterance(gapMillis, TextToSpeech.QUEUE_ADD, "cg-gap")
                speakRussian(russian, rate, queue = true)
            }
            notifySpeech(true)
        } catch (_: Throwable) {
        }
    }

    fun stop() {
        try {
            engine?.stop()
        } catch (_: Throwable) {
        }
        notifySpeech(false)
    }

    private companion object {
        val RUSSIAN: Locale = Locale("ru", "RU")
    }

    fun shutdown() {
        try {
            engine?.stop()
            engine?.shutdown()
        } catch (_: Throwable) {
        }
        engine = null
        languageReady = false
        notifySpeech(false)
    }
}
