package com.chinesegames.app.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chinesegames.app.ChineseGamesApplication
import com.chinesegames.app.billing.ConfirmResult
import com.chinesegames.app.billing.RobokassaConfig
import com.chinesegames.app.ui.components.CgTopBar
import com.chinesegames.app.ui.components.GhostButton
import com.chinesegames.app.ui.components.GlassCard
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.PixelTag
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.VSpace
import com.chinesegames.app.ui.shop.ShopViewModel
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.PixelType
import com.chinesegames.app.ui.theme.RoseAccent
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary

/**
 * Оплата счёта на странице Robokassa внутри WebView.
 *
 * Когда Robokassa перенаправляет на Success URL / Fail URL (адреса из
 * личного кабинета, см. [RobokassaConfig]), приложение перехватывает переход,
 * проверяет статус счёта через OpStateExt и открывает товар.
 * Кнопка «Я оплатил(а)» — на случай, если возврат на адрес не сработал.
 */
@Composable
fun PaymentScreen(
    invId: Long,
    initialUrl: String?,
    onDone: () -> Unit,
    viewModel: ShopViewModel = viewModel()
) {
    val sounds = LocalSounds.current
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val invoice = remember(invId, state.pending) { state.pending.firstOrNull { it.invId == invId } }

    var url by remember { mutableStateOf(initialUrl) }
    var loading by remember { mutableStateOf(true) }
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ConfirmResult?>(null) }
    var redirected by remember { mutableStateOf<Boolean?>(null) }

    // Ссылка могла не приехать через навигацию (например, после поворота) — пересобираем.
    LaunchedEffect(invoice) {
        if (url == null && invoice != null) {
            val manager = (context.applicationContext as ChineseGamesApplication).purchases
            url = runCatching { manager.paymentUrl(invoice) }.getOrNull()
        }
    }

    fun check() {
        if (checking || result is ConfirmResult.Granted) return
        checking = true
        viewModel.confirm(invId) { outcome ->
            checking = false
            result = outcome
            if (outcome is ConfirmResult.Granted) sounds.win()
        }
    }

    // Как только Robokassa вернула на Success URL — проверяем оплату сами.
    LaunchedEffect(redirected) {
        if (redirected == true) check()
    }

    PurpleBackground(petals = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            CgTopBar(
                title = invoice?.product?.title ?: "Оплата",
                subtitle = invoice?.let { "Счёт № ${it.invId} · ${it.product.priceLabel}" } ?: "Robokassa",
                emoji = "💳",
                onBack = {
                    sounds.whoosh()
                    onDone()
                }
            )

            when {
                result is ConfirmResult.Granted -> ResultPanel(
                    title = "Готово!",
                    message = "«${(result as ConfirmResult.Granted).product.title}» открыто. " +
                        "Включить его можно в Настройках → Оформление.",
                    accent = MintAccent,
                    onDone = onDone
                )

                redirected == false && result == null -> ResultPanel(
                    title = "Оплата не завершена",
                    message = "Robokassa сообщила об отказе или отмене. Если вы всё же оплатили — нажмите «Проверить оплату».",
                    accent = RoseAccent,
                    onDone = onDone,
                    onRetry = { check() }
                )

                invoice == null -> ResultPanel(
                    title = "Счёт не найден",
                    message = "Этот счёт уже закрыт. Если покупка не появилась — воспользуйтесь «Восстановить покупки» в магазине.",
                    accent = RoseAccent,
                    onDone = onDone
                )

                else -> {
                    if (loading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(3.dp),
                            color = GoldAccent,
                            trackColor = CG.track
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        val current = url
                        if (current == null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = GoldAccent)
                            }
                        } else {
                            RobokassaWebView(
                                url = current,
                                onLoading = { loading = it },
                                onSuccessRedirect = { redirected = true },
                                onFailRedirect = { redirected = false }
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        result?.let { outcome ->
                            if (outcome !is ConfirmResult.Granted) {
                                Text(
                                    text = when (outcome) {
                                        is ConfirmResult.Pending -> "Оплата пока не подтверждена — попробуйте проверить через минуту."
                                        is ConfirmResult.Failed -> "Платёж не прошёл: ${outcome.state.title.lowercase()}."
                                        is ConfirmResult.Error -> "Не удалось проверить: ${outcome.message}."
                                        else -> ""
                                    },
                                    style = PixelType.caption,
                                    color = TextSecondary
                                )
                                VSpace(8.dp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GradientButton(
                                text = if (checking) "Проверяем…" else "Я оплатил(а) — проверить",
                                icon = Icons.Filled.Verified,
                                modifier = Modifier.weight(1f),
                                enabled = !checking,
                                colors = CG.goldGradient
                            ) {
                                sounds.click()
                                check()
                            }
                            GhostButton(text = "Позже") {
                                sounds.whoosh()
                                onDone()
                            }
                        }
                        VSpace(4.dp)
                        Text(
                            text = "Средства не возвращаются. Счёт сохранится — проверить оплату можно позже в магазине.",
                            style = PixelType.caption,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultPanel(
    title: String,
    message: String,
    accent: androidx.compose.ui.graphics.Color,
    onDone: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    Column(modifier = Modifier.padding(16.dp)) {
        GlassCard(contentPadding = PaddingValues(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelTag(text = if (accent == MintAccent) "УСПЕХ" else "ВНИМАНИЕ", color = accent)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            VSpace(10.dp)
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            VSpace(16.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradientButton(text = "В магазин", modifier = Modifier.weight(1f), onClick = onDone)
                if (onRetry != null) {
                    GhostButton(text = "Проверить оплату", onClick = onRetry)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RobokassaWebView(
    url: String,
    onLoading: (Boolean) -> Unit,
    onSuccessRedirect: () -> Unit,
    onFailRedirect: () -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val target = request?.url?.toString() ?: return false
                        return handleRedirect(target)
                    }

                    override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                        onLoading(true)
                        pageUrl?.let { handleRedirect(it) }
                    }

                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                        onLoading(false)
                    }

                    private fun handleRedirect(target: String): Boolean = when {
                        target.startsWith(RobokassaConfig.SUCCESS_URL, ignoreCase = true) -> {
                            onSuccessRedirect()
                            true
                        }
                        target.startsWith(RobokassaConfig.FAIL_URL, ignoreCase = true) -> {
                            onFailRedirect()
                            true
                        }
                        else -> false
                    }
                }
                loadUrl(url)
            }
        },
        update = { view ->
            if (view.url == null) view.loadUrl(url)
        }
    )
}
