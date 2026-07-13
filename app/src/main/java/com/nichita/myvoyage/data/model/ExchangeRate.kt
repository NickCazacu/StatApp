package com.nichita.myvoyage.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сохранённый курс валюты относительно MDL (молдавского лея).
 *
 * [mdlPerUnit] — сколько лей стоит 1 единица валюты (напр. EUR ≈ 20.07).
 * Курсы кэшируются в БД, чтобы работать офлайн: при наличии интернета
 * обновляются из официального источника НБМ, иначе берутся последние сохранённые.
 */
@Entity(tableName = "exchange_rates")
data class ExchangeRate(
    /** ISO-код валюты (EUR, USD, …). MDL хранить не нужно — база всегда = 1. */
    @PrimaryKey
    val code: String,

    /** Сколько MDL за 1 единицу валюты. */
    val mdlPerUnit: Double,

    /** Когда курс получен (epoch millis) — для показа «обновлено …». */
    val updatedAt: Long
)
