package com.chinesegames.app.billing

import com.chinesegames.app.data.PendingInvoice
import com.chinesegames.app.data.Product
import com.chinesegames.app.data.PromoRedemption
import com.chinesegames.app.data.PurchaseStore
import kotlinx.coroutines.flow.StateFlow

/** Что показать пользователю после попытки подтвердить оплату. */
sealed class ConfirmResult {
    /** Товар открыт. */
    data class Granted(val product: Product) : ConfirmResult()

    /** Оплата ещё не дошла — счёт остаётся в ожидании, проверим позже. */
    data class Pending(val product: Product, val state: InvoiceState?) : ConfirmResult()

    /** Платёж точно не состоялся (отменён / возвращён). */
    data class Failed(val product: Product, val state: InvoiceState) : ConfirmResult()

    /** Не удалось проверить (нет сети/не настроено) — счёт остаётся в ожидании. */
    data class Error(val product: Product, val message: String) : ConfirmResult()
}

/** Готовый к оплате счёт. */
data class PaymentSession(val invoice: PendingInvoice, val url: String)

/**
 * Управление покупками: выставить счёт → открыть оплату → подтвердить → открыть товар.
 *
 * Обучение бесплатно, поэтому здесь только украшения; цены и коды — в [Product].
 * Все средства, полученные через Robokassa, не возвращаются — об этом магазин
 * предупреждает до покупки.
 */
class PurchaseManager(
    private val store: PurchaseStore,
    private val client: RobokassaClient = RobokassaClient()
) {
    val owned: StateFlow<Set<Product>> get() = store.owned
    val pending: StateFlow<List<PendingInvoice>> get() = store.pending

    fun has(product: Product): Boolean = store.has(product)

    /** Внутренний тестовый/авторский доступ без платежа. */
    fun redeemPromo(raw: String): PromoRedemption = store.redeemPromo(raw)

    /** Настроен ли магазин (без ключей Robokassa кнопки «Купить» неактивны). */
    val isAvailable: Boolean get() = RobokassaConfig.isConfigured

    /** Выставить счёт и получить ссылку на оплату. */
    suspend fun begin(product: Product, email: String? = null): PaymentSession {
        val invoice = PendingInvoice(
            invId = store.nextInvoiceId(),
            product = product,
            createdAt = System.currentTimeMillis()
        )
        val url = client.paymentUrl(product, invoice.invId, email)
        store.addPending(invoice)
        return PaymentSession(invoice, url)
    }

    /** Ссылка на оплату уже выставленного счёта (например, после поворота экрана). */
    suspend fun paymentUrl(invoice: PendingInvoice): String = client.paymentUrl(invoice.product, invoice.invId)

    fun pendingInvoice(invId: Long): PendingInvoice? = store.pending.value.firstOrNull { it.invId == invId }

    /**
     * Проверить оплату счёта. Если Robokassa подтверждает — товар открывается,
     * если платёж отменён — счёт убирается, иначе остаётся в ожидании.
     */
    suspend fun confirm(invoice: PendingInvoice): ConfirmResult {
        if (store.has(invoice.product)) {
            store.removePending(invoice.invId)
            return ConfirmResult.Granted(invoice.product)
        }
        return when (val result = client.checkStatus(invoice.invId)) {
            is StatusResult.Ok -> when {
                result.state == InvoiceState.PAID || result.state == InvoiceState.RECEIVED -> {
                    store.grant(invoice.product)
                    store.removePending(invoice.invId)
                    ConfirmResult.Granted(invoice.product)
                }
                result.state.isFinalFailure -> {
                    store.removePending(invoice.invId)
                    ConfirmResult.Failed(invoice.product, result.state)
                }
                result.state == InvoiceState.NOT_FOUND && isStale(invoice) -> {
                    store.removePending(invoice.invId)
                    ConfirmResult.Failed(invoice.product, result.state)
                }
                else -> ConfirmResult.Pending(invoice.product, result.state)
            }
            is StatusResult.Error -> ConfirmResult.Error(invoice.product, result.message)
        }
    }

    /** Пользователь закрыл оплату, не заплатив: свежий счёт можно просто забыть. */
    fun cancel(invoice: PendingInvoice) {
        store.removePending(invoice.invId)
    }

    /** «Восстановить покупки»: перепроверить все ожидающие счета. */
    suspend fun restorePending(): List<ConfirmResult> =
        store.pending.value.map { confirm(it) }

    /** Счёт старше суток, о котором Robokassa ничего не знает, — брошенный. */
    private fun isStale(invoice: PendingInvoice): Boolean =
        System.currentTimeMillis() - invoice.createdAt > 24L * 60 * 60 * 1000

    companion object {
        /** Текст, который магазин показывает до оплаты (по требованию оферты). */
        const val NON_REFUND_NOTICE =
            "Всё обучение в ChineseGames полностью бесплатно. Покупки — это только украшения " +
                "и благодарность разработчику. Оплата проходит через Robokassa; " +
                "средства не возвращаются."
    }
}
