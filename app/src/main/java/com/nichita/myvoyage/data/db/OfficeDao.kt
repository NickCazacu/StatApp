package com.nichita.myvoyage.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nichita.myvoyage.data.model.Office
import com.nichita.myvoyage.data.model.OfficeExpense
import kotlinx.coroutines.flow.Flow

/**
 * Доступ к офисам и их помесячным расходам.
 *
 * Суммы в SQL считаются только в разрезе валюты траты: складывать разные
 * валюты нельзя, свод в валюту офиса делается в Kotlin по курсу НБМ.
 */
@Dao
interface OfficeDao {

    // --- Офисы ---

    @Query("SELECT * FROM offices ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Office>>

    @Query("SELECT * FROM offices WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<Office?>

    @Query("SELECT * FROM offices WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Office?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(office: Office): Long

    @Update
    suspend fun update(office: Office)

    @Delete
    suspend fun delete(office: Office)

    // --- Расходы офиса ---

    /** Расходы офиса, свежие месяцы сверху. */
    @Query(
        """
        SELECT * FROM office_expenses
        WHERE officeId = :officeId
        ORDER BY year DESC, month DESC, id DESC
        """
    )
    fun observeExpenses(officeId: Long): Flow<List<OfficeExpense>>

    /** То же разово (для советов/экспорта). */
    @Query(
        """
        SELECT * FROM office_expenses
        WHERE officeId = :officeId
        ORDER BY year DESC, month DESC, id DESC
        """
    )
    suspend fun getExpenses(officeId: Long): List<OfficeExpense>

    @Query("SELECT * FROM office_expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): OfficeExpense?

    /**
     * Итоги по всем офисам в разрезе валюты траты (для списка офисов).
     * Разновалютные суммы сводятся в валюту офиса уже в Kotlin по курсу НБМ.
     */
    @Query(
        """
        SELECT officeId AS officeId, currency AS currency, SUM(amount) AS total
        FROM office_expenses
        GROUP BY officeId, currency
        """
    )
    fun observeCurrencyTotalsPerOffice(): Flow<List<OfficeCurrencyTotal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: OfficeExpense): Long

    @Update
    suspend fun updateExpense(expense: OfficeExpense)

    @Delete
    suspend fun deleteExpense(expense: OfficeExpense)
}
