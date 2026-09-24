package com.chinesegames.app.data

/**
 * Закрытые промокоды для автора и тестовой группы.
 *
 * Код вводится пользователем только один раз на устройстве, а выданный товар
 * синхронизируется с аккаунтом Google как обычная покупка. Коды намеренно не
 * показываются на витрине — это не рекламная механика, а тестовый доступ.
 */
enum class PromoCode(val value: String, val product: Product) {
    CHINA("RUIN4IK-PROMO20091", Product.STYLE_CHINA),
    CLOVER("RUIN4IK-PROMO20092", Product.STYLE_CLOVER),
    COLORS("RUIN4IK-PROMO20093", Product.COLOR_THEMES),
    CLOVERUSHKA("RUIN4IK-PROMO20094", Product.MASCOT),
    CUSTOM_MUSIC("RUIN4IK-PROMO20095", Product.CUSTOM_MUSIC),
    NO_ADS("RUIN4IK-PROMO20096", Product.NO_ADS);

    companion object {
        fun parse(raw: String): PromoCode? {
            val normalized = raw.trim().uppercase().replace('—', '-').replace('–', '-')
            return entries.firstOrNull { it.value == normalized }
        }
    }
}

/** Результат ввода промокода, не содержащий сам секретный код. */
sealed class PromoRedemption {
    data class Granted(val product: Product) : PromoRedemption()
    data object AlreadyUsed : PromoRedemption()
    data object Invalid : PromoRedemption()
}
