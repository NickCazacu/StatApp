package com.nichita.myvoyage.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nichita.myvoyage.ui.components.frostedGlass
import com.nichita.myvoyage.ui.offices.OfficesScreen
import com.nichita.myvoyage.ui.stats.OverallStatsScreen
import com.nichita.myvoyage.ui.tips.OverallTipsScreen
import com.nichita.myvoyage.ui.trips.TripsScreen
import com.nichita.myvoyage.ui.vehicles.VehiclesScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * Корневой экран с нижней навигацией (5 вкладок). Нижняя панель — «жидкое
 * стекло»: размывает прокручиваемый под ней контент (Haze). Поэтому контент
 * рисуется на всю высоту (под панелью), а его нижний отступ передаётся экранам.
 */
@Composable
fun HomeScaffold(
    onAddTrip: () -> Unit,
    onOpenTrip: (Long) -> Unit,
    onQuickExpense: (Long) -> Unit,
    onQuickFuel: (Long) -> Unit,
    onOpenRates: () -> Unit,
    onAddOffice: () -> Unit,
    onOpenOffice: (Long) -> Unit,
    onAddVehicle: () -> Unit,
    onOpenVehicle: (Long) -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val hazeState = remember { HazeState() }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.frostedGlass(hazeState),
                containerColor = Color.Transparent
            ) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Outlined.Map, contentDescription = "Рейсы") },
                    label = { Text("Рейсы") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Outlined.Business, contentDescription = "Офисы") },
                    label = { Text("Офисы") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Outlined.DirectionsCar, contentDescription = "Авто") },
                    label = { Text("Авто") }
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Outlined.BarChart, contentDescription = "Статистика") },
                    label = { Text("Статистика") }
                )
                NavigationBarItem(
                    selected = tab == 4,
                    onClick = { tab = 4 },
                    icon = { Icon(Icons.Outlined.Lightbulb, contentDescription = "Советы") },
                    label = { Text("Советы") }
                )
            }
        }
    ) { padding ->
        // Контент рисуется на всю высоту (под нижней панелью) — это источник
        // размытия. Высоту панели отдаём экранам как нижний отступ списков.
        val bottomInset = padding.calculateBottomPadding()
        Box(
            Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
        ) {
            when (tab) {
                0 -> TripsScreen(
                    onAddTrip = onAddTrip,
                    onOpenTrip = onOpenTrip,
                    onQuickExpense = onQuickExpense,
                    onQuickFuel = onQuickFuel,
                    onOpenRates = onOpenRates,
                    bottomInset = bottomInset
                )
                1 -> OfficesScreen(
                    onAddOffice = onAddOffice,
                    onOpenOffice = onOpenOffice,
                    bottomInset = bottomInset
                )
                2 -> VehiclesScreen(
                    onAddVehicle = onAddVehicle,
                    onOpenVehicle = onOpenVehicle,
                    bottomInset = bottomInset
                )
                3 -> OverallStatsScreen(onBack = {}, showBack = false, bottomInset = bottomInset)
                else -> OverallTipsScreen(onBack = {}, showBack = false, bottomInset = bottomInset)
            }
        }
    }
}
