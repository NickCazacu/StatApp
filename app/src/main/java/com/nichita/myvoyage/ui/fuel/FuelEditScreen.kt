package com.nichita.myvoyage.ui.fuel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.model.FuelType
import com.nichita.myvoyage.ui.components.DateField
import com.nichita.myvoyage.ui.components.EnumDropdown
import com.nichita.myvoyage.util.Format

/** Экран добавления/редактирования заправки. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: FuelEditViewModel = viewModel(factory = FuelEditViewModel.Factory)
) {
    val form by viewModel.form.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (form.isEditing) "Редактировать заправку" else "Новая заправка") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } },
                actions = {
                    TextButton(onClick = { viewModel.save(onSaved) }, enabled = form.isValid) {
                        Text("Сохранить")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DateField(
                label = "Дата",
                millis = form.date,
                onPick = viewModel::onDateChange
            )

            OutlinedTextField(
                value = form.cost,
                onValueChange = viewModel::onCostChange,
                label = { Text("Цена заправки") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            EnumDropdown(
                label = "Валюта",
                options = Currency.entries,
                selected = form.currency,
                optionLabel = { "${it.code} (${it.symbol})" },
                onSelected = viewModel::onCurrencyChange
            )

            // Если валюта заправки отличается от валюты рейса — показываем пересчёт.
            form.convertedToTrip?.let { converted ->
                Text("≈ ${Format.money(converted, form.tripCurrency)} по курсу НБМ")
            }

            EnumDropdown(
                label = "Тип топлива",
                options = FuelType.entries,
                selected = form.fuelType,
                optionLabel = { it.title },
                onSelected = viewModel::onFuelTypeChange
            )
        }
    }
}
