package com.nichita.myvoyage.ui.offices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nichita.myvoyage.ui.components.frostedGlass
import com.nichita.myvoyage.util.Format
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/** Вкладка «Офисы»: список офисов с итоговыми расходами и кнопкой добавления. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficesScreen(
    onAddOffice: () -> Unit,
    onOpenOffice: (Long) -> Unit,
    bottomInset: Dp = 0.dp,
    viewModel: OfficesViewModel = viewModel(factory = OfficesViewModel.Factory)
) {
    val offices by viewModel.offices.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val topHaze = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            LargeTopAppBar(
                title = { Text("Мои офисы") },
                modifier = Modifier.frostedGlass(topHaze),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            // Контент рисуется под нижней панелью навигации (Haze), поэтому FAB
            // поднимаем на её высоту — иначе он окажется под вкладками.
            FloatingActionButton(
                onClick = onAddOffice,
                modifier = Modifier.padding(bottom = bottomInset)
            ) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(topHaze),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp + bottomInset
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (offices.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Офисов пока нет", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Добавьте офис кнопкой «+», затем вносите его расходы " +
                                    "по месяцам — приложение покажет статистику и советы " +
                                    "по оптимизации.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            items(offices, key = { it.office.id }) { item ->
                OfficeCard(item, onClick = { onOpenOffice(item.office.id) })
            }
        }
    }
}

@Composable
private fun OfficeCard(item: OfficeListItem, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Business, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.office.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.office.address.isNotBlank()) {
                    Text(
                        item.office.address,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                Format.money(item.total, item.office.currency),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
