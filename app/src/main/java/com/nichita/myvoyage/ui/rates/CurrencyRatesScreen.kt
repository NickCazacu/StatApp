package com.nichita.myvoyage.ui.rates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.util.Format

/**
 * Экран курса валют (по данным НБМ). Онлайн показывает действующий курс и
 * обновляет кэш; офлайн — последний сохранённый курс с датой обновления.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyRatesScreen(
    onBack: () -> Unit,
    viewModel: CurrencyRatesViewModel = viewModel(factory = CurrencyRatesViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Курс валют") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } },
                actions = {
                    TextButton(
                        onClick = { viewModel.refresh() },
                        enabled = !state.isRefreshing
                    ) { Text("Обновить") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { StatusCard(state) }

            // Переключатель базовой валюты (в чём выражать курс).
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Базовая валюта",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.bases.forEach { base ->
                            FilterChip(
                                selected = state.base == base,
                                onClick = { viewModel.setBase(base) },
                                label = { Text("${base.code} · ${base.symbol}") }
                            )
                        }
                    }
                    Text(
                        "Показано, сколько ${state.base.symbol} (${currencyName(state.base)}) " +
                            "стоит 1 единица валюты.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(state.rates, key = { it.currency.code }) { item ->
                RateRow(item, state.base)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun StatusCard(state: CurrencyRatesUiState) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            Column(Modifier.weight(1f)) {
                val title = when {
                    state.isRefreshing -> "Обновление курса…"
                    state.online == true -> "Действующий курс"
                    state.online == false && state.updatedAt != null -> "Нет интернета"
                    state.online == false -> "Нет интернета"
                    else -> "Курс валют"
                }
                Text(title, style = MaterialTheme.typography.titleMedium)

                val subtitle = when {
                    state.updatedAt != null ->
                        "Обновлено: ${Format.dateTime(state.updatedAt)}"
                    else ->
                        "Показаны приблизительные курсы — обновятся при подключении к интернету"
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Источник: Национальный банк Молдовы",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RateRow(item: RateItem, base: Currency) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "${item.currency.code} · ${item.currency.symbol}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                currencyName(item.currency),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "1 ${item.currency.code} = ${Format.rate(item.valuePerUnit)} ${base.symbol}",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/** Человекочитаемое название валюты (для строки курса). */
private fun currencyName(currency: Currency): String = when (currency) {
    Currency.EUR -> "Евро"
    Currency.RON -> "Румынский лей"
    Currency.MDL -> "Молдавский лей"
    Currency.USD -> "Доллар США"
    Currency.PLN -> "Польский злотый"
    Currency.HUF -> "Венгерский форинт"
    Currency.BYN -> "Белорусский рубль"
}
