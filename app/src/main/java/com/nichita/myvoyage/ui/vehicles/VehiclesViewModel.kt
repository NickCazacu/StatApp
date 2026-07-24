package com.nichita.myvoyage.ui.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.model.Vehicle
import com.nichita.myvoyage.data.repository.RatesRepository
import com.nichita.myvoyage.data.repository.VehicleRepository
import com.nichita.myvoyage.ui.ratesRepository
import com.nichita.myvoyage.ui.vehicleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Автомобиль + сумма его расходов за всё время в валюте авто (по курсу НБМ). */
data class VehicleListItem(
    val vehicle: Vehicle,
    val total: Double
)

/** ViewModel вкладки «Авто»: список автомобилей с итогами в валюте авто. */
class VehiclesViewModel(
    repository: VehicleRepository,
    ratesRepository: RatesRepository
) : ViewModel() {

    val vehicles: StateFlow<List<VehicleListItem>> =
        combine(
            repository.observeVehicles(),
            repository.observeCurrencyTotalsPerVehicle(),
            ratesRepository.observeRates()
        ) { vehicles, totals, rates ->
            val byVehicle = totals.groupBy { it.vehicleId }
            vehicles.map { vehicle ->
                val total = byVehicle[vehicle.id].orEmpty()
                    .sumOf { rates.convert(it.total, it.currency, vehicle.currency) }
                VehicleListItem(vehicle, total)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer { VehiclesViewModel(vehicleRepository(), ratesRepository()) }
        }
    }
}
