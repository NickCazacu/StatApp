package com.nichita.myvoyage.ui.vehicles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.model.VehicleCategory
import com.nichita.myvoyage.data.model.VehicleExpense
import com.nichita.myvoyage.data.repository.VehicleRepository
import com.nichita.myvoyage.ui.nav.NavArgs
import com.nichita.myvoyage.ui.vehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

/** Состояние формы расхода автомобиля. */
data class VehicleExpenseFormState(
    val amount: String = "",
    val currency: Currency = Currency.MDL,
    val category: VehicleCategory = VehicleCategory.FUEL,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val note: String = "",
    val isEditing: Boolean = false
) {
    val isValid: Boolean get() = (amount.toDoubleOrNull() ?: 0.0) > 0.0
}

/**
 * ViewModel добавления/редактирования расхода автомобиля.
 * Аргументы навигации: vehicleId (обязателен) и itemId (0 — новый).
 */
class VehicleExpenseEditViewModel(
    private val repository: VehicleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vehicleId: Long = savedStateHandle.get<Long>(NavArgs.VEHICLE_ID) ?: 0L
    private val expenseId: Long = savedStateHandle.get<Long>(NavArgs.ITEM_ID) ?: 0L

    private val _form = MutableStateFlow(VehicleExpenseFormState())
    val form: StateFlow<VehicleExpenseFormState> = _form.asStateFlow()

    init {
        viewModelScope.launch {
            if (expenseId != 0L) {
                repository.getExpense(expenseId)?.let { e ->
                    _form.value = VehicleExpenseFormState(
                        amount = e.amount.toString(),
                        currency = e.currency,
                        category = e.category,
                        year = e.year,
                        month = e.month,
                        note = e.note,
                        isEditing = true
                    )
                }
            } else {
                // Для новой траты подставляем валюту автомобиля.
                repository.getVehicle(vehicleId)?.let { v ->
                    _form.update { it.copy(currency = v.currency) }
                }
            }
        }
    }

    fun onAmountChange(v: String) =
        _form.update { it.copy(amount = v.filter { c -> c.isDigit() || c == '.' }) }
    fun onCurrencyChange(v: Currency) = _form.update { it.copy(currency = v) }
    fun onCategoryChange(v: VehicleCategory) = _form.update { it.copy(category = v) }
    fun onYearChange(v: Int) = _form.update { it.copy(year = v) }
    fun onMonthChange(v: Int) = _form.update { it.copy(month = v) }
    fun onNoteChange(v: String) = _form.update { it.copy(note = v) }

    fun save(onDone: () -> Unit) {
        val s = _form.value
        if (!s.isValid) return
        viewModelScope.launch {
            repository.upsertExpense(
                VehicleExpense(
                    id = expenseId,
                    vehicleId = vehicleId,
                    year = s.year,
                    month = s.month,
                    category = s.category,
                    amount = s.amount.toDoubleOrNull() ?: 0.0,
                    currency = s.currency,
                    note = s.note.trim()
                )
            )
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                VehicleExpenseEditViewModel(vehicleRepository(), createSavedStateHandle())
            }
        }
    }
}
