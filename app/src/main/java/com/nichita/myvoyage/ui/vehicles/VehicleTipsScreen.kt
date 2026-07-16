package com.nichita.myvoyage.ui.vehicles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nichita.myvoyage.domain.Tip
import com.nichita.myvoyage.domain.TipSeverity

/** Экран «Советы» по автомобилю — rule-based рекомендации (офлайн, без AI). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleTipsScreen(
    onBack: () -> Unit,
    viewModel: VehicleTipsViewModel = viewModel(factory = VehicleTipsViewModel.Factory)
) {
    val tips by viewModel.tips.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Советы по автомобилю") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
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
            items(tips) { tip -> TipCard(tip) }
        }
    }
}

@Composable
private fun TipCard(tip: Tip) {
    // Цвет фона зависит от важности совета (как на экранах советов рейса/офиса).
    val container = when (tip.severity) {
        TipSeverity.ALERT -> MaterialTheme.colorScheme.errorContainer
        TipSeverity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        TipSeverity.POSITIVE -> MaterialTheme.colorScheme.primaryContainer
        TipSeverity.INFO -> MaterialTheme.colorScheme.secondaryContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(tip.title, style = MaterialTheme.typography.titleMedium)
            Text(tip.message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
