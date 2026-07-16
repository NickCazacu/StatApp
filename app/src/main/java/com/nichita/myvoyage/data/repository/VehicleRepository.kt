package com.nichita.myvoyage.data.repository

import com.nichita.myvoyage.data.db.MonthTotal
import com.nichita.myvoyage.data.db.VehicleCategorySum
import com.nichita.myvoyage.data.db.VehicleDao
import com.nichita.myvoyage.data.db.VehicleTotal
import com.nichita.myvoyage.data.model.Vehicle
import com.nichita.myvoyage.data.model.VehicleExpense
import kotlinx.coroutines.flow.Flow

/**
 * Доступ к автомобилям и их помесячным расходам для ViewModel-слоя.
 * Бизнес-расчёты (советы) — в domain (VehicleTipsAnalyzer).
 */
class VehicleRepository(private val dao: VehicleDao) {

    // --- Автомобили ---
    fun observeVehicles(): Flow<List<Vehicle>> = dao.observeAll()
    fun observeVehicle(id: Long): Flow<Vehicle?> = dao.observeById(id)
    suspend fun getVehicle(id: Long): Vehicle? = dao.getById(id)
    suspend fun upsertVehicle(vehicle: Vehicle): Long =
        if (vehicle.id == 0L) dao.insert(vehicle) else { dao.update(vehicle); vehicle.id }
    suspend fun deleteVehicle(vehicle: Vehicle) = dao.delete(vehicle)

    // --- Расходы ---
    fun observeExpenses(vehicleId: Long): Flow<List<VehicleExpense>> =
        dao.observeExpenses(vehicleId)
    suspend fun getExpense(id: Long): VehicleExpense? = dao.getExpenseById(id)
    fun observeTotal(vehicleId: Long): Flow<Double> = dao.observeTotal(vehicleId)
    fun observeTotalsPerVehicle(): Flow<List<VehicleTotal>> = dao.observeTotalsPerVehicle()
    fun observeMonthTotals(vehicleId: Long): Flow<List<MonthTotal>> =
        dao.observeMonthTotals(vehicleId)
    suspend fun getMonthTotals(vehicleId: Long): List<MonthTotal> = dao.getMonthTotals(vehicleId)
    fun observeCategorySums(vehicleId: Long): Flow<List<VehicleCategorySum>> =
        dao.observeCategorySums(vehicleId)
    suspend fun getCategorySums(vehicleId: Long): List<VehicleCategorySum> =
        dao.getCategorySums(vehicleId)
    suspend fun upsertExpense(expense: VehicleExpense): Long =
        if (expense.id == 0L) dao.insertExpense(expense)
        else { dao.updateExpense(expense); expense.id }
    suspend fun deleteExpense(expense: VehicleExpense) = dao.deleteExpense(expense)
}
