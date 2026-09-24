package com.chinesegames.app

import android.app.Application
import com.chinesegames.app.audio.BackgroundMusic
import com.chinesegames.app.audio.ChineseSpeaker
import com.chinesegames.app.ads.Ads
import com.chinesegames.app.audio.GameSounds
import com.chinesegames.app.audio.MascotVoice
import com.chinesegames.app.auth.AccountManager
import com.chinesegames.app.auth.CloudSync
import com.chinesegames.app.billing.PurchaseManager
import com.chinesegames.app.data.AppDatabase
import com.chinesegames.app.data.DeckRepository
import com.chinesegames.app.data.GameEvents
import com.chinesegames.app.data.HskDeckSeeder
import com.chinesegames.app.data.Product
import com.chinesegames.app.data.SettingsStore
import com.chinesegames.app.data.StylePack
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
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

    /** Единый источник купленных косметических улучшений (Robokassa). */
    private val purchaseStore by lazy { com.chinesegames.app.data.PurchaseStore(this) }
    val purchases: PurchaseManager by lazy { PurchaseManager(purchaseStore) }

    /** Объединение прогресса/покупок в Firestore после входа через Google. */
    val cloudSync: CloudSync by lazy {
        CloudSync({ FirebaseFirestore.getInstance() }, database.hskDao(), purchaseStore, settings)
    }

    /** Firebase Google Sign-In. При отсутствии google-services.json остаётся «не настроен». */
    val account: AccountManager by lazy { AccountManager(this, cloudSync) }

    /** Голос чиби-талисмана: записанные реплики + запасной русский TTS. */
    val mascotVoice: MascotVoice by lazy { MascotVoice(this, speaker, music) }

    /** Область для разовых фоновых задач приложения. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Пока звучит иероглиф — музыка приглушается, голос всегда слышно.
        speaker.onSpeechStateChange = { speaking -> music.setDucked(speaking) }
        music.setDayTrack(isDay = !settings.settings.theme.isNight)
        music.setSource(
            settings.settings.stylePack,
            settings.settings.customMusicUri.takeIf { purchases.has(Product.CUSTOM_MUSIC) }
        )
        music.onCustomTrackFailed = { settings.setCustomMusic(null, null) }
        sounds.animeSet = settings.settings.stylePack == StylePack.ANIME
        Ads.init(this)

        // Оформление меняет палитру в UI, музыку и набор коротких звуков сразу.
        appScope.launch {
            settings.state.collect { value ->
                val customTrack = value.customMusicUri.takeIf { purchases.has(Product.CUSTOM_MUSIC) }
                music.setSource(value.stylePack, customTrack)
                sounds.animeSet = value.stylePack == StylePack.ANIME
            }
        }

        // При восстановлении покупки «Своя музыка» сразу снова пробуем
        // выбранный пользователем трек (до этого URI намеренно игнорируется).
        appScope.launch {
            purchases.owned.collect { owned ->
                val current = settings.settings
                music.setSource(
                    current.stylePack,
                    current.customMusicUri.takeIf { Product.CUSTOM_MUSIC in owned }
                )
            }
        }

        // После уже выполненного Google-входа покупки и итоги игр тихо
        // отправляются в облако. Ошибка сети не мешает игре: ручная кнопка
        // «Синхронизировать» остаётся в Настройках.
        appScope.launch {
            purchases.owned.drop(1).collect { account.syncNow() }
        }
        appScope.launch {
            GameEvents.finished.collect { account.syncNow() }
        }

        // Системная папка «Выученное» существует с первого запуска:
        // туда курс складывает выученные слова, удалять её нельзя.
        // Папки словаря «HSK 1» … «HSK 7» с разделами курса — тоже.
        appScope.launch {
            repository.learnedDeckId()
            runCatching {
                HskDeckSeeder(this@ChineseGamesApplication, database.deckDao(), database.wordDao()).sync()
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        sounds.release()
        speaker.shutdown()
        mascotVoice.stop()
        music.release()
    }
}
