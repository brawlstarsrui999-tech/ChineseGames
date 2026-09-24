package com.chinesegames.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chinesegames.app.billing.PaymentSession
import com.chinesegames.app.billing.PurchaseManager
import com.chinesegames.app.data.Product
import com.chinesegames.app.ui.components.CgTopBar
import com.chinesegames.app.ui.components.GhostButton
import com.chinesegames.app.ui.components.GlassCard
import com.chinesegames.app.ui.components.GradientButton
import com.chinesegames.app.ui.components.PixelCatWisdom
import com.chinesegames.app.ui.components.PixelDivider
import com.chinesegames.app.ui.components.PixelSpriteView
import com.chinesegames.app.ui.components.PixelTag
import com.chinesegames.app.ui.components.PurpleBackground
import com.chinesegames.app.ui.components.SectionTitle
import com.chinesegames.app.ui.components.VSpace
import com.chinesegames.app.ui.components.DragonSprite
import com.chinesegames.app.ui.components.MascotSprite
import com.chinesegames.app.ui.components.SukunaSprite
import com.chinesegames.app.ui.components.WaguriSprite
import com.chinesegames.app.ui.shop.ShopViewModel
import com.chinesegames.app.ui.theme.CG
import com.chinesegames.app.ui.theme.GoldAccent
import com.chinesegames.app.ui.theme.LocalSounds
import com.chinesegames.app.ui.theme.MintAccent
import com.chinesegames.app.ui.theme.PixelType
import com.chinesegames.app.ui.theme.RoseAccent
import com.chinesegames.app.ui.theme.SkyAccent
import com.chinesegames.app.ui.theme.TextMuted
import com.chinesegames.app.ui.theme.TextPrimary
import com.chinesegames.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/** Порядок товаров на витрине. */
private val SHOWCASE = listOf(
    Product.STYLE_CHINA,
    Product.STYLE_ANIME,
    Product.COLOR_THEMES,
    Product.MASCOT,
    Product.CUSTOM_MUSIC,
    Product.NO_ADS
)

/**
 * Магазин украшений. Обучение полностью бесплатно; здесь только оформление,
 * талисман, своя музыка и отключение рекламы. Оплата — Robokassa.
 */
@Composable
fun ShopScreen(
    onBack: () -> Unit,
    onPay: (PaymentSession) -> Unit,
    onOpenPending: (Long) -> Unit,
    viewModel: ShopViewModel = viewModel()
) {
    val sounds = LocalSounds.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(4200)
            viewModel.clearMessage()
        }
    }

    PurpleBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            CgTopBar(
                title = "Магазин украшений",
                subtitle = "Обучение бесплатно навсегда",
                emoji = "🛍️",
                onBack = {
                    sounds.whoosh()
                    onBack()
                }
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                GlassCard(contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelTag(text = "ВАЖНО", color = GoldAccent)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Средства не возвращаются",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    VSpace(8.dp)
                    Text(
                        text = PurchaseManager.NON_REFUND_NOTICE,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    if (state.testMode) {
                        VSpace(8.dp)
                        Text(
                            text = "Тестовый режим Robokassa: платежи не списывают деньги.",
                            style = PixelType.caption,
                            color = SkyAccent
                        )
                    }
                    if (!state.available) {
                        VSpace(8.dp)
                        Text(
                            text = "Магазин ещё не подключён: добавьте ключи Robokassa в local.properties (см. docs/robokassa.md).",
                            style = PixelType.caption,
                            color = RoseAccent
                        )
                    }
                }

                VSpace(22.dp)
                SectionTitle("Что можно купить")
                VSpace(10.dp)

                SHOWCASE.forEach { product ->
                    val owned = product in state.owned
                    val pendingInvoice = state.pending.lastOrNull { it.product == product }
                    ProductCard(
                        product = product,
                        owned = owned,
                        busy = state.busyProduct == product,
                        hasPending = pendingInvoice != null && !owned,
                        enabled = state.available && state.busyProduct == null,
                        onBuy = {
                            sounds.click()
                            viewModel.buy(product) { session -> onPay(session) }
                        },
                        onCheckPending = {
                            sounds.click()
                            pendingInvoice?.let { onOpenPending(it.invId) }
                        }
                    )
                    VSpace(12.dp)
                }

                VSpace(8.dp)
                GhostButton(
                    text = if (state.restoring) "Проверяем…" else "Восстановить покупки",
                    icon = Icons.Filled.Refresh,
                    enabled = !state.restoring,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    sounds.click()
                    viewModel.restore()
                }
                VSpace(8.dp)
                Text(
                    text = "Покупки привязаны к устройству и к аккаунту Google (Настройки → Аккаунт): " +
                        "после входа они восстанавливаются на любом телефоне.",
                    style = PixelType.caption,
                    color = TextMuted
                )

                VSpace(20.dp)
                PixelCatWisdom(
                    text = "Спасибо, что поддерживаете бесплатное обучение!",
                    modifier = Modifier.fillMaxWidth()
                )
                VSpace(30.dp)
            }
        }

        AnimatedVisibility(
            visible = state.message != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(CG.surface))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = state.message.orEmpty(),
                    style = PixelType.caption,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    owned: Boolean,
    busy: Boolean,
    hasPending: Boolean,
    enabled: Boolean,
    onBuy: () -> Unit,
    onCheckPending: () -> Unit
) {
    GlassCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProductArt(product)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = product.description,
                    style = PixelType.caption,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            if (owned) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Куплено",
                    tint = MintAccent,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                PixelTag(text = product.priceLabel, color = GoldAccent)
            }
        }

        VSpace(12.dp)
        PixelDivider()
        VSpace(10.dp)

        product.perks.forEach { perk ->
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                Text(text = "✦", color = GoldAccent, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Text(text = perk, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        VSpace(14.dp)
        when {
            owned -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelTag(text = "КУПЛЕНО", color = MintAccent)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Включается в Настройках → Оформление",
                        style = PixelType.caption,
                        color = TextMuted
                    )
                }
            }
            busy -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = GoldAccent
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(text = "Выставляем счёт…", style = PixelType.caption, color = TextSecondary)
                }
            }
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradientButton(
                        text = "Купить за ${product.priceLabel}",
                        icon = Icons.Filled.ShoppingBag,
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                        colors = CG.goldGradient,
                        onClick = onBuy
                    )
                    if (hasPending) {
                        GhostButton(text = "Проверить", onClick = onCheckPending)
                    }
                }
                if (hasPending) {
                    VSpace(6.dp)
                    Text(
                        text = "Есть неподтверждённый счёт — нажмите «Проверить», если уже оплатили",
                        style = PixelType.caption,
                        color = SkyAccent
                    )
                }
            }
        }
    }
}

/** Пиксельная иллюстрация товара. */
@Composable
private fun ProductArt(product: Product) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(CG.primaryGradientDeep)),
        contentAlignment = Alignment.Center
    ) {
        when (product) {
            Product.STYLE_CHINA -> PixelSpriteView(sprite = DragonSprite, size = 16.dp)
            Product.STYLE_ANIME -> Row(verticalAlignment = Alignment.Bottom) {
                PixelSpriteView(sprite = WaguriSprite, size = 36.dp)
                Spacer(Modifier.width(2.dp))
                PixelSpriteView(sprite = SukunaSprite, size = 36.dp)
            }
            Product.MASCOT -> PixelSpriteView(sprite = MascotSprite, size = 40.dp)
            else -> Text(text = product.emoji, fontSize = 26.sp)
        }
    }
}
