package com.chinesegames.app.ui.shop

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chinesegames.app.ChineseGamesApplication
import com.chinesegames.app.billing.ConfirmResult
import com.chinesegames.app.billing.PaymentSession
import com.chinesegames.app.billing.PurchaseManager
import com.chinesegames.app.data.PendingInvoice
import com.chinesegames.app.data.Product
import com.chinesegames.app.data.PromoRedemption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShopUiState(
    val owned: Set<Product> = emptySet(),
    val pending: List<PendingInvoice> = emptyList(),
    /** Товар, для которого сейчас выставляется счёт. */
    val busyProduct: Product? = null,
    /** Идёт «Восстановить покупки». */
    val restoring: Boolean = false,
    /** Сообщение-тост внизу экрана. */
    val message: String? = null,
    val available: Boolean = false
)

/** Магазин украшений: покупка, подтверждение и восстановление. */
class ShopViewModel(app: Application) : AndroidViewModel(app) {

    private val manager: PurchaseManager = getApplication<ChineseGamesApplication>().purchases

    private val _state = MutableStateFlow(
        ShopUiState(
            owned = manager.owned.value,
            pending = manager.pending.value,
            available = manager.isAvailable
        )
    )
    val state: StateFlow<ShopUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            manager.owned.collect { owned -> _state.update { it.copy(owned = owned) } }
        }
        viewModelScope.launch {
            manager.pending.collect { pending -> _state.update { it.copy(pending = pending) } }
        }
    }

    /** Выставить счёт; при успехе открывается экран оплаты. */
    fun buy(product: Product, onSession: (PaymentSession) -> Unit) {
        if (_state.value.busyProduct != null) return
        if (manager.has(product)) {
            _state.update { it.copy(message = "«${product.title}» уже куплено") }
            return
        }
        if (!manager.isAvailable) {
            _state.update { it.copy(message = "Оплата сейчас недоступна. Попробуйте позже.") }
            return
        }
        _state.update { it.copy(busyProduct = product, message = null) }
        viewModelScope.launch {
            try {
                val session = manager.begin(product)
                _state.update { it.copy(busyProduct = null) }
                onSession(session)
            } catch (_: Throwable) {
                _state.update {
                    it.copy(busyProduct = null, message = "Не удалось выставить счёт. Проверьте подключение и попробуйте ещё раз.")
                }
            }
        }
    }

    /** Проверить конкретный счёт (после возврата с оплаты или по кнопке). */
    fun confirm(invId: Long, onResult: (ConfirmResult) -> Unit) {
        val invoice = manager.pendingInvoice(invId) ?: run {
            // Экран оплаты мог быть открыт после того, как счёт уже закрыт.
            // Не подменяем его «какой-нибудь» другой купленной вещью.
            onResult(ConfirmResult.Error(Product.NO_ADS, "Счёт не найден"))
            return
        }
        viewModelScope.launch {
            val result = manager.confirm(invoice)
            _state.update { it.copy(message = messageFor(result)) }
            onResult(result)
        }
    }

    fun cancel(invId: Long) {
        manager.pendingInvoice(invId)?.let { manager.cancel(it) }
    }

    /** Перепроверить все ожидающие счета. */
    fun restore() {
        if (_state.value.restoring) return
        _state.update { it.copy(restoring = true, message = null) }
        viewModelScope.launch {
            val results = manager.restorePending()
            val granted = results.count { it is ConfirmResult.Granted }
            val message = when {
                results.isEmpty() -> "Нет счетов, ожидающих подтверждения"
                granted > 0 -> "Восстановлено покупок: $granted"
                else -> results.firstOrNull()?.let { messageFor(it) } ?: "Ничего не изменилось"
            }
            _state.update { it.copy(restoring = false, message = message) }
        }
    }

    /** Промокод — отдельный от Robokassa тестовый путь, без выставления счёта. */
    fun redeemPromo(raw: String) {
        when (val result = manager.redeemPromo(raw)) {
            is PromoRedemption.Granted -> _state.update {
                it.copy(message = "Промокод принят: «${result.product.title}» открыто")
            }
            PromoRedemption.AlreadyUsed -> _state.update { it.copy(message = "Этот промокод уже был использован") }
            PromoRedemption.Invalid -> _state.update { it.copy(message = "Промокод не найден") }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun messageFor(result: ConfirmResult): String = when (result) {
        is ConfirmResult.Granted -> "«${result.product.title}» открыто. Спасибо! 💜"
        is ConfirmResult.Pending -> "Оплата «${result.product.title}» ещё не подтверждена" +
            (result.state?.let { " (${it.title.lowercase()})" } ?: "") + ". Проверим позже."
        is ConfirmResult.Failed -> "Платёж за «${result.product.title}» не прошёл: ${result.state.title.lowercase()}"
        is ConfirmResult.Error -> "Не удалось проверить оплату. Повторите чуть позже."
    }
}
