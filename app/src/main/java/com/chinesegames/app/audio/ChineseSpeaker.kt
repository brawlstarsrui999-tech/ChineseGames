package com.chinesegames.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
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

    init {
        try {
            engine = TextToSpeech(context.applicationContext) { status ->
                initStatus = status
                if (status == TextToSpeech.SUCCESS) configureLanguage()
            }
        } catch (_: Throwable) {
            engine = null
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
    fun speak(text: String) {
        if (text.isBlank()) return
        if (!ensureReady()) return
        try {
            engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cg-${text.hashCode()}")
        } catch (_: Throwable) {
        }
    }

    fun stop() {
        try {
            engine?.stop()
        } catch (_: Throwable) {
        }
    }

    fun shutdown() {
        try {
            engine?.stop()
            engine?.shutdown()
        } catch (_: Throwable) {
        }
        engine = null
        languageReady = false
    }
}
