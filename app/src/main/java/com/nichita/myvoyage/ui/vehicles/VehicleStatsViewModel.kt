package com.nichita.myvoyage.ui.vehicles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.db.MonthTotal
import com.nichita.myvoyage.data.db.VehicleCategorySum
import com.nichita.myvoyage.data.model.Vehicle
import com.nichita.myvoyage.data.repository.VehicleRepository
import com.nichita.myvoyage.ui.nav.NavArgs
import com.nichita.myvoyage.ui.vehicleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Состояние экрана статистики автомобиля. */
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vehicleId: Long = savedStateHandle.get<Long>(NavArgs.VEHICLE_ID) ?: 0L

    val uiState: StateFlow<VehicleStatsState> =
        combine(
            repository.observeVehicle(vehicleId),
            repository.observeTotal(vehicleId),
            repository.observeMonthTotals(vehicleId),
            repository.observeCategorySums(vehicleId)
        ) { vehicle, total, months, categories ->
            VehicleStatsState(
                vehicle = vehicle,
                total = total,
                monthTotals = months,
                categorySums = categories
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VehicleStatsState())

    companion object {
        val Factory = viewModelFactory {
            initializer { VehicleStatsViewModel(vehicleRepository(), createSavedStateHandle()) }
        }
    }
}
