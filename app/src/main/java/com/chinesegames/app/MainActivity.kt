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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.chinesegames.app.ui.mascot.MascotOverlay
import com.chinesegames.app.ui.onboarding.FirstLaunchGuide
import com.chinesegames.app.ui.navigation.ChineseGamesRoot
import com.chinesegames.app.ui.theme.ChineseGamesTheme
import com.chinesegames.app.ui.theme.CgPaletteState
import com.chinesegames.app.ui.theme.LocalMusic
import com.chinesegames.app.ui.theme.LocalSettings
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.LocalSpeaker
import com.chinesegames.app.ui.theme.LocalThemeMode
import com.chinesegames.app.ui.theme.LocalPurchases
import com.chinesegames.app.ui.theme.LocalAccount

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as ChineseGamesApplication

        setContent {
            val settings by app.settings.state.collectAsState()
            val navController = rememberNavController()
            val systemDensity = LocalDensity.current
            // Значения sp во всём приложении масштабируются вместе с системным
            // размером текста и выбранной пользователем настройкой.
            val textDensity = Density(
                density = systemDensity.density,
                fontScale = systemDensity.fontScale * settings.fontSize.scale
            )

            // Тема применяется целиком: key() пересобирает интерфейс,
            // поэтому цвета читаются как обычные значения (см. Color.kt).
            CgPaletteState.night = settings.theme.isNight
            CgPaletteState.style = settings.colorStyle
            CgPaletteState.pack = settings.stylePack

            key(settings.theme, CgPaletteState.key) {
                CompositionLocalProvider(LocalDensity provides textDensity) {
                    ChineseGamesTheme {
                        CompositionLocalProvider(
                            LocalSounds provides app.sounds,
                            LocalSpeaker provides app.speaker,
                            LocalMusic provides app.music,
                            LocalSettings provides app.settings,
                            LocalThemeMode provides settings.theme,
                            LocalPurchases provides app.purchases,
                            LocalAccount provides app.account
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                ChineseGamesRoot(navController = navController)
                                MascotOverlay(
                                    settings = app.settings,
                                    purchases = app.purchases,
                                    voice = app.mascotVoice
                                )
                                FirstLaunchGuide(
                                    visible = !settings.onboardingCompleted,
                                    onFinish = { app.settings.setOnboardingCompleted(true) }
                                )
                            }
                        }
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
