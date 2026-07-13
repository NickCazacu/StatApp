package com.nichita.myvoyage.ui.stats

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
import com.nichita.myvoyage.data.model.Trip
import com.nichita.myvoyage.data.repository.RatesRepository
import com.nichita.myvoyage.data.repository.VoyageRepository
import com.nichita.myvoyage.domain.CurrencyRates
import com.nichita.myvoyage.domain.FuelCalculator
import com.nichita.myvoyage.domain.FuelStats
import com.nichita.myvoyage.ui.nav.NavArgs
import com.nichita.myvoyage.ui.ratesRepository
import com.nichita.myvoyage.ui.voyageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Состояние экрана статистики одного рейса. Все суммы — в валюте рейса. */
data class StatsUiState(
    val trip: Trip? = null,
    /** Итог рейса: расходы + топливо (в валюте рейса). */
    val total: Double = 0.0,
    /** Разбивка по категориям (топливо включено), по убыванию. */
    val categorySums: List<CategorySum> = emptyList(),
    /** Расчёт по топливу. */
    val fuelStats: FuelStats = FuelStats(),
    /** Средний итог по прошлым рейсам, сведённый в валюту текущего рейса (null — нет данных). */
    val avgOtherTotal: Double? = null,
    /** Отклонение текущего рейса от среднего (доля, напр. 0.2 = +20%). */
    val diffVsAvg: Double? = null
)

/**
 * ViewModel статистики: всё считается реактивно из Room-потоков.
 * Разновалютные расходы конвертируются в валюту рейса по курсу НБМ, а средний
 * итог других рейсов приводится к валюте текущего рейса — чтобы сравнение было
 * корректным даже если рейсы велись в разных валютах.
 */
class StatsViewModel(
    repository: VoyageRepository,
    ratesRepository: RatesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: Long = savedStateHandle.get<Long>(NavArgs.TRIP_ID) ?: 0L

    /** Свёрнутые данные текущего рейса (в его валюте). */
    private data class Current(
        val trip: Trip?,
        val total: Double,
        val categorySums: List<CategorySum>,
        val fuelStats: FuelStats,
        val rates: CurrencyRates
    )

    // Часть состояния по текущему рейсу.
    private val current = combine(
        repository.observeTrip(tripId),
        repository.observeCurrencyCategorySumsForTrip(tripId),
        repository.observeFuel(tripId),
        ratesRepository.observeRates()
    ) { trip, currencyCatSums, fuel, rates ->
        val tripCurrency = trip?.currency ?: Currency.DEFAULT
        val fuelStats = FuelCalculator.calculate(fuel)
        val expenseCatSums = convertCategorySums(currencyCatSums, tripCurrency, rates)
        val categorySums = mergeFuelIntoCategories(expenseCatSums, fuelStats.totalFuelCost)
        val total = categorySums.sumOf { it.total }
        Current(trip, total, categorySums, fuelStats, rates)
    }

    // Историческая часть: итог каждого рейса в ЕГО валюте (для сравнения после
    // приведения к валюте текущего рейса).
    private data class History(
        /** tripId → (валюта рейса, итог в этой валюте). */
        val totalsByTrip: Map<Long, Pair<Currency, Double>>
    )

    private val history = combine(
        repository.observeCurrencyTotalsPerTrip(),
        repository.observeFuelTotalsPerTrip(),
        repository.observeTrips(),
        ratesRepository.observeRates()
    ) { currencyTotals, fuelTotals, trips, rates ->
        val currencyById = trips.associate { it.id to it.currency }
        val totals = HashMap<Long, Double>()
        currencyTotals.forEach { row ->
            val own = currencyById[row.tripId] ?: return@forEach
            totals[row.tripId] = (totals[row.tripId] ?: 0.0) +
                rates.convert(row.total, row.currency, own)
        }
        // Топливо уже в валюте рейса.
        fuelTotals.forEach { totals[it.tripId] = (totals[it.tripId] ?: 0.0) + it.total }
        History(totals.mapValues { (id, value) -> (currencyById[id] ?: Currency.DEFAULT) to value })
    }

    val uiState: StateFlow<StatsUiState> =
        combine(current, history) { cur, hist ->
            val currentCurrency = cur.trip?.currency ?: Currency.DEFAULT

            // Итоги других рейсов приводим к валюте текущего рейса.
            val others = hist.totalsByTrip
                .filterKeys { it != tripId }
                .map { (_, ownCurrencyAndTotal) ->
                    val (own, value) = ownCurrencyAndTotal
                    cur.rates.convert(value, own, currentCurrency)
                }
                .filter { it > 0.0 }
            val avg = others.takeIf { it.isNotEmpty() }?.average()
            val diff = if (avg != null && avg > 0.0) (cur.total - avg) / avg else null

            StatsUiState(
                trip = cur.trip,
                total = cur.total,
                categorySums = cur.categorySums,
                fuelStats = cur.fuelStats,
                avgOtherTotal = avg,
                diffVsAvg = diff
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsUiState()
        )

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

    /** Добавляет стоимость заправок в категорию «Топливо» и сортирует по убыванию. */
    private fun mergeFuelIntoCategories(
        expenseSums: List<CategorySum>,
        fuelCost: Double
    ): List<CategorySum> {
        if (fuelCost <= 0.0) return expenseSums.sortedByDescending { it.total }
        val map = expenseSums.associate { it.category to it.total }.toMutableMap()
        map[Category.FUEL] = (map[Category.FUEL] ?: 0.0) + fuelCost
        return map.map { CategorySum(it.key, it.value) }.sortedByDescending { it.total }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                StatsViewModel(voyageRepository(), ratesRepository(), createSavedStateHandle())
            }
        }
    }
}
