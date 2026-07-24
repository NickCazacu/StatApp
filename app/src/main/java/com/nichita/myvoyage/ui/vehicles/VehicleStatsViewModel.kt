package com.nichita.myvoyage.ui.vehicles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.db.MonthTotal
import com.nichita.myvoyage.data.db.VehicleCategorySum
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.model.Vehicle
import com.nichita.myvoyage.data.repository.RatesRepository
import com.nichita.myvoyage.data.repository.VehicleRepository
import com.nichita.myvoyage.domain.VehicleExpenseMath
import com.nichita.myvoyage.ui.nav.NavArgs
import com.nichita.myvoyage.ui.ratesRepository
import com.nichita.myvoyage.ui.vehicleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Состояние экрана статистики автомобиля. Все суммы — в валюте авто (курс НБМ). */
data class VehicleStatsState(
    val vehicle: Vehicle? = null,
    val total: Double = 0.0,
    /** Помесячные итоги в хронологическом порядке (для графика динамики). */
    val monthTotals: List<MonthTotal> = emptyList(),
    val categorySums: List<VehicleCategorySum> = emptyList()
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

/** ViewModel статистики автомобиля: динамика по месяцам и разбивка по категориям. */
class VehicleStatsViewModel(
    repository: VehicleRepository,
    ratesRepository: RatesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vehicleId: Long = savedStateHandle.get<Long>(NavArgs.VEHICLE_ID) ?: 0L

    val uiState: StateFlow<VehicleStatsState> =
        combine(
            repository.observeVehicle(vehicleId),
            repository.observeExpenses(vehicleId),
            ratesRepository.observeRates()
        ) { vehicle, expenses, rates ->
            val base = vehicle?.currency ?: Currency.MDL
            VehicleStatsState(
                vehicle = vehicle,
                total = VehicleExpenseMath.total(expenses, rates, base),
                monthTotals = VehicleExpenseMath.monthTotals(expenses, rates, base),
                categorySums = VehicleExpenseMath.categorySums(expenses, rates, base)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VehicleStatsState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                VehicleStatsViewModel(
                    vehicleRepository(),
                    ratesRepository(),
                    createSavedStateHandle()
                )
            }
        }
    }
}
