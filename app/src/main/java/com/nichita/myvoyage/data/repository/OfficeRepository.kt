package com.nichita.myvoyage.data.repository

import com.nichita.myvoyage.data.db.OfficeCurrencyTotal
import com.nichita.myvoyage.data.db.OfficeDao
import com.nichita.myvoyage.data.model.Office
import com.nichita.myvoyage.data.model.OfficeExpense
import kotlinx.coroutines.flow.Flow

/**
 * Доступ к офисам и их помесячным расходам для ViewModel-слоя.
 * Свод разновалютных сумм в валюту офиса и советы — в domain/VM.
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
    suspend fun getExpenses(officeId: Long): List<OfficeExpense> = dao.getExpenses(officeId)
    suspend fun getExpense(id: Long): OfficeExpense? = dao.getExpenseById(id)
    fun observeCurrencyTotalsPerOffice(): Flow<List<OfficeCurrencyTotal>> =
        dao.observeCurrencyTotalsPerOffice()
    suspend fun upsertExpense(expense: OfficeExpense): Long =
        if (expense.id == 0L) dao.insertExpense(expense)
        else { dao.updateExpense(expense); expense.id }
    suspend fun deleteExpense(expense: OfficeExpense) = dao.deleteExpense(expense)
}
