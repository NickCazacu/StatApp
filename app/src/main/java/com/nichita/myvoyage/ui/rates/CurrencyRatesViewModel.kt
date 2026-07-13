package com.nichita.myvoyage.ui.rates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.repository.RatesRepository
import com.nichita.myvoyage.ui.ratesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Одна строка курса: валюта и её цена в базовой валюте (за 1 единицу). */
data class RateItem(
    val currency: Currency,
    val valuePerUnit: Double
)

/** Состояние экрана курса валют. */
data class CurrencyRatesUiState(
    val rates: List<RateItem> = emptyList(),
    /** Базовая валюта, в которой выражен курс (MDL или EUR). */
    val base: Currency = Currency.MDL,
    /** Валюты, которые можно выбрать как базовые. */
    val bases: List<Currency> = listOf(Currency.MDL, Currency.EUR),
    /** Когда курс получен из сети (epoch millis); null — ещё ни разу. */
    val updatedAt: Long? = null,
    val isRefreshing: Boolean = false,
    /** Итог последней попытки обновления: true — онлайн, false — сеть недоступна, null — ещё не пробовали. */
    val online: Boolean? = null
)

/**
 * ViewModel экрана курса валют. При открытии пытается обновить курс из НБМ;
 * список берётся из кэша (реактивно), поэтому офлайн показываются последние
 * сохранённые значения, а онлайн — свежие.
 */
class CurrencyRatesViewModel(
    private val ratesRepository: RatesRepository
) : ViewModel() {

    private data class Status(val isRefreshing: Boolean, val online: Boolean?)

    private val status = MutableStateFlow(Status(isRefreshing = false, online = null))

    /** Базовая валюта, в которой показывается курс. По умолчанию — лей. */
    private val base = MutableStateFlow(Currency.MDL)

    /** Валюты, доступные как базовые. */
    private val bases = listOf(Currency.MDL, Currency.EUR)

    val uiState: StateFlow<CurrencyRatesUiState> =
        combine(ratesRepository.observeRates(), status, base) { rates, st, baseCurrency ->
            // Базовую валюту (всегда 1) в списке не показываем.
            val items = Currency.entries
                .filter { it != baseCurrency }
                .map { RateItem(it, rates.convert(1.0, it, baseCurrency)) }
            CurrencyRatesUiState(
                rates = items,
                base = baseCurrency,
                bases = bases,
                updatedAt = rates.updatedAt,
                isRefreshing = st.isRefreshing,
                online = st.online
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CurrencyRatesUiState()
        )

    init {
        refresh()
    }

    /** Меняет базовую валюту (переключатель MDL / EUR). */
    fun setBase(currency: Currency) {
        base.value = currency
    }

    /** Повторная попытка обновить курс из сети (кнопка «Обновить»). */
    fun refresh() {
        viewModelScope.launch {
            status.update { it.copy(isRefreshing = true) }
            val ok = ratesRepository.refresh()
            status.value = Status(isRefreshing = false, online = ok)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { CurrencyRatesViewModel(ratesRepository()) }
        }
    }
}
