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

/** Доступ к автомобилям и их помесячным расходам. Агрегации считаются в SQL. */
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

    @Query("SELECT * FROM vehicle_expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): VehicleExpense?

    /** Общая сумма расходов автомобиля за всё время. */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM vehicle_expenses WHERE vehicleId = :vehicleId")
    fun observeTotal(vehicleId: Long): Flow<Double>

    /** Итоги по всем автомобилям, реактивно (для списка). */
    @Query(
        """
        SELECT vehicleId AS vehicleId, SUM(amount) AS total
        FROM vehicle_expenses
        GROUP BY vehicleId
        """
    )
    fun observeTotalsPerVehicle(): Flow<List<VehicleTotal>>

    /** Помесячные итоги автомобиля, хронологически (для графика динамики). */
    @Query(
        """
        SELECT year AS year, month AS month, SUM(amount) AS total
        FROM vehicle_expenses
        WHERE vehicleId = :vehicleId
        GROUP BY year, month
        ORDER BY year ASC, month ASC
        """
    )
    fun observeMonthTotals(vehicleId: Long): Flow<List<MonthTotal>>

    /** То же разово (для анализа советов). */
    @Query(
        """
        SELECT year AS year, month AS month, SUM(amount) AS total
        FROM vehicle_expenses
        WHERE vehicleId = :vehicleId
        GROUP BY year, month
        ORDER BY year ASC, month ASC
        """
    )
    suspend fun getMonthTotals(vehicleId: Long): List<MonthTotal>

    /** Разбивка расходов автомобиля по категориям (для статистики/советов). */
    @Query(
        """
        SELECT category AS category, SUM(amount) AS total
        FROM vehicle_expenses
        WHERE vehicleId = :vehicleId
        GROUP BY category
        ORDER BY total DESC
        """
    )
    fun observeCategorySums(vehicleId: Long): Flow<List<VehicleCategorySum>>

    /** То же разово (для анализа советов). */
    @Query(
        """
        SELECT category AS category, SUM(amount) AS total
        FROM vehicle_expenses
        WHERE vehicleId = :vehicleId
        GROUP BY category
        ORDER BY total DESC
        """
    )
    suspend fun getCategorySums(vehicleId: Long): List<VehicleCategorySum>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: VehicleExpense): Long

    @Update
    suspend fun updateExpense(expense: VehicleExpense)

    @Delete
    suspend fun deleteExpense(expense: VehicleExpense)
}
