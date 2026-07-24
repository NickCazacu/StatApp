package com.nichita.myvoyage.data.repository

import com.nichita.myvoyage.data.db.VehicleCurrencyTotal
import com.nichita.myvoyage.data.db.VehicleDao
import com.nichita.myvoyage.data.model.Vehicle
import com.nichita.myvoyage.data.model.VehicleExpense
import kotlinx.coroutines.flow.Flow

/**
 * Доступ к автомобилям и их помесячным расходам для ViewModel-слоя.
 * Свод разновалютных сумм в валюту авто и советы — в domain/VM.
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
    suspend fun getExpenses(vehicleId: Long): List<VehicleExpense> = dao.getExpenses(vehicleId)
    suspend fun getExpense(id: Long): VehicleExpense? = dao.getExpenseById(id)
    fun observeCurrencyTotalsPerVehicle(): Flow<List<VehicleCurrencyTotal>> =
        dao.observeCurrencyTotalsPerVehicle()
    suspend fun upsertExpense(expense: VehicleExpense): Long =
        if (expense.id == 0L) dao.insertExpense(expense)
        else { dao.updateExpense(expense); expense.id }
    suspend fun deleteExpense(expense: VehicleExpense) = dao.deleteExpense(expense)
}
