package com.chinesegames.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.chinesegames.app.ui.navigation.ChineseGamesRoot
import com.chinesegames.app.ui.theme.ChineseGamesTheme
import com.chinesegames.app.ui.theme.CgPaletteState
import com.chinesegames.app.ui.theme.LocalMusic
import com.chinesegames.app.ui.theme.LocalSettings
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.LocalSpeaker
import com.chinesegames.app.ui.theme.LocalThemeMode

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as ChineseGamesApplication

        setContent {
            val settings by app.settings.state.collectAsState()
            val navController = rememberNavController()

            // Тема применяется целиком: key() пересобирает интерфейс,
            // поэтому цвета читаются как обычные значения (см. Color.kt).
            CgPaletteState.night = settings.theme.isNight

            key(settings.theme) {
                ChineseGamesTheme {
                    CompositionLocalProvider(
                        LocalSounds provides app.sounds,
                        LocalSpeaker provides app.speaker,
                        LocalMusic provides app.music,
                        LocalSettings provides app.settings,
                        LocalThemeMode provides settings.theme
                    ) {
                        ChineseGamesRoot(navController = navController)
                    }
                }
            }

            // Музыка играет только когда приложение на экране
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> app.music.onResume()
                        Lifecycle.Event.ON_STOP -> app.music.onPause()
                        Lifecycle.Event.ON_DESTROY -> app.speaker.stop()
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            val app = application as ChineseGamesApplication
            app.speaker.stop()
            app.music.release()
        }
        super.onDestroy()
    }
}
