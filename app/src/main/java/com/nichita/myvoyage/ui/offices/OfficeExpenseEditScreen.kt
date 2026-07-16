package com.nichita.myvoyage.ui.offices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nichita.myvoyage.data.model.OfficeCategory
import com.nichita.myvoyage.ui.components.EnumDropdown
import com.nichita.myvoyage.util.Format
import java.util.Calendar

/** Экран добавления/редактирования расхода офиса (месяц + категория + сумма). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeExpenseEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: OfficeExpenseEditViewModel = viewModel(factory = OfficeExpenseEditViewModel.Factory)
) {
    val form by viewModel.form.collectAsStateWithLifecycle()

    // Годы для выбора: от 2020 до следующего (планирование вперёд).
    val years = remember {
        val current = Calendar.getInstance().get(Calendar.YEAR)
        (2020..current + 1).toList().reversed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (form.isEditing) "Редактировать трату" else "Новая трата") },
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
            OutlinedTextField(
                value = form.amount,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Сумма") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            EnumDropdown(
                label = "Категория",
                options = OfficeCategory.entries,
                selected = form.category,
                optionLabel = { it.title },
                onSelected = viewModel::onCategoryChange
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EnumDropdown(
                    label = "Месяц",
                    options = (1..12).toList(),
                    selected = form.month,
                    optionLabel = { Format.MONTHS[it - 1] },
                    onSelected = viewModel::onMonthChange,
                    modifier = Modifier.weight(1f)
                )
                EnumDropdown(
                    label = "Год",
                    options = years,
                    selected = form.year,
                    optionLabel = { it.toString() },
                    onSelected = viewModel::onYearChange,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = form.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text("Заметка") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
