package com.chinesegames.app.billing

import com.chinesegames.app.BuildConfig

/**
 * Настройки подключения к Robokassa.
 *
 * Значения приходят из BuildConfig: Gradle читает их из `local.properties`
 * (или из свойств проекта `-P…`) и ни один секрет не попадает в git —
 * см. `docs/robokassa.md`.
 *
 *  ROBOKASSA_LOGIN       — идентификатор магазина (MerchantLogin)
 *  ROBOKASSA_PASSWORD1   — Пароль #1 (подпись платёжной ссылки)
 *  ROBOKASSA_PASSWORD2   — Пароль #2 (проверка статуса счёта, OpStateExt)
 *  ROBOKASSA_TEST        — true: тестовые платежи (IsTest=1), деньги не списываются
 *  ROBOKASSA_SIGN_URL    — (рекомендуется для продакшена) адрес вашего сервера,
 *                          который подписывает ссылку сам; тогда Пароль #1
 *                          вообще не нужен в приложении.
 */
object RobokassaConfig {
    val merchantLogin: String get() = BuildConfig.ROBOKASSA_LOGIN.trim()
    val password1: String get() = BuildConfig.ROBOKASSA_PASSWORD1.trim()
    val password2: String get() = BuildConfig.ROBOKASSA_PASSWORD2.trim()
    val isTest: Boolean get() = BuildConfig.ROBOKASSA_TEST
    val signatureEndpoint: String get() = BuildConfig.ROBOKASSA_SIGN_URL.trim()

    /** Платёжная страница Robokassa. */
    const val PAYMENT_URL = "https://auth.robokassa.ru/Merchant/Index.aspx"

    /** XML-интерфейс статуса операции. */
    const val OP_STATE_URL = "https://auth.robokassa.ru/Merchant/WebService/Service.asmx/OpStateExt"

    /**
     * Адреса возврата, указанные в личном кабинете Robokassa (Success URL / Fail URL).
     * Домен может быть любым: приложение перехватывает переход на эти адреса
     * внутри WebView и само проверяет статус счёта.
     */
    const val SUCCESS_URL = "https://chinesegames.app/pay/success"
    const val FAIL_URL = "https://chinesegames.app/pay/fail"

    /** Можно ли вообще выставлять счета (заполнен логин и есть чем подписывать). */
    val isConfigured: Boolean
        get() = merchantLogin.isNotEmpty() && (password1.isNotEmpty() || signatureEndpoint.isNotEmpty())

    /** Можно ли проверять статус оплаты прямо из приложения. */
    val canCheckStatus: Boolean
        get() = merchantLogin.isNotEmpty() && password2.isNotEmpty()
}
