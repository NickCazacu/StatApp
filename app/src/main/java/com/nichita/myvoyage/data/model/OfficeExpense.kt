package com.nichita.myvoyage.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Расход офиса, привязанный к конкретному месяцу (год + месяц), а не к дате —
 * учёт по офисам ведётся помесячно (аренда, коммунальные и т.д.).
 *
 * - Внешний ключ на [Office] с CASCADE: при удалении офиса удаляются и расходы.
 * - Индексы: по officeId — выборки "все расходы офиса"; по (officeId, year,
 *   month) — помесячные агрегаты для статистики.
 */
@Entity(
    tableName = "office_expenses",
    foreignKeys = [
        ForeignKey(
            entity = Office::class,
            parentColumns = ["id"],
            childColumns = ["officeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("officeId"), Index("officeId", "year", "month")]
)
data class OfficeExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Офис, к которому относится расход */
    val officeId: Long,

    /** Год (напр. 2026) */
    val year: Int,

    /** Месяц 1..12 */
    val month: Int,

    /** Категория офисного расхода */
    val category: OfficeCategory = OfficeCategory.OTHER,

    /** Сумма расхода */
    val amount: Double,

    /**
     * Валюта расхода (по умолчанию совпадает с валютой офиса). Итоги сводятся
     * в валюту офиса по курсу НБМ (см. [com.nichita.myvoyage.domain.CurrencyRates]).
     */
    val currency: Currency = Currency.MDL,

    /** Необязательная заметка */
    val note: String = ""
)
