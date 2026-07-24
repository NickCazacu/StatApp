package com.nichita.myvoyage.ui.tips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.db.CategorySum
import com.nichita.myvoyage.data.model.Category
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.repository.RatesRepository
import com.nichita.myvoyage.data.repository.VoyageRepository
import com.nichita.myvoyage.domain.NamedTripTotal
import com.nichita.myvoyage.domain.OverallTipsAnalyzer
import com.nichita.myvoyage.domain.OverallTipsInput
import com.nichita.myvoyage.domain.Tip
import com.nichita.myvoyage.ui.ratesRepository
import com.nichita.myvoyage.ui.voyageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Советы по одной валюте (рейсы этой валюты). */
data class TipsSection(
    val currency: Currency,
    val tips: List<Tip>
)

/** Состояние вкладки «Советы» (по всем рейсам). */
data class OverallTipsUiState(
    val sections: List<TipsSection> = emptyList(),
    val hasTrips: Boolean = false
)

/**
 * ViewModel сводных советов по ВСЕМ рейсам.
 * Реактивно агрегирует расходы по валютам и прогоняет через rule-based
 * [OverallTipsAnalyzer]. Валюты не смешиваются.
 */
class OverallTipsViewModel(
    repository: VoyageRepository,
    ratesRepository: RatesRepository
) : ViewModel() {

    val uiState: StateFlow<OverallTipsUiState> =
        combine(
            repository.observeTrips(),
            repository.observeCurrencyTotalsPerTrip(),
            repository.observeFuelTotalsPerTrip(),
            repository.observeCategorySumsByCurrency(),
            ratesRepository.observeRates()
        ) { trips, expenseTotals, fuelTotals, catByCurrency, rates ->
            // Итоги/количество/топливо и список рейсов — по каждой валюте.
            val totalByCurrency = HashMap<Currency, Double>()
            val countByCurrency = HashMap<Currency, Int>()
            val fuelByCurrency = HashMap<Currency, Double>()
            val tripsByCurrency = HashMap<Currency, MutableList<NamedTripTotal>>()
            trips.forEach { t ->
                // Разновалютные расходы и заправки рейса сводим в его валюту по курсу.
                val expenses = expenseTotals
                    .filter { it.tripId == t.id }
                    .sumOf { rates.convert(it.total, it.currency, t.currency) }
                val fuel = fuelTotals
                    .filter { it.tripId == t.id }
                    .sumOf { rates.convert(it.total, it.currency, t.currency) }
                val spent = expenses + fuel
                totalByCurrency[t.currency] = (totalByCurrency[t.currency] ?: 0.0) + spent
                countByCurrency[t.currency] = (countByCurrency[t.currency] ?: 0) + 1
                fuelByCurrency[t.currency] = (fuelByCurrency[t.currency] ?: 0.0) + fuel
                tripsByCurrency.getOrPut(t.currency) { mutableListOf() }
                    .add(NamedTripTotal(t.destination, spent))
            }

            // Разбивка по категориям (расходы) в разрезе валюты.
            val categoriesByCurrency: Map<Currency, MutableMap<Category, Double>> =
                catByCurrency.groupBy { it.currency }.mapValues { (_, rows) ->
                    rows.associate { it.category to it.total }.toMutableMap()
                }

            val sections = countByCurrency.keys.map { currency ->
                val cats = categoriesByCurrency[currency] ?: mutableMapOf()
                val fuelCost = fuelByCurrency[currency] ?: 0.0
                if (fuelCost > 0.0) cats[Category.FUEL] = (cats[Category.FUEL] ?: 0.0) + fuelCost
                val categorySums = cats.map { CategorySum(it.key, it.value) }
                    .filter { it.total > 0.0 }
                    .sortedByDescending { it.total }

                val tips = OverallTipsAnalyzer.analyze(
                    OverallTipsInput(
                        currency = currency,
                        tripCount = countByCurrency[currency] ?: 0,
                        total = totalByCurrency[currency] ?: 0.0,
                        categorySums = categorySums,
                        tripTotals = tripsByCurrency[currency] ?: emptyList()
                    )
                )
                TipsSection(currency, tips)
            }
                .filter { it.tips.isNotEmpty() }
                .sortedByDescending { section -> totalByCurrency[section.currency] ?: 0.0 }

            OverallTipsUiState(sections = sections, hasTrips = trips.isNotEmpty())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OverallTipsUiState()
        )

    companion object {
        val Factory = viewModelFactory {
            initializer { OverallTipsViewModel(voyageRepository(), ratesRepository()) }
        }
    }
}
