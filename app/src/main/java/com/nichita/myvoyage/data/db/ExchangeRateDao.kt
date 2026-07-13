package com.nichita.myvoyage.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nichita.myvoyage.data.model.ExchangeRate
import kotlinx.coroutines.flow.Flow

/** Доступ к кэшу курсов валют. */
@Dao
interface ExchangeRateDao {

    /** Все сохранённые курсы, реактивно (пересчёт итогов при обновлении). */
    @Query("SELECT * FROM exchange_rates")
    fun observeAll(): Flow<List<ExchangeRate>>

    /** Разово — для расчётов вне UI (экспорт и т.п.). */
    @Query("SELECT * FROM exchange_rates")
    suspend fun getAll(): List<ExchangeRate>

    /** Обновляет/добавляет курсы пачкой (после успешной загрузки из сети). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rates: List<ExchangeRate>)
}
