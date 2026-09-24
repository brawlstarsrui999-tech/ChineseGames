package com.chinesegames.app.billing

import android.net.Uri
import com.chinesegames.app.data.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

/** Состояние счёта в Robokassa (коды State.Code из OpStateExt). */
enum class InvoiceState(val code: Int, val title: String) {
    INITIATED(5, "Оплата ещё не подтверждена"),
    CANCELLED(10, "Платёж отменён"),
    HOLD(20, "Средства захолдированы"),
    RECEIVED(50, "Деньги получены, идёт зачисление"),
    REFUSED(60, "Отказ в зачислении, деньги возвращены"),
    SUSPENDED(80, "Платёж приостановлен на проверку"),
    PAID(100, "Оплачено"),
    NOT_FOUND(-1, "Счёт не найден"),
    UNKNOWN(-2, "Статус неизвестен");

    val isFinalFailure: Boolean get() = this == CANCELLED || this == REFUSED

    companion object {
        fun fromCode(code: Int): InvoiceState = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}

/** Результат проверки статуса. */
sealed class StatusResult {
    data class Ok(val state: InvoiceState) : StatusResult()
    data class Error(val message: String) : StatusResult()
}

/**
 * Клиент Robokassa: платёжные ссылки и проверка статуса.
 *
 *  * Платёжная ссылка: `MerchantLogin`, `OutSum`, `InvId`, `Description`,
 *    `SignatureValue = md5("login:outsum:invid:пароль#1:Shp_product=…")`,
 *    `IsTest=1` в тестовом режиме. Пользовательские параметры `Shp_*`
 *    входят в подпись в алфавитном порядке.
 *  * Статус: `OpStateExt?MerchantLogin&InvoiceID&Signature=md5("login:invid:пароль#2")`,
 *    ответ — XML с `Result/Code` (0 — ок) и `State/Code` (100 — оплачено).
 *
 * Для продакшена подпись стоит считать на сервере ([RobokassaConfig.signatureEndpoint]):
 * тогда Пароль #1 в приложении не хранится.
 */
class RobokassaClient(private val config: RobokassaConfig = RobokassaConfig) {

    /** Сумма в формате Robokassa: два знака после точки. */
    fun outSum(product: Product): String = String.format(Locale.US, "%.2f", product.priceRub.toDouble())

    /** Описание платежа (видно покупателю на странице оплаты). */
    fun description(product: Product): String = "ChineseGames: ${product.title}"

    /** Подпись платёжной ссылки Паролем #1. */
    fun paymentSignature(product: Product, invId: Long): String {
        val base = "${config.merchantLogin}:${outSum(product)}:$invId:${config.password1}:Shp_product=${product.code}"
        return md5(base)
    }

    /** Платёжная ссылка, подписанная локально (тестовый режим / простая схема). */
    fun buildPaymentUrl(product: Product, invId: Long, email: String? = null): String {
        val builder = Uri.parse(RobokassaConfig.PAYMENT_URL).buildUpon()
            .appendQueryParameter("MerchantLogin", config.merchantLogin)
            .appendQueryParameter("OutSum", outSum(product))
            .appendQueryParameter("InvId", invId.toString())
            .appendQueryParameter("Description", description(product))
            .appendQueryParameter("SignatureValue", paymentSignature(product, invId))
            .appendQueryParameter("Shp_product", product.code)
            .appendQueryParameter("Culture", "ru")
            .appendQueryParameter("Encoding", "utf-8")
        if (!email.isNullOrBlank()) builder.appendQueryParameter("Email", email)
        if (config.isTest) builder.appendQueryParameter("IsTest", "1")
        return builder.build().toString()
    }

    /**
     * Платёжная ссылка: либо от вашего сервера подписи (POST JSON
     * `{"product","invId","outSum","description"}` → `{"url": "..."}`),
     * либо подписанная локально.
     */
    suspend fun paymentUrl(product: Product, invId: Long, email: String? = null): String {
        val endpoint = config.signatureEndpoint
        if (endpoint.isEmpty()) return buildPaymentUrl(product, invId, email)
        return withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("product", product.code)
                .put("invId", invId)
                .put("outSum", outSum(product))
                .put("description", description(product))
                .put("email", email ?: "")
                .put("isTest", config.isTest)
                .toString()
            val response = httpPostJson(endpoint, body)
            val json = JSONObject(response)
            json.optString("url").takeIf { it.isNotBlank() }
                ?: throw IOException("Сервер подписи не вернул ссылку")
        }
    }

    /** Подпись запроса статуса Паролем #2. */
    fun statusSignature(invId: Long): String = md5("${config.merchantLogin}:$invId:${config.password2}")

    /** Проверка статуса счёта через OpStateExt. */
    suspend fun checkStatus(invId: Long): StatusResult = withContext(Dispatchers.IO) {
        if (!config.canCheckStatus) {
            return@withContext StatusResult.Error("Проверка оплаты не настроена (нет Пароля #2)")
        }
        try {
            val url = Uri.parse(RobokassaConfig.OP_STATE_URL).buildUpon()
                .appendQueryParameter("MerchantLogin", config.merchantLogin)
                .appendQueryParameter("InvoiceID", invId.toString())
                .appendQueryParameter("Signature", statusSignature(invId))
                .apply { if (config.isTest) appendQueryParameter("IsTest", "1") }
                .build()
                .toString()
            val xml = httpGet(url)
            StatusResult.Ok(parseOpState(xml))
        } catch (t: Throwable) {
            StatusResult.Error(t.message ?: "Нет связи с Robokassa")
        }
    }

    /** Разбор ответа OpStateExt без XML-парсера: нужны только два кода. */
    fun parseOpState(xml: String): InvoiceState {
        val resultCode = Regex("<Result>\\s*<Code>(\\d+)</Code>", RegexOption.IGNORE_CASE)
            .find(xml)?.groupValues?.get(1)?.toIntOrNull()
        if (resultCode == null) return InvoiceState.UNKNOWN
        if (resultCode == 3) return InvoiceState.NOT_FOUND
        if (resultCode != 0) return InvoiceState.UNKNOWN
        val stateCode = Regex("<State>\\s*<Code>(\\d+)</Code>", RegexOption.IGNORE_CASE)
            .find(xml)?.groupValues?.get(1)?.toIntOrNull() ?: return InvoiceState.UNKNOWN
        return InvoiceState.fromCode(stateCode)
    }

    /* ------------------------------- HTTP ------------------------------- */

    private fun httpGet(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }
        return connection.use { it.readBody() }
    }

    private fun httpPostJson(url: String, body: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        return connection.use { conn ->
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            conn.readBody()
        }
    }

    private fun HttpURLConnection.readBody(): String {
        val code = responseCode
        val stream = if (code in 200..299) inputStream else (errorStream ?: inputStream)
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (code !in 200..299) throw IOException("HTTP $code: ${text.take(200)}")
        return text
    }

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T = try {
        block(this)
    } finally {
        disconnect()
    }

    companion object {
        private const val TIMEOUT_MS = 15_000

        fun md5(text: String): String {
            val digest = MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        }
    }
}
