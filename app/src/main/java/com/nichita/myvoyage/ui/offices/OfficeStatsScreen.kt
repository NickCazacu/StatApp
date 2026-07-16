package com.nichita.myvoyage.ui.offices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nichita.myvoyage.data.db.MonthTotal
import com.nichita.myvoyage.data.db.OfficeCategorySum
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.util.Format
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import kotlin.math.roundToInt

/** Экран статистики офиса: итог, средний месяц, динамика и категории. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeStatsScreen(
    onBack: () -> Unit,
    viewModel: OfficeStatsViewModel = viewModel(factory = OfficeStatsViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = state.office?.currency ?: Currency.MDL

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика офиса") },
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
            item { TotalCard(state, currency) }
            item { Text("Динамика по месяцам", style = MaterialTheme.typography.titleMedium) }
            item { MonthChartCard(state.monthTotals, currency) }
            item { Text("Расходы по категориям", style = MaterialTheme.typography.titleMedium) }
            item { CategoryChartCard(state.categorySums, state.total, currency) }
        }
    }
}

@Composable
private fun TotalCard(state: OfficeStatsState, currency: Currency) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Всего расходов", style = MaterialTheme.typography.labelMedium)
            Text(Format.money(state.total, currency), style = MaterialTheme.typography.headlineSmall)

            val avg = state.avgPerMonth
            if (avg != null) {
                Text(
                    "В среднем за месяц: ${Format.money(avg, currency)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            val diff = state.lastVsAvg
            if (diff != null) {
                val pct = (diff * 100).roundToInt()
                val text = when {
                    pct > 0 -> "Последний месяц на $pct% дороже среднего"
                    pct < 0 -> "Последний месяц на ${-pct}% дешевле среднего"
                    else -> "Последний месяц на уровне среднего"
                }
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (pct > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** График помесячных итогов (Vico) с легендой «месяц → сумма». */
@Composable
private fun MonthChartCard(months: List<MonthTotal>, currency: Currency) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (months.isEmpty()) {
                Text("Нет данных для графика", style = MaterialTheme.typography.bodySmall)
                return@Column
            }

            val modelProducer = remember { CartesianChartModelProducer() }
            LaunchedEffect(months) {
                modelProducer.runTransaction {
                    columnSeries { series(months.map { it.total }) }
                }
            }

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom()
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            // Легенда: номер столбца → месяц и сумма.
            months.forEachIndexed { index, m ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${index + 1}. ${Format.monthYear(m.year, m.month)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        Format.money(m.total, currency),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/** Разбивка по категориям с долями (без графика — суммы и проценты). */
@Composable
private fun CategoryChartCard(sums: List<OfficeCategorySum>, total: Double, currency: Currency) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (sums.isEmpty()) {
                Text("Нет данных", style = MaterialTheme.typography.bodySmall)
                return@Column
            }
            sums.forEach { cs ->
                val share = if (total > 0) (cs.total / total * 100).roundToInt() else 0
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(cs.category.title, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${Format.money(cs.total, currency)} ($share%)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
