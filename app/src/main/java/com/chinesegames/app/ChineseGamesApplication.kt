package com.chinesegames.app

import android.app.Application
import com.chinesegames.app.audio.BackgroundMusic
import com.chinesegames.app.audio.ChineseSpeaker
import com.chinesegames.app.audio.GameSounds
import com.chinesegames.app.data.AppDatabase
import com.chinesegames.app.data.DeckRepository
import com.chinesegames.app.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ChineseGamesApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }

    val repository: DeckRepository by lazy {
        DeckRepository(
            database.deckDao(),
            database.wordDao(),
            database.statsDao(),
            database.favoriteDao(),
            database.hskDao()
        )
    }

    /** Настройки (тема, звук, музыка). */
    val settings: SettingsStore by lazy { SettingsStore(this) }

    /** Звуковые эффекты (SoundPool). */
    val sounds: GameSounds by lazy { GameSounds(this) }

    /** Озвучка иероглифов (TTS). */
    val speaker: ChineseSpeaker by lazy { ChineseSpeaker(this) }

    /** Тихая фоновая музыка. */
    val music: BackgroundMusic by lazy { BackgroundMusic(this) }

    /** Область для разовых фоновых задач приложения. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Пока звучит иероглиф — музыка приглушается, голос всегда слышно.
        speaker.onSpeechStateChange = { speaking -> music.setDucked(speaking) }
        music.setDayTrack(isDay = !settings.settings.theme.isNight)

        // Системная папка «Выученное» существует с первого запуска:
        // туда курс складывает выученные слова, удалять её нельзя.
        appScope.launch { repository.learnedDeckId() }
    }

    override fun onTerminate() {
        super.onTerminate()
        sounds.release()
        speaker.shutdown()
        music.release()
    }
}
