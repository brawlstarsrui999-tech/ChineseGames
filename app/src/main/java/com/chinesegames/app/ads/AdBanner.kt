package com.chinesegames.app.ads

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.chinesegames.app.BuildConfig
import com.chinesegames.app.data.Product
import com.chinesegames.app.ui.theme.LocalPurchases
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Реклама (Google AdMob): один скромный баннер на спокойных экранах.
 *
 *  * Внутри игр и курса рекламы нет — ничто не отвлекает от учёбы.
 *  * После покупки «Без рекламы» баннер исчезает навсегда.
 *  * Идентификаторы берутся из BuildConfig; по умолчанию — тестовые ID Google,
 *    боевые задаются в local.properties (см. docs/robokassa.md, раздел «Реклама»).
 */
object Ads {
    private val initialized = AtomicBoolean(false)

    /** Инициализация SDK — один раз, в фоне, чтобы не тормозить запуск. */
    fun init(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        Thread {
            try {
                MobileAds.initialize(context.applicationContext) {}
            } catch (t: Throwable) {
                Log.w(TAG, "AdMob не инициализирован", t)
            }
        }.start()
    }

    val bannerUnitId: String get() = BuildConfig.ADMOB_BANNER_ID.ifBlank { TEST_BANNER_ID }

    /** Тестовый баннер Google — безопасен для разработки, за клики не банят. */
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"

    private const val TAG = "Ads"
}

/**
 * Баннер AdMob. Ничего не рисует, если куплено «Без рекламы».
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val purchases = LocalPurchases.current
    val owned by purchases.owned.collectAsState()
    if (Product.NO_ADS in owned) return

    val context = LocalContext.current
    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = Ads.bannerUnitId
        }
    }

    DisposableEffect(adView) {
        Ads.init(context)
        try {
            adView.loadAd(AdRequest.Builder().build())
        } catch (t: Throwable) {
            Log.w("Ads", "Баннер не загрузился", t)
        }
        onDispose {
            try {
                adView.destroy()
            } catch (_: Throwable) {
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(factory = { adView })
    }
}
