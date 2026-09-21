package com.chinesegames.app

import android.app.Application
import com.chinesegames.app.audio.ChineseSpeaker
import com.chinesegames.app.audio.GameSounds
import com.chinesegames.app.data.AppDatabase
import com.chinesegames.app.data.DeckRepository

class ChineseGamesApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }

    val repository: DeckRepository by lazy {
        DeckRepository(database.deckDao(), database.wordDao(), database.statsDao())
    }

    /** Звуковые эффекты (SoundPool). */
    val sounds: GameSounds by lazy { GameSounds(this) }

    /** Озвучка иероглифов (TTS). */
    val speaker: ChineseSpeaker by lazy { ChineseSpeaker(this) }

    override fun onTerminate() {
        super.onTerminate()
        sounds.release()
        speaker.shutdown()
    }
}
