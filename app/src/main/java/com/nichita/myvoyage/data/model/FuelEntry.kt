package com.nichita.myvoyage.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Заправка, привязанная к рейсу.
 *
 * Фиксируется только итоговая цена заправки — литры и одометр не вводятся.
 */
@Entity(
    tableName = "fuel_entries",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class FuelEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Рейс, к которому относится заправка */
    val tripId: Long,

    /** Дата заправки (epoch millis) */
    val date: Long,

    /** Цена заправки (в валюте [currency]) */
    val cost: Double,

    /** Валюта заправки (может отличаться от валюты рейса) */
    val currency: Currency = Currency.DEFAULT,

    /** Тип топлива */
    val fuelType: FuelType = FuelType.DEFAULT
)
