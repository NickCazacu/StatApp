package com.nichita.myvoyage.data.remote

import android.util.Xml
import com.nichita.myvoyage.data.model.Currency
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    /**
     * Возвращает курсы (код валюты → MDL за единицу) или null при любой ошибке
     * (нет сети, таймаут, некорректный ответ). Вызывать на IO-диспетчере.
     */
    fun fetchRates(): Map<String, Double>? {
        val date = SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date())
        val url = URL("https://www.bnm.md/en/official_exchange_rates?get_xml=1&date=$date")
        val supported = Currency.entries.map { it.code }.toSet()

        var connection: HttpURLConnection? = null
        return try {
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.use { stream -> parse(stream, supported) }
                .takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            // Офлайн/сбой — молча возвращаем null, вызывающий оставит кэш.
            null
        } finally {
            connection?.disconnect()
        }
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
