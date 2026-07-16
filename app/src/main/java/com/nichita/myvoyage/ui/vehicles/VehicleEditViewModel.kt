package com.nichita.myvoyage.ui.vehicles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.model.Vehicle
import com.nichita.myvoyage.data.repository.VehicleRepository
import com.nichita.myvoyage.ui.nav.NavArgs
import com.nichita.myvoyage.ui.vehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Состояние формы автомобиля. */
data class VehicleFormState(
    val name: String = "",
    val plate: String = "",
    val currency: Currency = Currency.MDL,
    val isEditing: Boolean = false
) {
    val isValid: Boolean get() = name.isNotBlank()
}

/**
 * ViewModel добавления/редактирования автомобиля.
 * Аргумент навигации: vehicleId (0 — новый).
 */
class VehicleEditViewModel(
    private val repository: VehicleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vehicleId: Long = savedStateHandle.get<Long>(NavArgs.VEHICLE_ID) ?: 0L

    private val _form = MutableStateFlow(VehicleFormState())
    val form: StateFlow<VehicleFormState> = _form.asStateFlow()

    init {
        if (vehicleId != 0L) {
            viewModelScope.launch {
                repository.getVehicle(vehicleId)?.let { v ->
                    _form.value = VehicleFormState(
                        name = v.name,
                        plate = v.plate,
                        currency = v.currency,
                        isEditing = true
                    )
                }
            }
        }
    }

    fun onNameChange(v: String) = _form.update { it.copy(name = v) }
    fun onPlateChange(v: String) = _form.update { it.copy(plate = v) }
    fun onCurrencyChange(v: Currency) = _form.update { it.copy(currency = v) }

    fun save(onDone: () -> Unit) {
        val s = _form.value
        if (!s.isValid) return
        viewModelScope.launch {
            repository.upsertVehicle(
                Vehicle(
                    id = vehicleId,
                    name = s.name.trim(),
                    plate = s.plate.trim(),
                    currency = s.currency
                )
            )
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { VehicleEditViewModel(vehicleRepository(), createSavedStateHandle()) }
        }
    }
}
