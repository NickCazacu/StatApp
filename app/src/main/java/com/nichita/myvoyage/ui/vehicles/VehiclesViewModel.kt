package com.nichita.myvoyage.ui.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.model.Vehicle
import com.nichita.myvoyage.data.repository.VehicleRepository
import com.nichita.myvoyage.ui.vehicleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Автомобиль + сумма его расходов за всё время (для карточки в списке). */
data class VehicleListItem(
    val vehicle: Vehicle,
    val total: Double
)

/** ViewModel вкладки «Авто»: список автомобилей с итогами. */
class VehiclesViewModel(repository: VehicleRepository) : ViewModel() {

    val vehicles: StateFlow<List<VehicleListItem>> =
        combine(
            repository.observeVehicles(),
            repository.observeTotalsPerVehicle()
        ) { vehicles, totals ->
            val byId = totals.associateBy({ it.vehicleId }, { it.total })
            vehicles.map { VehicleListItem(it, byId[it.id] ?: 0.0) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer { VehiclesViewModel(vehicleRepository()) }
        }
    }
}
