package com.nichita.myvoyage.ui.offices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.db.MonthTotal
import com.nichita.myvoyage.data.db.OfficeCategorySum
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.model.Office
import com.nichita.myvoyage.data.repository.OfficeRepository
import com.nichita.myvoyage.data.repository.RatesRepository
import com.nichita.myvoyage.domain.OfficeExpenseMath
import com.nichita.myvoyage.ui.nav.NavArgs
import com.nichita.myvoyage.ui.officeRepository
import com.nichita.myvoyage.ui.ratesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Состояние экрана статистики офиса. Все суммы — в валюте офиса (курс НБМ). */
data class OfficeStatsState(
    val office: Office? = null,
    val total: Double = 0.0,
    /** Помесячные итоги в хронологическом порядке (для графика динамики). */
    val monthTotals: List<MonthTotal> = emptyList(),
    val categorySums: List<OfficeCategorySum> = emptyList()
) {
    /** Средние расходы за месяц (по месяцам, где были траты). */
    val avgPerMonth: Double?
        get() = monthTotals.filter { it.total > 0.0 }
            .takeIf { it.isNotEmpty() }
            ?.map { it.total }
            ?.average()

    /** Отклонение последнего месяца от среднего по остальным (доля, напр. 0.15). */
    val lastVsAvg: Double?
        get() {
            if (monthTotals.size < 2) return null
            val last = monthTotals.last()
            val others = monthTotals.dropLast(1).map { it.total }.filter { it > 0.0 }
            if (others.isEmpty()) return null
            val avg = others.average()
            if (avg <= 0.0) return null
            return (last.total - avg) / avg
        }
}

/** ViewModel статистики офиса: динамика по месяцам и разбивка по категориям. */
class OfficeStatsViewModel(
    repository: OfficeRepository,
    ratesRepository: RatesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val officeId: Long = savedStateHandle.get<Long>(NavArgs.OFFICE_ID) ?: 0L

    val uiState: StateFlow<OfficeStatsState> =
        combine(
            repository.observeOffice(officeId),
            repository.observeExpenses(officeId),
            ratesRepository.observeRates()
        ) { office, expenses, rates ->
            val base = office?.currency ?: Currency.MDL
            OfficeStatsState(
                office = office,
                total = OfficeExpenseMath.total(expenses, rates, base),
                monthTotals = OfficeExpenseMath.monthTotals(expenses, rates, base),
                categorySums = OfficeExpenseMath.categorySums(expenses, rates, base)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OfficeStatsState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                OfficeStatsViewModel(
                    officeRepository(),
                    ratesRepository(),
                    createSavedStateHandle()
                )
            }
        }
    }
}
