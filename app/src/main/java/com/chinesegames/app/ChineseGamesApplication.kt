package com.chinesegames.app

import android.app.Application
import com.chinesegames.app.audio.BackgroundMusic
import com.chinesegames.app.audio.ChineseSpeaker
import com.chinesegames.app.audio.GameSounds
import com.chinesegames.app.data.AppDatabase
import com.chinesegames.app.data.DeckRepository
import com.chinesegames.app.data.SettingsStore

class ChineseGamesApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }

    val repository: DeckRepository by lazy {
        DeckRepository(database.deckDao(), database.wordDao(), database.statsDao(), database.favoriteDao())
    }

    /** Настройки (тема, звук, музыка). */
    val settings: SettingsStore by lazy { SettingsStore(this) }

    /** Звуковые эффекты (SoundPool). */
    val sounds: GameSounds by lazy { GameSounds(this) }

    /** Озвучка иероглифов (TTS). */
    val speaker: ChineseSpeaker by lazy { ChineseSpeaker(this) }

    /** Тихая фоновая музыка. */
    val music: BackgroundMusic by lazy { BackgroundMusic(this) }

    override fun onCreate() {
        super.onCreate()
        // Пока звучит иероглиф — музыка приглушается, голос всегда слышно.
        speaker.onSpeechStateChange = { speaking -> music.setDucked(speaking) }
        music.setDayTrack(isDay = !settings.settings.theme.isNight)
    }

    override fun onTerminate() {
        super.onTerminate()
        sounds.release()
        speaker.shutdown()
        music.release()
    }
}
