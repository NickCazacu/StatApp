package com.nichita.myvoyage.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Офис — объект, по которому ведётся учёт ежемесячных расходов
 * (аренда, коммунальные, зарплаты и т.д.).
 *
 * Все траты офиса ведутся в одной валюте [currency] — так месячные итоги
 * складываются без конвертации (в отличие от рейсов, где валюты смешиваются).
 */
@Entity(tableName = "offices")
data class Office(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Название, напр. "Офис Кишинёв, центр" */
    val name: String,

    /** Адрес или заметка (необязательно) */
    val address: String = "",

    /** Валюта учёта расходов офиса */
    val currency: Currency = Currency.MDL
)
