package com.nichita.myvoyage.ui.offices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.repository.OfficeRepository
import com.nichita.myvoyage.domain.OfficeTipsAnalyzer
import com.nichita.myvoyage.domain.OfficeTipsInput
import com.nichita.myvoyage.domain.Tip
import com.nichita.myvoyage.domain.TipSeverity
import com.nichita.myvoyage.ui.nav.NavArgs
import com.nichita.myvoyage.ui.officeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** ViewModel советов по офису: собирает данные и запускает rule-based анализ. */
class OfficeTipsViewModel(
    private val repository: OfficeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val officeId: Long = savedStateHandle.get<Long>(NavArgs.OFFICE_ID) ?: 0L

    private val _tips = MutableStateFlow<List<Tip>>(emptyList())
    val tips: StateFlow<List<Tip>> = _tips.asStateFlow()

    init {
        viewModelScope.launch {
            val office = repository.getOffice(officeId)
            if (office == null) {
                _tips.value = listOf(
                    Tip(TipSeverity.INFO, "Офис не найден", "Попробуйте открыть экран заново.")
                )
                return@launch
            }
            val months = repository.getMonthTotals(officeId)
            if (months.isEmpty()) {
                _tips.value = listOf(
                    Tip(
                        TipSeverity.INFO,
                        "Пока нет данных",
                        "Добавьте расходы офиса хотя бы за один месяц — " +
                            "и здесь появятся советы по оптимизации."
                    )
                )
                return@launch
            }
            _tips.value = OfficeTipsAnalyzer.analyze(
                OfficeTipsInput(
                    office = office,
                    monthTotals = months,
                    categorySums = repository.getCategorySums(officeId)
                )
            )
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { OfficeTipsViewModel(officeRepository(), createSavedStateHandle()) }
        }
    }
}
