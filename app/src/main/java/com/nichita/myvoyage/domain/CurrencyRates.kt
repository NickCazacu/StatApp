package com.nichita.myvoyage.domain

import com.nichita.myvoyage.data.model.Currency

/**
 * Снимок курсов валют для конвертации сумм в валюту рейса.
 *
 * Хранит «сколько MDL за 1 единицу валюты» ([mdlPerUnit]). Конвертация идёт
 * через MDL как опорную валюту: `amount * rate(from) / rate(to)`.
 *
 * Всегда содержит все валюты приложения: реальные курсы (из БД/сети) поверх
 * встроенных [DEFAULTS], поэтому конвертация работает даже до первой загрузки
 * из интернета. [updatedAt] = null, пока ни разу не удалось обновиться.
 */
data class CurrencyRates(
    private val mdlPerUnit: Map<Currency, Double>,
    val updatedAt: Long?
) {
    /** Курс валюты к MDL; для неизвестных — 1.0 (не роняем расчёт). */
    fun rate(currency: Currency): Double =
        mdlPerUnit[currency] ?: DEFAULTS[currency] ?: 1.0

    /** Переводит [amount] из валюты [from] в валюту [to]. */
    fun convert(amount: Double, from: Currency, to: Currency): Double =
        if (from == to) amount else amount * rate(from) / rate(to)

    companion object {
        /**
         * Резервные курсы (MDL за единицу) на случай, если интернета не было ни
         * разу. Ориентир — официальные курсы НБМ середины 2026 г.; при первом же
         * успешном обновлении заменяются актуальными.
         */
        val DEFAULTS: Map<Currency, Double> = mapOf(
            Currency.MDL to 1.0,
            Currency.EUR to 20.0754,
            Currency.USD to 17.5576,
            Currency.RON to 3.8358,
            Currency.PLN to 4.6186,
            Currency.HUF to 0.056471, // НБМ даёт за 100 форинтов — здесь за 1
            Currency.BYN to 6.1211
        )

        /** Курсы «по умолчанию» — до появления данных из БД/сети. */
        val FALLBACK = CurrencyRates(DEFAULTS, null)
    }
}
