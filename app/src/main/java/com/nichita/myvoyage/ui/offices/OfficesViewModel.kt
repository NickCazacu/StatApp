package com.nichita.myvoyage.ui.offices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.model.Office
import com.nichita.myvoyage.data.repository.OfficeRepository
import com.nichita.myvoyage.data.repository.RatesRepository
import com.nichita.myvoyage.ui.officeRepository
import com.nichita.myvoyage.ui.ratesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Офис + сумма его расходов за всё время в валюте офиса (по курсу НБМ). */
data class OfficeListItem(
    val office: Office,
    val total: Double
)

/** ViewModel вкладки «Офисы»: список офисов с итогами в валюте офиса. */
class OfficesViewModel(
    repository: OfficeRepository,
    ratesRepository: RatesRepository
) : ViewModel() {

    val offices: StateFlow<List<OfficeListItem>> =
        combine(
            repository.observeOffices(),
            repository.observeCurrencyTotalsPerOffice(),
            ratesRepository.observeRates()
        ) { offices, totals, rates ->
            val byOffice = totals.groupBy { it.officeId }
            offices.map { office ->
                val total = byOffice[office.id].orEmpty()
                    .sumOf { rates.convert(it.total, it.currency, office.currency) }
                OfficeListItem(office, total)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer { OfficesViewModel(officeRepository(), ratesRepository()) }
        }
    }
}
