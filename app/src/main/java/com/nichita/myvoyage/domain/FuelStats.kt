package com.nichita.myvoyage.domain

import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.model.FuelEntry

/**
 * Результат расчёта по топливу для одного рейса.
 */
data class FuelStats(
    /** Полная стоимость топлива за рейс (в валюте рейса). */
    val totalFuelCost: Double = 0.0,
    /** Количество заправок. */
    val entriesCount: Int = 0
)

/** Суммирует стоимость заправок рейса. */
object FuelCalculator {

    /** Разновалютные заправки сводятся в [currency] по курсу [rates]. */
    fun calculate(entries: List<FuelEntry>, rates: CurrencyRates, currency: Currency): FuelStats =
        FuelStats(
            totalFuelCost = entries.sumOf { rates.convert(it.cost, it.currency, currency) },
            entriesCount = entries.size
        )
}
