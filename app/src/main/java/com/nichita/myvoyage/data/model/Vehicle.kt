package com.nichita.myvoyage.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Автомобиль — объект, по которому ведётся учёт ежемесячных расходов
 * (топливо, ТО, страховка, налоги и т.д.).
 *
 * Все траты автомобиля ведутся в одной валюте [currency] — так месячные
 * итоги складываются без конвертации (как у офисов).
 */
@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Название, напр. "Mercedes Sprinter" */
    val name: String,

    /** Гос. номер или заметка (необязательно) */
    val plate: String = "",

    /** Валюта учёта расходов автомобиля */
    val currency: Currency = Currency.MDL
)
