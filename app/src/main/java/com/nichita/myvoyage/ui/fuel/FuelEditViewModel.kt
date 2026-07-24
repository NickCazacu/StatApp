package com.nichita.myvoyage.ui.fuel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.model.FuelEntry
import com.nichita.myvoyage.data.model.FuelType
import com.nichita.myvoyage.data.repository.RatesRepository
import com.nichita.myvoyage.data.repository.VoyageRepository
import com.nichita.myvoyage.domain.CurrencyRates
import com.nichita.myvoyage.ui.nav.NavArgs
import com.nichita.myvoyage.ui.ratesRepository
import com.nichita.myvoyage.ui.voyageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Состояние формы заправки. */
data class FuelFormState(
    val date: Long = System.currentTimeMillis(),
    val cost: String = "",
    val currency: Currency = Currency.DEFAULT,
    val fuelType: FuelType = FuelType.DEFAULT,
    val isEditing: Boolean = false,
    /** Валюта рейса — для подсказки «≈ в валюте рейса» при иной валюте заправки. */
    val tripCurrency: Currency = Currency.DEFAULT,
    val rates: CurrencyRates = CurrencyRates.FALLBACK
) {
    val isValid: Boolean
        get() = (cost.toDoubleOrNull() ?: 0.0) > 0.0

    /** Пересчёт в валюту рейса (null — валюта совпадает или сумма не введена). */
    val convertedToTrip: Double?
        get() {
            if (currency == tripCurrency) return null
            val value = cost.toDoubleOrNull() ?: return null
            if (value <= 0.0) return null
            return rates.convert(value, currency, tripCurrency)
        }
}

/**
 * ViewModel добавления/редактирования заправки.
 * Аргументы: tripId (обязателен) и fuelId (0 — новая).
 */
class FuelEditViewModel(
    private val repository: VoyageRepository,
    private val ratesRepository: RatesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: Long = savedStateHandle.get<Long>(NavArgs.TRIP_ID) ?: 0L
    private val fuelId: Long = savedStateHandle.get<Long>(NavArgs.ITEM_ID) ?: 0L

    private val _form = MutableStateFlow(FuelFormState())
    val form: StateFlow<FuelFormState> = _form.asStateFlow()

    init {
        viewModelScope.launch {
            val tripCurrency = repository.getTrip(tripId)?.currency ?: Currency.DEFAULT
            val rates = ratesRepository.currentRates()
            if (fuelId != 0L) {
                repository.getFuelEntry(fuelId)?.let { e ->
                    _form.value = FuelFormState(
                        date = e.date,
                        cost = e.cost.toString(),
                        currency = e.currency,
                        fuelType = e.fuelType,
                        isEditing = true,
                        tripCurrency = tripCurrency,
                        rates = rates
                    )
                }
            } else {
                // Для новой заправки подставляем валюту рейса.
                _form.update { it.copy(currency = tripCurrency, tripCurrency = tripCurrency, rates = rates) }
            }
        }
    }

    fun onDateChange(v: Long) = _form.update { it.copy(date = v) }
    fun onCostChange(v: String) = _form.update { it.copy(cost = numeric(v)) }
    fun onCurrencyChange(v: Currency) = _form.update { it.copy(currency = v) }
    fun onFuelTypeChange(v: FuelType) = _form.update { it.copy(fuelType = v) }

    private fun numeric(v: String) = v.filter { it.isDigit() || it == '.' }

    fun save(onDone: () -> Unit) {
        val s = _form.value
        if (!s.isValid) return
        viewModelScope.launch {
            repository.upsertFuel(
                FuelEntry(
                    id = fuelId,
                    tripId = tripId,
                    date = s.date,
                    cost = s.cost.toDoubleOrNull() ?: 0.0,
                    currency = s.currency,
                    fuelType = s.fuelType
                )
            )
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                FuelEditViewModel(voyageRepository(), ratesRepository(), createSavedStateHandle())
            }
        }
    }
}
