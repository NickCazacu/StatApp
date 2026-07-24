package com.nichita.myvoyage.ui.offices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.model.OfficeCategory
import com.nichita.myvoyage.data.model.OfficeExpense
import com.nichita.myvoyage.data.repository.OfficeRepository
import com.nichita.myvoyage.ui.nav.NavArgs
import com.nichita.myvoyage.ui.officeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

/** Состояние формы расхода офиса. */
data class OfficeExpenseFormState(
    val amount: String = "",
    val currency: Currency = Currency.MDL,
    val category: OfficeCategory = OfficeCategory.RENT,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val note: String = "",
    val isEditing: Boolean = false
) {
    val isValid: Boolean get() = (amount.toDoubleOrNull() ?: 0.0) > 0.0
}

/**
 * ViewModel добавления/редактирования расхода офиса.
 * Аргументы навигации: officeId (обязателен) и itemId (0 — новый).
 */
class OfficeExpenseEditViewModel(
    private val repository: OfficeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val officeId: Long = savedStateHandle.get<Long>(NavArgs.OFFICE_ID) ?: 0L
    private val expenseId: Long = savedStateHandle.get<Long>(NavArgs.ITEM_ID) ?: 0L

    private val _form = MutableStateFlow(OfficeExpenseFormState())
    val form: StateFlow<OfficeExpenseFormState> = _form.asStateFlow()

    init {
        viewModelScope.launch {
            if (expenseId != 0L) {
                repository.getExpense(expenseId)?.let { e ->
                    _form.value = OfficeExpenseFormState(
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
                // Для новой траты подставляем валюту офиса.
                repository.getOffice(officeId)?.let { o ->
                    _form.update { it.copy(currency = o.currency) }
                }
            }
        }
    }

    fun onAmountChange(v: String) =
        _form.update { it.copy(amount = v.filter { c -> c.isDigit() || c == '.' }) }
    fun onCurrencyChange(v: Currency) = _form.update { it.copy(currency = v) }
    fun onCategoryChange(v: OfficeCategory) = _form.update { it.copy(category = v) }
    fun onYearChange(v: Int) = _form.update { it.copy(year = v) }
    fun onMonthChange(v: Int) = _form.update { it.copy(month = v) }
    fun onNoteChange(v: String) = _form.update { it.copy(note = v) }

    fun save(onDone: () -> Unit) {
        val s = _form.value
        if (!s.isValid) return
        viewModelScope.launch {
            repository.upsertExpense(
                OfficeExpense(
                    id = expenseId,
                    officeId = officeId,
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
                OfficeExpenseEditViewModel(officeRepository(), createSavedStateHandle())
            }
        }
    }
}
