package com.nichita.myvoyage.data.repository

import com.nichita.myvoyage.data.db.MonthTotal
import com.nichita.myvoyage.data.db.OfficeCategorySum
import com.nichita.myvoyage.data.db.OfficeDao
import com.nichita.myvoyage.data.db.OfficeTotal
import com.nichita.myvoyage.data.model.Office
import com.nichita.myvoyage.data.model.OfficeExpense
import kotlinx.coroutines.flow.Flow

/**
 * Доступ к офисам и их помесячным расходам для ViewModel-слоя.
 * Бизнес-расчёты (советы) — в domain (OfficeTipsAnalyzer).
 */
class OfficeRepository(private val dao: OfficeDao) {

    // --- Офисы ---
    fun observeOffices(): Flow<List<Office>> = dao.observeAll()
    fun observeOffice(id: Long): Flow<Office?> = dao.observeById(id)
    suspend fun getOffice(id: Long): Office? = dao.getById(id)
    suspend fun upsertOffice(office: Office): Long =
        if (office.id == 0L) dao.insert(office) else { dao.update(office); office.id }
    suspend fun deleteOffice(office: Office) = dao.delete(office)

    // --- Расходы ---
    fun observeExpenses(officeId: Long): Flow<List<OfficeExpense>> = dao.observeExpenses(officeId)
    suspend fun getExpense(id: Long): OfficeExpense? = dao.getExpenseById(id)
    fun observeTotal(officeId: Long): Flow<Double> = dao.observeTotal(officeId)
    fun observeTotalsPerOffice(): Flow<List<OfficeTotal>> = dao.observeTotalsPerOffice()
    fun observeMonthTotals(officeId: Long): Flow<List<MonthTotal>> =
        dao.observeMonthTotals(officeId)
    suspend fun getMonthTotals(officeId: Long): List<MonthTotal> = dao.getMonthTotals(officeId)
    fun observeCategorySums(officeId: Long): Flow<List<OfficeCategorySum>> =
        dao.observeCategorySums(officeId)
    suspend fun getCategorySums(officeId: Long): List<OfficeCategorySum> =
        dao.getCategorySums(officeId)
    suspend fun upsertExpense(expense: OfficeExpense): Long =
        if (expense.id == 0L) dao.insertExpense(expense)
        else { dao.updateExpense(expense); expense.id }
    suspend fun deleteExpense(expense: OfficeExpense) = dao.deleteExpense(expense)
}
