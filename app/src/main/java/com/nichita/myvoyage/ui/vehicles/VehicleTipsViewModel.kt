package com.nichita.myvoyage.ui.vehicles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.repository.VehicleRepository
import com.nichita.myvoyage.domain.Tip
import com.nichita.myvoyage.domain.TipSeverity
import com.nichita.myvoyage.domain.VehicleTipsAnalyzer
import com.nichita.myvoyage.domain.VehicleTipsInput
import com.nichita.myvoyage.ui.nav.NavArgs
import com.nichita.myvoyage.ui.vehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** ViewModel советов по автомобилю: собирает данные и запускает rule-based анализ. */
class VehicleTipsViewModel(
    private val repository: VehicleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vehicleId: Long = savedStateHandle.get<Long>(NavArgs.VEHICLE_ID) ?: 0L

    private val _tips = MutableStateFlow<List<Tip>>(emptyList())
    val tips: StateFlow<List<Tip>> = _tips.asStateFlow()

    init {
        viewModelScope.launch {
            val vehicle = repository.getVehicle(vehicleId)
            if (vehicle == null) {
                _tips.value = listOf(
                    Tip(
                        TipSeverity.INFO,
                        "Автомобиль не найден",
                        "Попробуйте открыть экран заново."
                    )
                )
                return@launch
            }
            val months = repository.getMonthTotals(vehicleId)
            if (months.isEmpty()) {
                _tips.value = listOf(
                    Tip(
                        TipSeverity.INFO,
                        "Пока нет данных",
                        "Добавьте расходы автомобиля хотя бы за один месяц — " +
                            "и здесь появятся советы по оптимизации."
                    )
                )
                return@launch
            }
            _tips.value = VehicleTipsAnalyzer.analyze(
                VehicleTipsInput(
                    vehicle = vehicle,
                    monthTotals = months,
                    categorySums = repository.getCategorySums(vehicleId)
                )
            )
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { VehicleTipsViewModel(vehicleRepository(), createSavedStateHandle()) }
        }
    }
}
