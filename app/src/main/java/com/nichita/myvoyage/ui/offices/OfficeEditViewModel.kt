package com.nichita.myvoyage.ui.offices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.model.Office
import com.nichita.myvoyage.data.repository.OfficeRepository
import com.nichita.myvoyage.ui.nav.NavArgs
import com.nichita.myvoyage.ui.officeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Состояние формы офиса. */
data class OfficeFormState(
    val name: String = "",
    val address: String = "",
    val currency: Currency = Currency.MDL,
    val isEditing: Boolean = false
) {
    val isValid: Boolean get() = name.isNotBlank()
}

/**
 * ViewModel добавления/редактирования офиса.
 * Аргумент навигации: officeId (0 — новый).
 */
class OfficeEditViewModel(
    private val repository: OfficeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val officeId: Long = savedStateHandle.get<Long>(NavArgs.OFFICE_ID) ?: 0L

    private val _form = MutableStateFlow(OfficeFormState())
    val form: StateFlow<OfficeFormState> = _form.asStateFlow()

    init {
        if (officeId != 0L) {
            viewModelScope.launch {
                repository.getOffice(officeId)?.let { o ->
                    _form.value = OfficeFormState(
                        name = o.name,
                        address = o.address,
                        currency = o.currency,
                        isEditing = true
                    )
                }
            }
        }
    }

    fun onNameChange(v: String) = _form.update { it.copy(name = v) }
    fun onAddressChange(v: String) = _form.update { it.copy(address = v) }
    fun onCurrencyChange(v: Currency) = _form.update { it.copy(currency = v) }

    fun save(onDone: () -> Unit) {
        val s = _form.value
        if (!s.isValid) return
        viewModelScope.launch {
            repository.upsertOffice(
                Office(
                    id = officeId,
                    name = s.name.trim(),
                    address = s.address.trim(),
                    currency = s.currency
                )
            )
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { OfficeEditViewModel(officeRepository(), createSavedStateHandle()) }
        }
    }
}
