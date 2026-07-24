package com.nichita.myvoyage.ui.vehicles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.db.MonthTotal
import com.nichita.myvoyage.data.db.VehicleCategorySum
import com.nichita.myvoyage.data.model.Vehicle
import com.nichita.myvoyage.data.model.VehicleExpense
import com.nichita.myvoyage.data.repository.RatesRepository
import com.nichita.myvoyage.data.repository.VehicleRepository
import com.nichita.myvoyage.domain.VehicleExpenseMath
import com.nichita.myvoyage.export.ReportTable
import com.nichita.myvoyage.export.TripReport
import com.nichita.myvoyage.ui.nav.NavArgs
import com.nichita.myvoyage.ui.ratesRepository
import com.nichita.myvoyage.ui.vehicleRepository
import com.nichita.myvoyage.util.Format
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Готовит отчёт по автомобилю (статистика расходов по месяцам) для экспорта. */
class VehicleExportViewModel(
    private val repository: VehicleRepository,
    private val ratesRepository: RatesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vehicleId: Long = savedStateHandle.get<Long>(NavArgs.VEHICLE_ID) ?: 0L

    private val _report = MutableStateFlow<TripReport?>(null)
    val report: StateFlow<TripReport?> = _report.asStateFlow()

    init {
        viewModelScope.launch {
            val vehicle = repository.getVehicle(vehicleId) ?: return@launch
            val expenses = repository.getExpenses(vehicleId)
            // Свод разновалютных трат в валюту автомобиля по курсу НБМ.
            val rates = ratesRepository.currentRates()
            val months = VehicleExpenseMath.monthTotals(expenses, rates, vehicle.currency)
            val categories = VehicleExpenseMath.categorySums(expenses, rates, vehicle.currency)
            val total = VehicleExpenseMath.total(expenses, rates, vehicle.currency)
            _report.value = build(vehicle, expenses, months, categories, total)
        }
    }

    private fun build(
        vehicle: Vehicle,
        expenses: List<VehicleExpense>,
        months: List<MonthTotal>,
        categories: List<VehicleCategorySum>,
        total: Double
    ): TripReport {
        val currency = vehicle.currency

        val categoryRows = categories.map { cs ->
            val share = if (total > 0) (cs.total / total * 100).roundToInt() else 0
            listOf(cs.category.title, Format.money(cs.total, currency), "$share%")
        }

        val monthRows = months.map {
            listOf(Format.monthYear(it.year, it.month), Format.money(it.total, currency))
        }

        // Все траты: свежие месяцы сверху, суммы — в валюте самой траты.
        val expenseRows = expenses.map {
            listOf(
                Format.monthYear(it.year, it.month),
                it.category.title,
                it.note.ifBlank { "—" },
                Format.money(it.amount, it.currency)
            )
        }

        val period = if (months.isNotEmpty()) {
            val first = months.first()
            val last = months.last()
            "${Format.monthYear(first.year, first.month)} — " +
                "${Format.monthYear(last.year, last.month)} • ${months.size} мес."
        } else {
            "Нет данных по месяцам"
        }

        val avg = months.filter { it.total > 0.0 }
            .takeIf { it.isNotEmpty() }?.map { it.total }?.average()
        val maxMonth = months.maxByOrNull { it.total }
        val topCategory = categories.firstOrNull()

        val summaryLines = buildList {
            if (vehicle.plate.isNotBlank()) add("Гос. номер: ${vehicle.plate}")
            add("Трат: ${expenses.size} • Месяцев с данными: ${months.size}")
            if (avg != null) add("В среднем за месяц: ${Format.money(avg, currency)}")
            if (maxMonth != null) {
                add(
                    "Самый дорогой месяц: ${Format.monthYear(maxMonth.year, maxMonth.month)} " +
                        "(${Format.money(maxMonth.total, currency)})"
                )
            }
            if (topCategory != null) add("Крупнейшая статья: ${topCategory.category.title}")
            add("Валюта учёта: ${currency.code} (${currency.symbol})")
            if (expenses.any { it.currency != currency }) {
                add("Разновалютные траты сведены в ${currency.code} по курсу НБМ")
            }
        }

        val tables = listOf(
            ReportTable(
                "Расходы по категориям",
                listOf("Категория", "Сумма", "Доля"),
                listOf(3f, 2f, 1f),
                categoryRows
            ),
            ReportTable(
                "Итоги по месяцам",
                listOf("Месяц", "Сумма"),
                listOf(3f, 2f),
                monthRows
            ),
            ReportTable(
                "Все траты (${expenses.size})",
                listOf("Месяц", "Категория", "Заметка", "Сумма"),
                listOf(2.2f, 2.4f, 3f, 2f),
                expenseRows
            )
        )

        return TripReport(
            title = vehicle.name,
            period = period,
            totalText = Format.money(total, currency),
            summaryLines = summaryLines,
            tables = tables,
            generatedAt = "Сформировано в MyVoyage • ${Format.date(System.currentTimeMillis())}",
            kicker = "Отчёт по автомобилю"
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                VehicleExportViewModel(
                    vehicleRepository(),
                    ratesRepository(),
                    createSavedStateHandle()
                )
            }
        }
    }
}
