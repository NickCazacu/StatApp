package com.nichita.myvoyage.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nichita.myvoyage.data.model.Expense
import kotlinx.coroutines.flow.Flow

/** Доступ к расходам. Агрегации считаются в SQL (быстрее, чем в Kotlin). */
@Dao
interface ExpenseDao {

    /** Расходы рейса, новые сверху. */
    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY date DESC")
    fun observeByTrip(tripId: Long): Flow<List<Expense>>

    /** Расходы рейса по дате (разово — для экспорта отчёта). */
    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY date ASC")
    suspend fun getByTrip(tripId: Long): List<Expense>

    /** Последние N расходов рейса (для компактных карточек/дашборда). */
    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY date DESC LIMIT :limit")
    fun observeRecent(tripId: Long, limit: Int): Flow<List<Expense>>

    /** Общая сумма расходов рейса (включая топливо). */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE tripId = :tripId")
    fun observeTotalForTrip(tripId: Long): Flow<Double>

    /** Разбивка расходов рейса по категориям (для графика). */
    @Query(
        """
        SELECT category AS category, SUM(amount) AS total
        FROM expenses
        WHERE tripId = :tripId
        GROUP BY category
        ORDER BY total DESC
        """
    )
    fun observeCategorySums(tripId: Long): Flow<List<CategorySum>>

    /** Итоги по всем рейсам, реактивно (для списка рейсов). */
    @Query(
        """
        SELECT tripId AS tripId, SUM(amount) AS total
        FROM expenses
        GROUP BY tripId
        """
    )
    fun observeTotalsPerTrip(): Flow<List<TripTotal>>

    /**
     * Итоги по всем рейсам в разрезе валюты расхода (для списка рейсов).
     * Разновалютные суммы сводятся в валюту рейса уже в Kotlin по курсу НБМ.
     */
    @Query(
        """
        SELECT tripId AS tripId, currency AS currency, SUM(amount) AS total
        FROM expenses
        GROUP BY tripId, currency
        """
    )
    fun observeCurrencyTotalsPerTrip(): Flow<List<TripCurrencyTotal>>

    /** То же разово — для советов/экспорта. */
    @Query(
        """
        SELECT tripId AS tripId, currency AS currency, SUM(amount) AS total
        FROM expenses
        GROUP BY tripId, currency
        """
    )
    suspend fun getCurrencyTotalsPerTrip(): List<TripCurrencyTotal>

    /** Разбивка расходов рейса по валюте и категории (для конвертации в валюту рейса). */
    @Query(
        """
        SELECT currency AS currency, category AS category, SUM(amount) AS total
        FROM expenses
        WHERE tripId = :tripId
        GROUP BY currency, category
        """
    )
    fun observeCurrencyCategorySumsForTrip(tripId: Long): Flow<List<CurrencyCategorySum>>

    /** То же разово (для советов). */
    @Query(
        """
        SELECT currency AS currency, category AS category, SUM(amount) AS total
        FROM expenses
        WHERE tripId = :tripId
        GROUP BY currency, category
        """
    )
    suspend fun getCurrencyCategorySumsForTrip(tripId: Long): List<CurrencyCategorySum>

    /** Разбивка по категориям одного рейса (разово, для анализа советов). */
    @Query(
        """
        SELECT category AS category, SUM(amount) AS total
        FROM expenses
        WHERE tripId = :tripId
        GROUP BY category
        """
    )
    suspend fun getCategorySumsForTrip(tripId: Long): List<CategorySum>

    /** Итоги по всем рейсам (для сравнения со средним). */
    @Query(
        """
        SELECT tripId AS tripId, SUM(amount) AS total
        FROM expenses
        GROUP BY tripId
        """
    )
    suspend fun getTotalsPerTrip(): List<TripTotal>

    /** Разбивка по категориям по всем рейсам (для анализа "что дороже всего"). */
    @Query(
        """
        SELECT category AS category, SUM(amount) AS total
        FROM expenses
        GROUP BY category
        ORDER BY total DESC
        """
    )
    suspend fun getCategorySumsAllTrips(): List<CategorySum>

    /**
     * Разбивка по категориям в разрезе валюты рейса (для сводной статистики).
     * Валюта берётся у рейса (основная), суммы по разным валютам не смешиваются.
     */
    @Query(
        """
        SELECT t.currency AS currency, e.category AS category, SUM(e.amount) AS total
        FROM expenses e
        JOIN trips t ON e.tripId = t.id
        GROUP BY t.currency, e.category
        """
    )
    fun observeCategorySumsByCurrency(): Flow<List<CurrencyCategorySum>>

    /** То же разово (для советов — свод категорий по всем рейсам в общую валюту). */
    @Query(
        """
        SELECT t.currency AS currency, e.category AS category, SUM(e.amount) AS total
        FROM expenses e
        JOIN trips t ON e.tripId = t.id
        GROUP BY t.currency, e.category
        """
    )
    suspend fun getCategorySumsByCurrency(): List<CurrencyCategorySum>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Expense?
}
