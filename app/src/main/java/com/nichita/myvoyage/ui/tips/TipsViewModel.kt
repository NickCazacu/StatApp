package com.nichita.myvoyage.ui.tips

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.db.CategorySum
import com.nichita.myvoyage.data.db.CurrencyCategorySum
import com.nichita.myvoyage.data.model.Category
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.repository.RatesRepository
import com.nichita.myvoyage.data.repository.VoyageRepository
import com.nichita.myvoyage.domain.CurrencyRates
import com.nichita.myvoyage.domain.FuelCalculator
import com.nichita.myvoyage.domain.Tip
import com.nichita.myvoyage.domain.TipsAnalyzer
import com.nichita.myvoyage.domain.TipsInput
import com.nichita.myvoyage.ui.nav.NavArgs
import com.nichita.myvoyage.ui.ratesRepository
import com.nichita.myvoyage.ui.voyageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel раздела «Советы».
 * Собирает снимок данных по рейсу и истории, прогоняет через rule-based
 * [TipsAnalyzer] и отдаёт список советов. Пересчёт — по запросу/при входе.
 */
class TipsViewModel(
    private val repository: VoyageRepository,
    private val ratesRepository: RatesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: Long = savedStateHandle.get<Long>(NavArgs.TRIP_ID) ?: 0L

    private val _tips = MutableStateFlow<List<Tip>>(emptyList())
    val tips: StateFlow<List<Tip>> = _tips.asStateFlow()

    init {
        analyze()
    }

    fun analyze() {
        viewModelScope.launch {
            val trip = repository.getTrip(tripId) ?: return@launch
            val tripCurrency = trip.currency
            // Все суммы приводим к валюте текущего рейса по курсу НБМ.
            val rates = ratesRepository.currentRates()

            // --- Текущий рейс ---
            val currentFuel = repository.getFuelForTrip(tripId)
            val currentFuelStats = FuelCalculator.calculate(currentFuel)
            val currentExpenseCatSums = convertCategorySums(
                repository.getCurrencyCategorySumsForTrip(tripId), tripCurrency, rates
            )
            val currentCatSums = mergeFuel(currentExpenseCatSums, currentFuelStats.totalFuelCost)
            val currentTotal = currentCatSums.sumOf { it.total }

            // --- История (другие рейсы), итоги в валюте текущего рейса ---
            val tripCurrencyById = repository.getAllTrips().associate { it.id to it.currency }
            val fuelTotals = repository.getFuelTotalsPerTrip().associate { it.tripId to it.total }
            val combinedTotals = HashMap<Long, Double>()
            // Расходы: конвертируем каждую валютную группу рейса в валюту текущего рейса.
            repository.getCurrencyTotalsPerTrip().forEach { row ->
                combinedTotals[row.tripId] = (combinedTotals[row.tripId] ?: 0.0) +
                    rates.convert(row.total, row.currency, tripCurrency)
            }
            // Топливо хранится в валюте своего рейса — переводим в валюту текущего.
            fuelTotals.forEach { (id, cost) ->
                val own = tripCurrencyById[id] ?: tripCurrency
                combinedTotals[id] = (combinedTotals[id] ?: 0.0) +
                    rates.convert(cost, own, tripCurrency)
            }
            val otherTotals = combinedTotals.filterKeys { it != tripId }.values.toList()

            // Категории по всем рейсам (с учётом топлива) — для правила "топливо дороже всего".
            val allExpenseCatSums = convertCategorySums(
                repository.getCategorySumsByCurrency(), tripCurrency, rates
            )
            val allFuelCost = fuelTotals.entries.sumOf { (id, cost) ->
                rates.convert(cost, tripCurrencyById[id] ?: tripCurrency, tripCurrency)
            }
            val allCatSums = mergeFuel(allExpenseCatSums, allFuelCost)

            _tips.value = TipsAnalyzer.analyze(
                TipsInput(
                    trip = trip,
                    currentTotal = currentTotal,
                    currentCategorySums = currentCatSums,
                    otherTripTotals = otherTotals,
                    categorySumsAllTrips = allCatSums
                )
            )
        }
    }

    /** Сводит разновалютные суммы по категориям в валюту рейса. */
    private fun convertCategorySums(
        rows: List<CurrencyCategorySum>,
        tripCurrency: Currency,
        rates: CurrencyRates
    ): List<CategorySum> {
        val byCategory = HashMap<Category, Double>()
        rows.forEach { row ->
            byCategory[row.category] = (byCategory[row.category] ?: 0.0) +
                rates.convert(row.total, row.currency, tripCurrency)
        }
        return byCategory.map { CategorySum(it.key, it.value) }
    }

    /** Складывает стоимость топлива в категорию FUEL. */
    private fun mergeFuel(expenseSums: List<CategorySum>, fuelCost: Double): List<CategorySum> {
        if (fuelCost <= 0.0) return expenseSums
        val map = expenseSums.associate { it.category to it.total }.toMutableMap()
        map[Category.FUEL] = (map[Category.FUEL] ?: 0.0) + fuelCost
        return map.map { CategorySum(it.key, it.value) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                TipsViewModel(voyageRepository(), ratesRepository(), createSavedStateHandle())
            }
        }
    }
}
