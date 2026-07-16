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

/** Доступ к офисам и их помесячным расходам. Агрегации считаются в SQL. */
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

    @Query("SELECT * FROM office_expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): OfficeExpense?

    /** Общая сумма расходов офиса за всё время. */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM office_expenses WHERE officeId = :officeId")
    fun observeTotal(officeId: Long): Flow<Double>

    /** Итоги по всем офисам, реактивно (для списка офисов). */
    @Query(
        """
        SELECT officeId AS officeId, SUM(amount) AS total
        FROM office_expenses
        GROUP BY officeId
        """
    )
    fun observeTotalsPerOffice(): Flow<List<OfficeTotal>>

    /** Помесячные итоги офиса, в хронологическом порядке (для графика динамики). */
    @Query(
        """
        SELECT year AS year, month AS month, SUM(amount) AS total
        FROM office_expenses
        WHERE officeId = :officeId
        GROUP BY year, month
        ORDER BY year ASC, month ASC
        """
    )
    fun observeMonthTotals(officeId: Long): Flow<List<MonthTotal>>

    /** То же разово (для анализа советов). */
    @Query(
        """
        SELECT year AS year, month AS month, SUM(amount) AS total
        FROM office_expenses
        WHERE officeId = :officeId
        GROUP BY year, month
        ORDER BY year ASC, month ASC
        """
    )
    suspend fun getMonthTotals(officeId: Long): List<MonthTotal>

    /** Разбивка расходов офиса по категориям (для графика/советов). */
    @Query(
        """
        SELECT category AS category, SUM(amount) AS total
        FROM office_expenses
        WHERE officeId = :officeId
        GROUP BY category
        ORDER BY total DESC
        """
    )
    fun observeCategorySums(officeId: Long): Flow<List<OfficeCategorySum>>

    /** То же разово (для анализа советов). */
    @Query(
        """
        SELECT category AS category, SUM(amount) AS total
        FROM office_expenses
        WHERE officeId = :officeId
        GROUP BY category
        ORDER BY total DESC
        """
    )
    suspend fun getCategorySums(officeId: Long): List<OfficeCategorySum>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: OfficeExpense): Long

    @Update
    suspend fun updateExpense(expense: OfficeExpense)

    @Delete
    suspend fun deleteExpense(expense: OfficeExpense)
}
