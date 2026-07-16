package com.nichita.myvoyage.ui.offices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.model.Office
import com.nichita.myvoyage.data.repository.OfficeRepository
import com.nichita.myvoyage.ui.officeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Офис + сумма его расходов за всё время (для карточки в списке). */
data class OfficeListItem(
    val office: Office,
    val total: Double
)

/** ViewModel вкладки «Офисы»: список офисов с итогами. */
class OfficesViewModel(repository: OfficeRepository) : ViewModel() {

    val offices: StateFlow<List<OfficeListItem>> =
        combine(
            repository.observeOffices(),
            repository.observeTotalsPerOffice()
        ) { offices, totals ->
            val byId = totals.associateBy({ it.officeId }, { it.total })
            offices.map { OfficeListItem(it, byId[it.id] ?: 0.0) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer { OfficesViewModel(officeRepository()) }
        }
    }
}
