package com.chinesegames.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.chinesegames.app.ui.navigation.ChineseGamesRoot
import com.chinesegames.app.ui.theme.ChineseGamesTheme
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.LocalSpeaker

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as ChineseGamesApplication

        setContent {
            ChineseGamesTheme {
                CompositionLocalProvider(
                    LocalSounds provides app.sounds,
                    LocalSpeaker provides app.speaker
                ) {
                    ChineseGamesRoot()
                }
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            (application as ChineseGamesApplication).speaker.stop()
        }
        super.onDestroy()
    }
}
