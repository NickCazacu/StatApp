package com.nichita.myvoyage.data.db

import com.nichita.myvoyage.data.model.Category
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.model.OfficeCategory
import com.nichita.myvoyage.data.model.VehicleCategory

/** Сумма расходов по одной категории (результат GROUP BY в DAO). */
data class CategorySum(
    val category: Category,
    val total: Double
)

/**
 * Сумма расходов по категории в разрезе валюты рейса.
 * Нужна для сводной статистики по всем рейсам: валюты не складываются,
 * поэтому разбивка считается отдельно для каждой валюты.
 */
data class CurrencyCategorySum(
    val currency: Currency,
    val category: Category,
    val total: Double
)

/** Итоговая сумма по рейсу (для сравнения рейсов между собой). */
data class TripTotal(
    val tripId: Long,
    val total: Double
)

/**
 * Сумма расходов рейса в разрезе валюты самого расхода.
 * Позволяет свести разновалютные расходы в валюту рейса по курсу (конвертация
 * делается в Kotlin, т.к. SQL не знает курсов).
 */
data class TripCurrencyTotal(
    val tripId: Long,
    val currency: Currency,
    val total: Double
)

/** Итог расходов офиса за один месяц (результат GROUP BY year, month). */
data class MonthTotal(
    val year: Int,
    val month: Int,
    val total: Double
)

/** Сумма расходов офиса по одной категории. */
data class OfficeCategorySum(
    val category: OfficeCategory,
    val total: Double
)

/** Итоговая сумма по офису за всё время (для списка офисов). */
data class OfficeTotal(
    val officeId: Long,
    val total: Double
)

/** Сумма расходов автомобиля по одной категории. */
data class VehicleCategorySum(
    val category: VehicleCategory,
    val total: Double
)

/** Итоговая сумма по автомобилю за всё время (для списка автомобилей). */
data class VehicleTotal(
    val vehicleId: Long,
    val total: Double
)
