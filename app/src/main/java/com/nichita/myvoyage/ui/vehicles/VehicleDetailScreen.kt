package com.nichita.myvoyage.ui.vehicles

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.model.VehicleExpense
import com.nichita.myvoyage.domain.CurrencyRates
import com.nichita.myvoyage.util.Format

/** Экран деталей автомобиля: итог, действия и расходы по месяцам. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    onBack: () -> Unit,
    onEditVehicle: () -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onOpenStats: () -> Unit,
    onOpenTips: () -> Unit,
    onOpenExport: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: VehicleDetailViewModel = viewModel(factory = VehicleDetailViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = state.vehicle?.currency ?: Currency.MDL
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить автомобиль?") },
            text = {
                Text("Автомобиль и все его расходы будут удалены безвозвратно.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteVehicle(onDeleted)
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
                        state.vehicle?.name ?: "Автомобиль",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } },
                actions = { TextButton(onClick = onEditVehicle) { Text("Изменить") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- Шапка: номер и итог за всё время ---
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        state.vehicle?.plate?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("Всего расходов", style = MaterialTheme.typography.labelMedium)
                        Text(
                            Format.money(state.total, currency),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }

            // --- Действия ---
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenStats, modifier = Modifier.weight(1f)) {
                        Text("Статистика")
                    }
                    OutlinedButton(onClick = onOpenTips, modifier = Modifier.weight(1f)) {
                        Text("Советы")
                    }
                    OutlinedButton(onClick = onOpenExport, modifier = Modifier.weight(1f)) {
                        Text("Экспорт")
                    }
                }
            }

            // --- Расходы по месяцам ---
            if (state.months.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Расходов пока нет", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Добавьте первую трату кнопкой «+»: выберите месяц, " +
                                    "категорию и сумму.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            state.months.forEach { group ->
                item(key = "month-${group.year}-${group.month}") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            Format.monthYear(group.year, group.month),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            Format.money(group.total, currency),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                items(group.items, key = { it.id }) { expense ->
                    ExpenseRow(
                        expense = expense,
                        baseCurrency = currency,
                        rates = state.rates,
                        onClick = { onEditExpense(expense.id) },
                        onDelete = { viewModel.deleteExpense(expense) }
                    )
                }
            }

            // --- Удаление автомобиля ---
            item {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить автомобиль")
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: VehicleExpense,
    baseCurrency: Currency,
    rates: CurrencyRates,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(expense.category.title, style = MaterialTheme.typography.bodyLarge)
                if (expense.note.isNotBlank()) {
                    Text(
                        expense.note,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Сумма — в валюте самой траты; итоги месяца сведены в валюту авто.
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Format.money(expense.amount, expense.currency),
                    style = MaterialTheme.typography.bodyLarge
                )
                // Если валюта траты отличается от валюты авто — показываем пересчёт.
                if (expense.currency != baseCurrency) {
                    val converted =
                        rates.convert(expense.amount, expense.currency, baseCurrency)
                    Text(
                        "≈ ${Format.money(converted, baseCurrency)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            TextButton(onClick = onDelete) { Text("Удал.") }
        }
    }
}
