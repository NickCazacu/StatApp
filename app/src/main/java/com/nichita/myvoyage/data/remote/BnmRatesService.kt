package com.nichita.myvoyage.data.remote

import android.util.Xml
import com.nichita.myvoyage.data.model.Currency
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

/**
 * Загрузка официальных курсов валют Национального банка Молдовы (bnm.md).
 *
 * Источник открытый и не требует ключа. Ответ — XML вида:
 * ```
 * <ValCurs Date="13.07.2026">
 *   <Valute ID="47">
 *     <CharCode>EUR</CharCode><Nominal>1</Nominal><Value>20.0754</Value>
 *   </Valute> ...
 * </ValCurs>
 * ```
 * `Value` — сколько MDL за `Nominal` единиц валюты, поэтому курс за 1 единицу =
 * `Value / Nominal`. Берём только валюты, которые реально используются в приложении.
 *
 * Реализовано на [HttpURLConnection] + [Xml] (встроены в Android) — без внешних
 * сетевых зависимостей, чтобы не усложнять сборку и R8.
 */
class BnmRatesService {

    private companion object {
        /**
         * Потолок на размер ответа. Настоящий XML НБМ — десятки килобайт;
         * миллион байт с огромным запасом покрывает любой нормальный ответ.
         * Лимит защищает от «бесконечного» тела ответа (подменённый DNS,
         * сломанный прокси, сбой на стороне сервера), которое иначе съело бы
         * память телефона на этапе парсинга.
         */
        const val MAX_RESPONSE_BYTES = 1_000_000
    }

    /**
     * Возвращает курсы (код валюты → MDL за единицу) или null при любой ошибке
     * (нет сети, таймаут, некорректный ответ). Вызывать на IO-диспетчере.
     */
    fun fetchRates(): Map<String, Double>? {
        val date = SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date())
        val url = URL("https://www.bnm.md/en/official_exchange_rates?get_xml=1&date=$date")
        val supported = Currency.entries.map { it.code }.toSet()

        var connection: HttpsURLConnection? = null
        return try {
            // Только TLS. Если соединение почему-то оказалось обычным HTTP
            // (подмена URL, редирект вниз по протоколу) — приведение упадёт,
            // и курсы просто не обновятся, вместо отправки запроса в открытую.
            connection = (url.openConnection() as HttpsURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                useCaches = false
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = connection.inputStream.use { it.readAtMost(MAX_RESPONSE_BYTES) }
                ?: return null
            parse(ByteArrayInputStream(body), supported).takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            // Офлайн/сбой — молча возвращаем null, вызывающий оставит кэш.
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Читает поток целиком, но не больше [limit] байт. Если данных оказалось
     * больше — возвращает null: такой ответ точно не курсы НБМ, доверять ему
     * и тем более разбирать его не нужно.
     */
    private fun java.io.InputStream.readAtMost(limit: Int): ByteArray? {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024)
        while (true) {
            val read = read(chunk)
            if (read == -1) break
            if (buffer.size() + read > limit) return null
            buffer.write(chunk, 0, read)
        }
        return buffer.toByteArray()
    }

    private fun parse(stream: java.io.InputStream, supported: Set<String>): Map<String, Double> {
        val result = LinkedHashMap<String, Double>()
        val parser = Xml.newPullParser()
        parser.setInput(stream, null)

        var code: String? = null
        var nominal = 1.0
        var value = 0.0
        var text = ""

        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (event) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> {
                    if (parser.name == "Valute") {
                        code = null; nominal = 1.0; value = 0.0
                    }
                    text = ""
                }
                org.xmlpull.v1.XmlPullParser.TEXT -> text = parser.text
                org.xmlpull.v1.XmlPullParser.END_TAG -> when (parser.name) {
                    "CharCode" -> code = text.trim()
                    "Nominal" -> nominal = text.trim().toDoubleOrNull() ?: 1.0
                    "Value" -> value = text.trim().toDoubleOrNull() ?: 0.0
                    "Valute" -> {
                        val c = code
                        if (c != null && c in supported && nominal > 0.0 && value > 0.0) {
                            result[c] = value / nominal
                        }
                    }
                }
            }
            event = parser.next()
        }
        return result
    }
}
