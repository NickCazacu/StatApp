package com.nichita.myvoyage.ui.export

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.model.Category
import com.nichita.myvoyage.data.model.Expense
import com.nichita.myvoyage.data.model.FuelEntry
import com.nichita.myvoyage.data.model.Trip
import com.nichita.myvoyage.data.repository.RatesRepository
import com.nichita.myvoyage.data.repository.VoyageRepository
import com.nichita.myvoyage.domain.CurrencyRates
import com.nichita.myvoyage.export.ReportTable
import com.nichita.myvoyage.export.TripReport
import com.nichita.myvoyage.ui.nav.NavArgs
import com.nichita.myvoyage.ui.ratesRepository
import com.nichita.myvoyage.ui.voyageRepository
import com.nichita.myvoyage.util.Format
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Готовит подробный отчёт по рейсу для экспорта в PDF/Word. */
class ExportViewModel(
    private val repository: VoyageRepository,
    private val ratesRepository: RatesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: Long = savedStateHandle.get<Long>(NavArgs.TRIP_ID) ?: 0L

    private val _report = MutableStateFlow<TripReport?>(null)
    val report: StateFlow<TripReport?> = _report.asStateFlow()

    init {
        viewModelScope.launch {
            val trip = repository.getTrip(tripId) ?: return@launch
            val expenses = repository.getExpensesForTrip(tripId)
            val fuel = repository.getFuelForTrip(tripId)
            // Свод разновалютных трат в валюту рейса по курсу НБМ — как на экране рейса.
            val rates = ratesRepository.currentRates()
            _report.value = build(trip, expenses, fuel, rates)
        }
    }

    private fun build(
        trip: Trip,
        expenses: List<Expense>,
        fuel: List<FuelEntry>,
        rates: CurrencyRates
    ): TripReport {
        val currency = trip.currency
        // И расходы, и заправки сводим в валюту рейса по курсу.
        val fuelTotal = fuel.sumOf { rates.convert(it.cost, it.currency, currency) }
        val expenseTotal = expenses.sumOf { rates.convert(it.amount, it.currency, currency) }
        val total = expenseTotal + fuelTotal

        // Категории (с учётом топлива), по убыванию.
        val byCategory = HashMap<Category, Double>()
        expenses.forEach {
            val converted = rates.convert(it.amount, it.currency, currency)
            byCategory[it.category] = (byCategory[it.category] ?: 0.0) + converted
        }
        if (fuelTotal > 0) byCategory[Category.FUEL] = (byCategory[Category.FUEL] ?: 0.0) + fuelTotal
        val categoryRows = byCategory.entries
            .sortedByDescending { it.value }
            .map { (cat, value) ->
                val share = if (total > 0) (value / total * 100).roundToInt() else 0
                listOf(cat.title, Format.money(value, currency), "$share%")
            }

        val expenseRows = expenses.sortedBy { it.date }.map {
            listOf(
                Format.date(it.date),
                it.category.title,
                it.note.ifBlank { "—" },
                Format.money(it.amount, it.currency)
            )
        }

        val fuelRows = fuel.sortedBy { it.date }.map {
            listOf(Format.date(it.date), it.fuelType.title, Format.money(it.cost, it.currency))
        }

        // Длительность рейса в днях (включительно).
        val endMillis = trip.endDate ?: System.currentTimeMillis()
        val days = ((endMillis - trip.startDate) / 86_400_000L).toInt().coerceAtLeast(0) + 1
        val perDay = if (days > 0) total / days else 0.0
        val topCategory = categoryRows.firstOrNull()?.firstOrNull()

        val period = if (trip.endDate != null)
            "${Format.date(trip.startDate)} — ${Format.date(trip.endDate)} • $days дн."
        else
            "с ${Format.date(trip.startDate)} • $days дн. (рейс ещё не завершён)"

        val summaryLines = buildList {
            add("Расходов: ${expenses.size} • Заправок: ${fuel.size}")
            add("Расходы (без топлива): ${Format.money(expenseTotal, currency)}")
            add("Топливо: ${Format.money(fuelTotal, currency)}")
            add("В среднем за день: ${Format.money(perDay, currency)}")
            if (topCategory != null) add("Крупнейшая статья: $topCategory")
            add("Валюта рейса: ${currency.code} (${currency.symbol})")
        }

        val tables = buildList {
            add(
                ReportTable(
                    "Расходы по категориям",
                    listOf("Категория", "Сумма", "Доля"),
                    listOf(3f, 2f, 1f),
                    categoryRows
                )
            )
            add(
                ReportTable(
                    "Расходы (${expenses.size})",
                    listOf("Дата", "Категория", "Заметка", "Сумма"),
                    listOf(2f, 2.4f, 3f, 2f),
                    expenseRows
                )
            )
            add(
                ReportTable(
                    "Заправки (${fuel.size})",
                    listOf("Дата", "Топливо", "Цена"),
                    listOf(2f, 3f, 2f),
                    fuelRows
                )
            )
        }

        return TripReport(
            title = trip.destination,
            period = period,
            totalText = Format.money(total, currency),
            summaryLines = summaryLines,
            tables = tables,
            generatedAt = "Сформировано в MyVoyage • ${Format.date(System.currentTimeMillis())}"
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ExportViewModel(voyageRepository(), ratesRepository(), createSavedStateHandle())
            }
        }
    }
}
