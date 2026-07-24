package com.nichita.myvoyage.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Расход автомобиля, привязанный к конкретному месяцу (год + месяц) —
 * учёт по автомобилям ведётся помесячно, как у офисов.
 *
 * - Внешний ключ на [Vehicle] с CASCADE: при удалении авто удаляются и расходы.
 * - Индексы: по vehicleId — выборки "все расходы авто"; по (vehicleId, year,
 *   month) — помесячные агрегаты для статистики.
 */
@Entity(
    tableName = "vehicle_expenses",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId"), Index("vehicleId", "year", "month")]
)
data class VehicleExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Автомобиль, к которому относится расход */
    val vehicleId: Long,

    /** Год (напр. 2026) */
    val year: Int,

    /** Месяц 1..12 */
    val month: Int,

    /** Категория расхода автомобиля */
    val category: VehicleCategory = VehicleCategory.OTHER,

    /** Сумма расхода */
    val amount: Double,

    /**
     * Валюта расхода (по умолчанию совпадает с валютой автомобиля). Итоги
     * сводятся в валюту авто по курсу НБМ (см. [com.nichita.myvoyage.domain.CurrencyRates]).
     */
    val currency: Currency = Currency.MDL,

    /** Необязательная заметка */
    val note: String = ""
)
