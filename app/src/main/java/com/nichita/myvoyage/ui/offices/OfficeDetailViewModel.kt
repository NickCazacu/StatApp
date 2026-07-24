package com.nichita.myvoyage.ui.offices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.model.Office
import com.nichita.myvoyage.data.model.OfficeExpense
import com.nichita.myvoyage.data.repository.OfficeRepository
import com.nichita.myvoyage.data.repository.RatesRepository
import com.nichita.myvoyage.domain.CurrencyRates
import com.nichita.myvoyage.domain.OfficeExpenseMath
import com.nichita.myvoyage.ui.nav.NavArgs
import com.nichita.myvoyage.ui.officeRepository
import com.nichita.myvoyage.ui.ratesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Расходы офиса за один месяц; [total] — в валюте офиса по курсу НБМ. */
data class MonthGroup(
    val year: Int,
    val month: Int,
    val total: Double,
    val items: List<OfficeExpense>
)

/** Состояние экрана деталей офиса. [total] — в валюте офиса по курсу НБМ. */
data class OfficeDetailState(
    val office: Office? = null,
    val total: Double = 0.0,
    val months: List<MonthGroup> = emptyList(),
    /** Курсы для показа пересчёта «≈ …» у трат в чужой валюте. */
    val rates: CurrencyRates = CurrencyRates.FALLBACK
)

/** ViewModel деталей офиса: шапка, итог и расходы, сгруппированные по месяцам. */
class OfficeDetailViewModel(
    private val repository: OfficeRepository,
    ratesRepository: RatesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val officeId: Long = savedStateHandle.get<Long>(NavArgs.OFFICE_ID) ?: 0L

    val uiState: StateFlow<OfficeDetailState> =
        combine(
            repository.observeOffice(officeId),
            repository.observeExpenses(officeId),
            ratesRepository.observeRates()
        ) { office, expenses, rates ->
            val base = office?.currency ?: Currency.MDL
            // Расходы приходят отсортированными (год/месяц по убыванию) —
            // группировка сохраняет этот порядок.
            val months = expenses
                .groupBy { it.year to it.month }
                .map { (key, items) ->
                    MonthGroup(
                        year = key.first,
                        month = key.second,
                        total = OfficeExpenseMath.total(items, rates, base),
                        items = items
                    )
                }
            OfficeDetailState(
                office = office,
                total = OfficeExpenseMath.total(expenses, rates, base),
                months = months,
                rates = rates
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OfficeDetailState())

    /** Удаляет офис целиком (расходы удалятся каскадом). */
    fun deleteOffice(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val office = uiState.value.office ?: repository.getOffice(officeId) ?: return@launch
            repository.deleteOffice(office)
            onDeleted()
        }
    }

    fun deleteExpense(expense: OfficeExpense) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                OfficeDetailViewModel(
                    officeRepository(),
                    ratesRepository(),
                    createSavedStateHandle()
                )
            }
        }
    }
}
