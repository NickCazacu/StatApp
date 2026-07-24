package com.nichita.myvoyage.domain

import com.nichita.myvoyage.data.db.MonthTotal
import com.nichita.myvoyage.data.db.OfficeCategorySum
import com.nichita.myvoyage.data.db.VehicleCategorySum
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.model.OfficeExpense
import com.nichita.myvoyage.data.model.VehicleExpense

/**
 * Свод помесячных трат (офисы) в базовую валюту по курсу НБМ.
 * SQL не знает курсов, поэтому агрегаты считаются здесь, в Kotlin.
 */
object OfficeExpenseMath {

    /** Общая сумма в валюте [base]. */
    fun total(expenses: List<OfficeExpense>, rates: CurrencyRates, base: Currency): Double =
        expenses.sumOf { rates.convert(it.amount, it.currency, base) }

    /** Итоги по месяцам в хронологическом порядке, в валюте [base]. */
    fun monthTotals(
        expenses: List<OfficeExpense>,
        rates: CurrencyRates,
        base: Currency
    ): List<MonthTotal> =
        expenses.groupBy { it.year to it.month }
            .map { (key, items) -> MonthTotal(key.first, key.second, total(items, rates, base)) }
            .sortedWith(compareBy({ it.year }, { it.month }))

    /** Разбивка по категориям по убыванию суммы, в валюте [base]. */
    fun categorySums(
        expenses: List<OfficeExpense>,
        rates: CurrencyRates,
        base: Currency
    ): List<OfficeCategorySum> =
        expenses.groupBy { it.category }
            .map { (category, items) -> OfficeCategorySum(category, total(items, rates, base)) }
            .sortedByDescending { it.total }
}

/**
 * Свод помесячных трат (автомобили) в базовую валюту по курсу НБМ.
 * Зеркало [OfficeExpenseMath] для другого типа категорий.
 */
object VehicleExpenseMath {

    /** Общая сумма в валюте [base]. */
    fun total(expenses: List<VehicleExpense>, rates: CurrencyRates, base: Currency): Double =
        expenses.sumOf { rates.convert(it.amount, it.currency, base) }

    /** Итоги по месяцам в хронологическом порядке, в валюте [base]. */
    fun monthTotals(
        expenses: List<VehicleExpense>,
        rates: CurrencyRates,
        base: Currency
    ): List<MonthTotal> =
        expenses.groupBy { it.year to it.month }
            .map { (key, items) -> MonthTotal(key.first, key.second, total(items, rates, base)) }
            .sortedWith(compareBy({ it.year }, { it.month }))

    /** Разбивка по категориям по убыванию суммы, в валюте [base]. */
    fun categorySums(
        expenses: List<VehicleExpense>,
        rates: CurrencyRates,
        base: Currency
    ): List<VehicleCategorySum> =
        expenses.groupBy { it.category }
            .map { (category, items) -> VehicleCategorySum(category, total(items, rates, base)) }
            .sortedByDescending { it.total }
}
