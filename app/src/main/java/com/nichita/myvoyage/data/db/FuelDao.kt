package com.nichita.myvoyage.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nichita.myvoyage.data.model.FuelEntry
import kotlinx.coroutines.flow.Flow

/** Доступ к заправкам. */
@Dao
interface FuelDao {

    /** Заправки рейса по дате. */
    @Query("SELECT * FROM fuel_entries WHERE tripId = :tripId ORDER BY date ASC")
    fun observeByTrip(tripId: Long): Flow<List<FuelEntry>>

    /** Разовое чтение заправок рейса. */
    @Query("SELECT * FROM fuel_entries WHERE tripId = :tripId ORDER BY date ASC")
    suspend fun getByTrip(tripId: Long): List<FuelEntry>

    /** Все заправки (для сравнения стоимости между рейсами). */
    @Query("SELECT * FROM fuel_entries ORDER BY date ASC")
    suspend fun getAll(): List<FuelEntry>

    /**
     * Стоимость топлива по каждому рейсу в разрезе валюты заправки, реактивно.
     * Заправки хранятся отдельно от expenses, поэтому их стоимость учитывается
     * отдельным запросом; свод в валюту рейса делается в ViewModel по курсу.
     */
    @Query(
        """
        SELECT tripId AS tripId, currency AS currency, SUM(cost) AS total
        FROM fuel_entries
        GROUP BY tripId, currency
        """
    )
    fun observeFuelTotalsPerTrip(): Flow<List<TripCurrencyTotal>>

    /** Стоимость топлива по рейсам и валютам (разово, для сравнения со средним). */
    @Query(
        """
        SELECT tripId AS tripId, currency AS currency, SUM(cost) AS total
        FROM fuel_entries
        GROUP BY tripId, currency
        """
    )
    suspend fun getFuelTotalsPerTrip(): List<TripCurrencyTotal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: FuelEntry): Long

    @Update
    suspend fun update(entry: FuelEntry)

    @Delete
    suspend fun delete(entry: FuelEntry)

    @Query("SELECT * FROM fuel_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FuelEntry?
}
