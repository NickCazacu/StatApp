package com.nichita.myvoyage.ui.vehicles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.model.Vehicle
import com.nichita.myvoyage.data.model.VehicleExpense
import com.nichita.myvoyage.data.repository.VehicleRepository
import com.nichita.myvoyage.ui.nav.NavArgs
import com.nichita.myvoyage.ui.vehicleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Расходы автомобиля за один месяц (для сгруппированного списка). */
data class VehicleMonthGroup(
    val year: Int,
    val month: Int,
    val total: Double,
    val items: List<VehicleExpense>
)

/** Состояние экрана деталей автомобиля. */
data class VehicleDetailState(
    val vehicle: Vehicle? = null,
    val total: Double = 0.0,
    val months: List<VehicleMonthGroup> = emptyList()
)

/** ViewModel деталей автомобиля: шапка, итог и расходы по месяцам. */
class VehicleDetailViewModel(
    private val repository: VehicleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vehicleId: Long = savedStateHandle.get<Long>(NavArgs.VEHICLE_ID) ?: 0L

    val uiState: StateFlow<VehicleDetailState> =
        combine(
            repository.observeVehicle(vehicleId),
            repository.observeExpenses(vehicleId),
            repository.observeTotal(vehicleId)
        ) { vehicle, expenses, total ->
            // Расходы приходят отсортированными (год/месяц по убыванию) —
            // группировка сохраняет этот порядок.
            val months = expenses
                .groupBy { it.year to it.month }
                .map { (key, items) ->
                    VehicleMonthGroup(
                        year = key.first,
                        month = key.second,
                        total = items.sumOf { it.amount },
                        items = items
                    )
                }
            VehicleDetailState(vehicle = vehicle, total = total, months = months)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VehicleDetailState())

    /** Удаляет автомобиль целиком (расходы удалятся каскадом). */
    fun deleteVehicle(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val vehicle = uiState.value.vehicle
                ?: repository.getVehicle(vehicleId)
                ?: return@launch
            repository.deleteVehicle(vehicle)
            onDeleted()
        }
    }

    fun deleteExpense(expense: VehicleExpense) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { VehicleDetailViewModel(vehicleRepository(), createSavedStateHandle()) }
        }
    }
}
