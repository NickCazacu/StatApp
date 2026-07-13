package com.nichita.myvoyage.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.model.Expense
import com.nichita.myvoyage.data.model.FuelEntry
import com.nichita.myvoyage.data.model.Trip
import com.nichita.myvoyage.data.repository.RatesRepository
import com.nichita.myvoyage.data.repository.VoyageRepository
import com.nichita.myvoyage.domain.CurrencyRates
import com.nichita.myvoyage.ui.nav.NavArgs
import com.nichita.myvoyage.ui.ratesRepository
import com.nichita.myvoyage.ui.voyageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Состояние экрана рейса (детали): сам рейс, расходы, заправки, итог.
 * [total] уже сведён в валюту рейса по курсу [rates] (разновалютные расходы
 * сконвертированы). [rates] отдаётся в UI, чтобы показывать пересчёт по строкам.
 */
data class TripDetailUiState(
    val trip: Trip? = null,
    val expenses: List<Expense> = emptyList(),
    val fuel: List<FuelEntry> = emptyList(),
    val total: Double = 0.0,
    val rates: CurrencyRates = CurrencyRates.FALLBACK
)

/**
 * ViewModel экрана конкретного рейса.
 * Реактивно собирает всё, что относится к рейсу, в одно состояние.
 */
class TripDetailViewModel(
    private val repository: VoyageRepository,
    ratesRepository: RatesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: Long = savedStateHandle.get<Long>(NavArgs.TRIP_ID) ?: 0L

    val uiState: StateFlow<TripDetailUiState> =
        combine(
            repository.observeTrip(tripId),
            repository.observeExpenses(tripId),
            repository.observeFuel(tripId),
            ratesRepository.observeRates()
        ) { trip, expenses, fuel, rates ->
            val tripCurrency = trip?.currency ?: Currency.DEFAULT
            // Расходы сводим в валюту рейса по курсу; заправки уже в валюте рейса.
            val expensesTotal = expenses.sumOf {
                rates.convert(it.amount, it.currency, tripCurrency)
            }
            val fuelCost = fuel.sumOf { it.cost }
            TripDetailUiState(trip, expenses, fuel, expensesTotal + fuelCost, rates)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TripDetailUiState()
        )

    /** Удаляет рейс целиком (расходы и заправки удалятся каскадом). */
    fun deleteTrip(onDeleted: () -> Unit) {
        val trip = uiState.value.trip ?: return
        viewModelScope.launch {
            repository.deleteTrip(trip)
            onDeleted()
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    fun deleteFuel(entry: FuelEntry) {
        viewModelScope.launch { repository.deleteFuel(entry) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                TripDetailViewModel(voyageRepository(), ratesRepository(), createSavedStateHandle())
            }
        }
    }
}
