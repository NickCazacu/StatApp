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

/**
 * Сумма расходов офиса в разрезе валюты траты (для списка офисов).
 * Свод в валюту офиса делается в Kotlin по курсу НБМ.
 */
data class OfficeCurrencyTotal(
    val officeId: Long,
    val currency: Currency,
    val total: Double
)

/** Сумма расходов автомобиля по одной категории. */
data class VehicleCategorySum(
    val category: VehicleCategory,
    val total: Double
)

/**
 * Сумма расходов автомобиля в разрезе валюты траты (для списка автомобилей).
 * Свод в валюту авто делается в Kotlin по курсу НБМ.
 */
data class VehicleCurrencyTotal(
    val vehicleId: Long,
    val currency: Currency,
    val total: Double
)
