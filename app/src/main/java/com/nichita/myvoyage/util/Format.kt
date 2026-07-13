package com.nichita.myvoyage.util

import com.nichita.myvoyage.data.model.Currency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Утилиты форматирования для UI (русская локаль). */
object Format {

    private val dateFmt = SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("ru"))
    private val dateTimeFmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("ru"))

    /** Дата из epoch millis в "дд.мм.гггг". */
    fun date(millis: Long): String = dateFmt.format(Date(millis))

    /** Дата и время из epoch millis в "дд.мм.гггг чч:мм". */
    fun dateTime(millis: Long): String = dateTimeFmt.format(Date(millis))

    /** Денежная сумма с символом валюты, 2 знака. */
    fun money(value: Double, currency: Currency): String =
        "${twoDecimals(value)} ${currency.symbol}"

    fun twoDecimals(value: Double): String =
        String.format(Locale.US, "%.2f", value)

    /**
     * Курс валюты с точностью под масштаб: крупные значения — 2 знака, мелкие —
     * до 4–6, чтобы дешёвые валюты (напр. форинт в евро) не схлопывались в 0.00.
     */
    fun rate(value: Double): String {
        val a = abs(value)
        val pattern = when {
            a >= 1.0 -> "%.2f"
            a >= 0.01 -> "%.4f"
            else -> "%.6f"
        }
        return String.format(Locale.US, pattern, value)
    }

    fun oneDecimal(value: Double): String =
        String.format(Locale.US, "%.1f", value)

    /** Целые километры. */
    fun km(value: Double): String = "${value.roundToInt()} км"
}
