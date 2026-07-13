package com.nichita.myvoyage.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.model.Expense
import com.nichita.myvoyage.data.model.FuelEntry
import com.nichita.myvoyage.util.Format

/** Экран одного рейса: сводка, переходы к статистике/советам, списки расходов и заправок. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    onBack: () -> Unit,
    onEditTrip: () -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onAddFuel: () -> Unit,
    onEditFuel: (Long) -> Unit,
    onOpenStats: () -> Unit,
    onOpenTips: () -> Unit,
    onOpenExport: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: TripDetailViewModel = viewModel(factory = TripDetailViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val trip = state.trip
    val currency = trip?.currency ?: Currency.DEFAULT
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить рейс?") },
            text = {
                Text(
                    "Рейс «${trip?.destination.orEmpty()}» и все его расходы и заправки " +
                        "будут удалены безвозвратно."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteTrip(onDeleted)
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        trip?.destination ?: "Рейс",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } },
                actions = { TextButton(onClick = onEditTrip) { Text("Изм.") } }
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
            // --- Сводка ---
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Итого потрачено", style = MaterialTheme.typography.labelMedium)
                        Text(
                            Format.money(state.total, currency),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }

            // --- Навигация к статистике/советам ---
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onOpenStats, modifier = Modifier.weight(1f)) {
                        Text("Статистика")
                    }
                    OutlinedButton(onClick = onOpenTips, modifier = Modifier.weight(1f)) {
                        Text("Советы")
                    }
                }
            }

            // --- Экспорт отчёта ---
            item {
                Button(onClick = onOpenExport, modifier = Modifier.fillMaxWidth()) {
                    Text("Экспорт отчёта · PDF · Word · Excel")
                }
            }

            // --- Кнопки добавления ---
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onAddExpense, modifier = Modifier.weight(1f)) {
                        Text("+ Расход")
                    }
                    Button(onClick = onAddFuel, modifier = Modifier.weight(1f)) {
                        Text("+ Заправка")
                    }
                }
            }

            // --- Расходы ---
            item {
                Text("Расходы", style = MaterialTheme.typography.titleMedium)
            }
            if (state.expenses.isEmpty()) {
                item { Text("Расходов пока нет", style = MaterialTheme.typography.bodySmall) }
            } else {
                items(state.expenses, key = { "e${it.id}" }) { expense ->
                    ExpenseRow(
                        expense = expense,
                        tripCurrency = currency,
                        rates = state.rates,
                        onClick = { onEditExpense(expense.id) },
                        onDelete = { viewModel.deleteExpense(expense) }
                    )
                    HorizontalDivider()
                }
            }

            // --- Заправки ---
            item {
                Text("Заправки", style = MaterialTheme.typography.titleMedium)
            }
            if (state.fuel.isEmpty()) {
                item { Text("Заправок пока нет", style = MaterialTheme.typography.bodySmall) }
            } else {
                items(state.fuel, key = { "f${it.id}" }) { entry ->
                    FuelRow(
                        entry = entry,
                        currency = currency,
                        onClick = { onEditFuel(entry.id) },
                        onDelete = { viewModel.deleteFuel(entry) }
                    )
                    HorizontalDivider()
                }
            }

            // --- Удаление рейса ---
            item {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить рейс")
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    tripCurrency: Currency,
    rates: com.nichita.myvoyage.domain.CurrencyRates,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            Text(expense.category.title, style = MaterialTheme.typography.bodyLarge)
            val sub = buildString {
                append(Format.date(expense.date))
                if (expense.note.isNotBlank()) append(" • ${expense.note}")
            }
            Text(sub, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            Text(
                Format.money(expense.amount, expense.currency),
                style = MaterialTheme.typography.bodyMedium
            )
            // Если валюта расхода отличается от валюты рейса — показываем пересчёт.
            if (expense.currency != tripCurrency) {
                val converted = rates.convert(expense.amount, expense.currency, tripCurrency)
                Text(
                    "≈ ${Format.money(converted, tripCurrency)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        TextButton(onClick = onClick) { Text("Изм.") }
        TextButton(onClick = onDelete) { Text("Удал.") }
    }
}

@Composable
private fun FuelRow(entry: FuelEntry, currency: Currency, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            Text(
                entry.fuelType.title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                Format.date(entry.date),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(Format.money(entry.cost, currency), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onClick) { Text("Изм.") }
        TextButton(onClick = onDelete) { Text("Удал.") }
    }
}
