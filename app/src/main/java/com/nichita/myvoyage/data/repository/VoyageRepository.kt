package com.nichita.myvoyage.data.repository

import com.nichita.myvoyage.data.db.CategorySum
import com.nichita.myvoyage.data.db.CurrencyCategorySum
import com.nichita.myvoyage.data.db.ExpenseDao
import com.nichita.myvoyage.data.db.FuelDao
import com.nichita.myvoyage.data.db.TripCurrencyTotal
import com.nichita.myvoyage.data.db.TripDao
import com.nichita.myvoyage.data.db.TripTotal
import com.nichita.myvoyage.data.model.Expense
import com.nichita.myvoyage.data.model.FuelEntry
import com.nichita.myvoyage.data.model.Trip
import kotlinx.coroutines.flow.Flow

/**
 * Единая точка доступа к данным для ViewModel-слоя.
 *
 * Скрывает DAO за понятным API. Не содержит бизнес-расчётов —
 * они вынесены в domain (FuelCalculator, TipsAnalyzer).
 */
class VoyageRepository(
    private val tripDao: TripDao,
    private val expenseDao: ExpenseDao,
    private val fuelDao: FuelDao
) {

    // --- Рейсы ---
    fun observeTrips(): Flow<List<Trip>> = tripDao.observeAll()
    fun observeTrip(tripId: Long): Flow<Trip?> = tripDao.observeById(tripId)
    suspend fun getTrip(tripId: Long): Trip? = tripDao.getById(tripId)
    suspend fun getAllTrips(): List<Trip> = tripDao.getAll()
    suspend fun upsertTrip(trip: Trip): Long =
        if (trip.id == 0L) tripDao.insert(trip) else { tripDao.update(trip); trip.id }
    suspend fun deleteTrip(trip: Trip) = tripDao.delete(trip)

    // --- Расходы ---
    fun observeExpenses(tripId: Long): Flow<List<Expense>> = expenseDao.observeByTrip(tripId)
    fun observeTotal(tripId: Long): Flow<Double> = expenseDao.observeTotalForTrip(tripId)
    fun observeCategorySums(tripId: Long): Flow<List<CategorySum>> =
        expenseDao.observeCategorySums(tripId)
    suspend fun getExpense(id: Long): Expense? = expenseDao.getById(id)
    suspend fun getExpensesForTrip(tripId: Long): List<Expense> = expenseDao.getByTrip(tripId)
    fun observeTotalsPerTrip(): Flow<List<TripTotal>> = expenseDao.observeTotalsPerTrip()
    suspend fun getTotalsPerTrip(): List<TripTotal> = expenseDao.getTotalsPerTrip()
    fun observeCurrencyTotalsPerTrip(): Flow<List<TripCurrencyTotal>> =
        expenseDao.observeCurrencyTotalsPerTrip()
    suspend fun getCurrencyTotalsPerTrip(): List<TripCurrencyTotal> =
        expenseDao.getCurrencyTotalsPerTrip()
    fun observeCurrencyCategorySumsForTrip(tripId: Long): Flow<List<CurrencyCategorySum>> =
        expenseDao.observeCurrencyCategorySumsForTrip(tripId)
    suspend fun getCurrencyCategorySumsForTrip(tripId: Long): List<CurrencyCategorySum> =
        expenseDao.getCurrencyCategorySumsForTrip(tripId)
    suspend fun getCategorySumsForTrip(tripId: Long): List<CategorySum> =
        expenseDao.getCategorySumsForTrip(tripId)
    suspend fun getCategorySumsAllTrips(): List<CategorySum> = expenseDao.getCategorySumsAllTrips()
    fun observeCategorySumsByCurrency(): Flow<List<CurrencyCategorySum>> =
        expenseDao.observeCategorySumsByCurrency()
    suspend fun getCategorySumsByCurrency(): List<CurrencyCategorySum> =
        expenseDao.getCategorySumsByCurrency()
    suspend fun upsertExpense(expense: Expense): Long =
        if (expense.id == 0L) expenseDao.insert(expense) else { expenseDao.update(expense); expense.id }
    suspend fun deleteExpense(expense: Expense) = expenseDao.delete(expense)

    // --- Заправки ---
    fun observeFuel(tripId: Long): Flow<List<FuelEntry>> = fuelDao.observeByTrip(tripId)
    fun observeFuelTotalsPerTrip(): Flow<List<TripTotal>> = fuelDao.observeFuelTotalsPerTrip()
    suspend fun getFuelTotalsPerTrip(): List<TripTotal> = fuelDao.getFuelTotalsPerTrip()
    suspend fun getFuelForTrip(tripId: Long): List<FuelEntry> = fuelDao.getByTrip(tripId)
    suspend fun getAllFuel(): List<FuelEntry> = fuelDao.getAll()
    suspend fun getFuelEntry(id: Long): FuelEntry? = fuelDao.getById(id)
    suspend fun upsertFuel(entry: FuelEntry): Long =
        if (entry.id == 0L) fuelDao.insert(entry) else { fuelDao.update(entry); entry.id }
    suspend fun deleteFuel(entry: FuelEntry) = fuelDao.delete(entry)
}
