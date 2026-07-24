package com.nichita.myvoyage.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nichita.myvoyage.data.model.Vehicle
import com.nichita.myvoyage.data.model.VehicleExpense
import kotlinx.coroutines.flow.Flow

/**
 * Доступ к автомобилям и их помесячным расходам.
 *
 * Суммы в SQL считаются только в разрезе валюты траты: складывать разные
 * валюты нельзя, свод в валюту автомобиля делается в Kotlin по курсу НБМ.
 */
@Dao
interface VehicleDao {

    // --- Автомобили ---

    @Query("SELECT * FROM vehicles ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<Vehicle?>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Vehicle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vehicle: Vehicle): Long

    @Update
    suspend fun update(vehicle: Vehicle)

    @Delete
    suspend fun delete(vehicle: Vehicle)

    // --- Расходы автомобиля ---

    /** Расходы автомобиля, свежие месяцы сверху. */
    @Query(
        """
        SELECT * FROM vehicle_expenses
        WHERE vehicleId = :vehicleId
        ORDER BY year DESC, month DESC, id DESC
        """
    )
    fun observeExpenses(vehicleId: Long): Flow<List<VehicleExpense>>

    /** То же разово (для советов/экспорта). */
    @Query(
        """
        SELECT * FROM vehicle_expenses
        WHERE vehicleId = :vehicleId
        ORDER BY year DESC, month DESC, id DESC
        """
    )
    suspend fun getExpenses(vehicleId: Long): List<VehicleExpense>

    @Query("SELECT * FROM vehicle_expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): VehicleExpense?

    /**
     * Итоги по всем автомобилям в разрезе валюты траты (для списка).
     * Разновалютные суммы сводятся в валюту авто уже в Kotlin по курсу НБМ.
     */
    @Query(
        """
        SELECT vehicleId AS vehicleId, currency AS currency, SUM(amount) AS total
        FROM vehicle_expenses
        GROUP BY vehicleId, currency
        """
    )
    fun observeCurrencyTotalsPerVehicle(): Flow<List<VehicleCurrencyTotal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: VehicleExpense): Long

    @Update
    suspend fun updateExpense(expense: VehicleExpense)

    @Delete
    suspend fun deleteExpense(expense: VehicleExpense)
}
