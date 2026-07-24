package com.nichita.myvoyage.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.model.Trip
import com.nichita.myvoyage.data.repository.RatesRepository
import com.nichita.myvoyage.data.repository.VoyageRepository
import com.nichita.myvoyage.ui.ratesRepository
import com.nichita.myvoyage.ui.voyageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Элемент списка рейсов: рейс + уже потраченная сумма. */
data class TripListItem(
    val trip: Trip,
    val spent: Double
)

/**
 * ViewModel списка рейсов.
 * Объединяет поток рейсов с потоком итоговых сумм по рейсам.
 */
class TripsViewModel(
    private val repository: VoyageRepository,
    ratesRepository: RatesRepository
) : ViewModel() {

    /** Состояние экрана — список рейсов с потраченными суммами (расходы + топливо). */
    val trips: StateFlow<List<TripListItem>> =
        combine(
            repository.observeTrips(),
            repository.observeCurrencyTotalsPerTrip(),
            repository.observeFuelTotalsPerTrip(),
            ratesRepository.observeRates()
        ) { trips, currencyTotals, fuelTotals, rates ->
            trips.map { t ->
                // Разновалютные расходы и заправки сводим в валюту рейса по курсу.
                val expenseSpent = currencyTotals
                    .filter { it.tripId == t.id }
                    .sumOf { rates.convert(it.total, it.currency, t.currency) }
                val fuelSpent = fuelTotals
                    .filter { it.tripId == t.id }
                    .sumOf { rates.convert(it.total, it.currency, t.currency) }
                TripListItem(t, expenseSpent + fuelSpent)
            }
        }.stateIn(
            scope = viewModelScope,
            // Держим подписку 5с после ухода с экрана — переживает поворот экрана.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch { repository.deleteTrip(trip) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { TripsViewModel(voyageRepository(), ratesRepository()) }
        }
    }
}
