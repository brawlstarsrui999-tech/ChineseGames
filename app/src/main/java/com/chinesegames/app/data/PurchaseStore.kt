package com.chinesegames.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Товары магазина. Всё обучение бесплатно: здесь только украшения,
 * удобства и отключение рекламы. Цены — в рублях, оплата через Robokassa.
 */
enum class Product(
    /** Стабильный код товара — уходит в Robokassa (Shp_product) и в облако. */
    val code: String,
    val title: String,
    val emoji: String,
    val priceRub: Int,
    val description: String,
    val perks: List<String>
) {
    CUSTOM_MUSIC(
        code = "custom_music",
        title = "Своя музыка",
        emoji = "🎵",
        priceRub = 99,
        description = "Любой трек с телефона вместо стандартной фоновой музыки",
        perks = listOf(
            "Выбор любого аудиофайла с устройства",
            "Трек играет тихо и зациклено, как встроенная музыка",
            "В любой момент можно вернуть стандартную"
        )
    ),
    COLOR_THEMES(
        code = "color_themes",
        title = "Цветные стили",
        emoji = "🎨",
        priceRub = 199,
        description = "Розовый, синий, красный, оранжевый, жёлтый, зелёный и голубой — все сразу",
        perks = listOf(
            "Семь цветовых стилей одной покупкой",
            "Работают и в ночной, и в дневной теме",
            "Фиолетовый остаётся бесплатным всегда"
        )
    ),
    STYLE_CHINA(
        code = "style_china",
        title = "Стиль «Китайский дракон»",
        emoji = "🐉",
        priceRub = 199,
        description = "Красное золото, летящий дракон, фонарики, монеты и облака",
        perks = listOf(
            "Множество украшений на каждом экране",
            "Тихая традиционная китайская музыка (гучжэн и флейта)",
            "Красно-золотая палитра в подарок"
        )
    ),
    STYLE_ANIME(
        code = "style_anime",
        title = "Аниме-стиль",
        emoji = "✨",
        priceRub = 199,
        description = "Пиксельные Вагури с одной стороны и Сукуна-в-Мэгуми с другой",
        perks = listOf(
            "Чиби-персонажи по краям экрана, блёстки и звёзды",
            "Аниме-звуки в играх и энергичная фоновая музыка",
            "Розовая палитра в подарок"
        )
    ),
    MASCOT(
        code = "mascot",
        title = "Чиби-талисман",
        emoji = "💙",
        priceRub = 499,
        description = "Милая девочка с голубыми волосами, которая хвалит вас голосом",
        perks = listOf(
            "Озвученные фразы: «Отлично!», «Хороший результат, малыш!» и другие",
            "Реагирует на результаты игр и на касание",
            "Перетаскивается в любой угол экрана"
        )
    ),
    NO_ADS(
        code = "no_ads",
        title = "Без рекламы",
        emoji = "🚫",
        priceRub = 99,
        description = "Навсегда убирает рекламный баннер",
        perks = listOf(
            "Ни одного баннера на всех экранах",
            "Покупка сохраняется в аккаунте",
            "Обучение и так бесплатно — это только благодарность"
        )
    );

    val priceLabel: String get() = "$priceRub ₽"

    companion object {
        fun fromCode(code: String?): Product? = entries.firstOrNull { it.code == code }
    }
}

/** Счёт, который ждёт подтверждения оплаты от Robokassa. */
data class PendingInvoice(
    val invId: Long,
    val product: Product,
    val createdAt: Long
)

/**
 * Хранилище покупок: что куплено и какие счета ещё не подтверждены.
 *
 * Покупки лежат в отдельном файле SharedPreferences и дублируются в облако
 * после входа в аккаунт Google (см. `auth/CloudSync`): при переустановке
 * или смене телефона всё возвращается автоматически.
 */
class PurchaseStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _owned = MutableStateFlow(readOwned())
    /** Купленные товары — интерфейс подписывается и сразу перерисовывается. */
    val owned: StateFlow<Set<Product>> = _owned.asStateFlow()

    private val _pending = MutableStateFlow(readPending())
    /** Счета, оплату которых ещё нужно проверить. */
    val pending: StateFlow<List<PendingInvoice>> = _pending.asStateFlow()

    fun has(product: Product): Boolean = product in _owned.value

    /** Открыть товар (после подтверждённой оплаты или восстановления из облака). */
    fun grant(product: Product) {
        val next = _owned.value + product
        writeOwned(next)
        _owned.value = next
    }

    fun grantAll(products: Collection<Product>) {
        if (products.isEmpty()) return
        val next = _owned.value + products
        writeOwned(next)
        _owned.value = next
    }

    /** Отладочная отмена (например, в тестовой сборке); в интерфейсе не используется. */
    fun revoke(product: Product) {
        val next = _owned.value - product
        writeOwned(next)
        _owned.value = next
    }

    /** Новый номер счёта: монотонно растёт, уникален в рамках устройства. */
    fun nextInvoiceId(): Long {
        val base = maxOf(prefs.getLong(KEY_LAST_INVOICE, 0L), System.currentTimeMillis() / 1000L)
        val next = base + 1
        prefs.edit().putLong(KEY_LAST_INVOICE, next).apply()
        return next
    }

    fun addPending(invoice: PendingInvoice) {
        val next = (_pending.value.filter { it.invId != invoice.invId } + invoice)
            .sortedBy { it.createdAt }
        writePending(next)
        _pending.value = next
    }

    fun removePending(invId: Long) {
        val next = _pending.value.filter { it.invId != invId }
        writePending(next)
        _pending.value = next
    }

    /** Коды купленных товаров — для синхронизации с облаком. */
    fun exportCodes(): List<String> = _owned.value.map { it.code }.sorted()

    /** Объединение с покупками из облака: ничего не теряется. */
    fun importCodes(codes: Collection<String>) {
        val products = codes.mapNotNull { Product.fromCode(it) }
        grantAll(products)
    }

    /* ------------------------------ Хранение ------------------------------ */

    private fun readOwned(): Set<Product> =
        prefs.getStringSet(KEY_OWNED, emptySet()).orEmpty()
            .mapNotNull { Product.fromCode(it) }
            .toSet()

    private fun writeOwned(products: Set<Product>) {
        prefs.edit().putStringSet(KEY_OWNED, products.map { it.code }.toSet()).apply()
    }

    private fun readPending(): List<PendingInvoice> {
        val raw = prefs.getString(KEY_PENDING, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val product = Product.fromCode(item.optString("product")) ?: continue
                    add(
                        PendingInvoice(
                            invId = item.getLong("invId"),
                            product = product,
                            createdAt = item.optLong("createdAt", 0L)
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun writePending(list: List<PendingInvoice>) {
        val array = JSONArray()
        list.forEach { invoice ->
            array.put(
                JSONObject()
                    .put("invId", invoice.invId)
                    .put("product", invoice.product.code)
                    .put("createdAt", invoice.createdAt)
            )
        }
        prefs.edit().putString(KEY_PENDING, array.toString()).apply()
    }

    companion object {
        const val PREFS = "chinese_games_purchases"
        private const val KEY_OWNED = "owned"
        private const val KEY_PENDING = "pending"
        private const val KEY_LAST_INVOICE = "last_invoice"
    }
}
