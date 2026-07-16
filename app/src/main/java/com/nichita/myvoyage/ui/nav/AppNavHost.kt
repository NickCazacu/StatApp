package com.nichita.myvoyage.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nichita.myvoyage.ui.detail.TripDetailScreen
import com.nichita.myvoyage.ui.expense.ExpenseEditScreen
import com.nichita.myvoyage.ui.export.ExportScreen
import com.nichita.myvoyage.ui.fuel.FuelEditScreen
import com.nichita.myvoyage.ui.home.HomeScaffold
import com.nichita.myvoyage.ui.offices.OfficeDetailScreen
import com.nichita.myvoyage.ui.offices.OfficeEditScreen
import com.nichita.myvoyage.ui.offices.OfficeExpenseEditScreen
import com.nichita.myvoyage.ui.offices.OfficeStatsScreen
import com.nichita.myvoyage.ui.offices.OfficeTipsScreen
import com.nichita.myvoyage.ui.rates.CurrencyRatesScreen
import com.nichita.myvoyage.ui.stats.StatsScreen
import com.nichita.myvoyage.ui.tips.TipsScreen
import com.nichita.myvoyage.ui.trips.TripEditScreen

/**
 * Граф навигации приложения.
 *
 * Аргументы рейса/элемента передаются через маршрут и попадают в
 * SavedStateHandle ViewModel'ей (см. NavArgs).
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.TRIPS) {

        // Главный экран: нижняя навигация (рейсы + сводная статистика)
        composable(Routes.TRIPS) {
            HomeScaffold(
                onAddTrip = { navController.navigate(Routes.tripEdit()) },
                onOpenTrip = { id -> navController.navigate(Routes.tripDetail(id)) },
                onQuickExpense = { id -> navController.navigate(Routes.expenseEdit(id)) },
                onQuickFuel = { id -> navController.navigate(Routes.fuelEdit(id)) },
                onOpenRates = { navController.navigate(Routes.currencyRates()) },
                onAddOffice = { navController.navigate(Routes.officeEdit()) },
                onOpenOffice = { id -> navController.navigate(Routes.officeDetail(id)) }
            )
        }

        // Курс валют (НБМ)
        composable(Routes.CURRENCY_RATES) {
            CurrencyRatesScreen(onBack = { navController.popBackStack() })
        }

        // Создание/редактирование рейса
        composable(
            route = Routes.TRIP_EDIT,
            arguments = listOf(
                navArgument(NavArgs.TRIP_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            TripEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // Детали рейса
        composable(
            route = Routes.TRIP_DETAIL,
            arguments = listOf(navArgument(NavArgs.TRIP_ID) { type = NavType.LongType })
        ) { entry ->
            val tripId = entry.arguments?.getLong(NavArgs.TRIP_ID) ?: 0L
            TripDetailScreen(
                onBack = { navController.popBackStack() },
                onEditTrip = { navController.navigate(Routes.tripEdit(tripId)) },
                onAddExpense = { navController.navigate(Routes.expenseEdit(tripId)) },
                onEditExpense = { id -> navController.navigate(Routes.expenseEdit(tripId, id)) },
                onAddFuel = { navController.navigate(Routes.fuelEdit(tripId)) },
                onEditFuel = { id -> navController.navigate(Routes.fuelEdit(tripId, id)) },
                onOpenStats = { navController.navigate(Routes.stats(tripId)) },
                onOpenTips = { navController.navigate(Routes.tips(tripId)) },
                onOpenExport = { navController.navigate(Routes.export(tripId)) },
                onDeleted = { navController.popBackStack() }
            )
        }

        // Статистика рейса
        composable(
            route = Routes.STATS,
            arguments = listOf(navArgument(NavArgs.TRIP_ID) { type = NavType.LongType })
        ) {
            StatsScreen(onBack = { navController.popBackStack() })
        }

        // Советы по рейсу
        composable(
            route = Routes.TIPS,
            arguments = listOf(navArgument(NavArgs.TRIP_ID) { type = NavType.LongType })
        ) {
            TipsScreen(onBack = { navController.popBackStack() })
        }

        // Экспорт отчёта по рейсу (PDF/Word/Excel)
        composable(
            route = Routes.EXPORT,
            arguments = listOf(navArgument(NavArgs.TRIP_ID) { type = NavType.LongType })
        ) {
            ExportScreen(onBack = { navController.popBackStack() })
        }

        // Создание/редактирование расхода
        composable(
            route = Routes.EXPENSE_EDIT,
            arguments = listOf(
                navArgument(NavArgs.TRIP_ID) { type = NavType.LongType },
                navArgument(NavArgs.ITEM_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            ExpenseEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // Создание/редактирование заправки
        composable(
            route = Routes.FUEL_EDIT,
            arguments = listOf(
                navArgument(NavArgs.TRIP_ID) { type = NavType.LongType },
                navArgument(NavArgs.ITEM_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            FuelEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // Создание/редактирование офиса
        composable(
            route = Routes.OFFICE_EDIT,
            arguments = listOf(
                navArgument(NavArgs.OFFICE_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            OfficeEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // Детали офиса: помесячные расходы, статистика, советы
        composable(
            route = Routes.OFFICE_DETAIL,
            arguments = listOf(navArgument(NavArgs.OFFICE_ID) { type = NavType.LongType })
        ) { entry ->
            val officeId = entry.arguments?.getLong(NavArgs.OFFICE_ID) ?: 0L
            OfficeDetailScreen(
                onBack = { navController.popBackStack() },
                onEditOffice = { navController.navigate(Routes.officeEdit(officeId)) },
                onAddExpense = { navController.navigate(Routes.officeExpenseEdit(officeId)) },
                onEditExpense = { id ->
                    navController.navigate(Routes.officeExpenseEdit(officeId, id))
                },
                onOpenStats = { navController.navigate(Routes.officeStats(officeId)) },
                onOpenTips = { navController.navigate(Routes.officeTips(officeId)) },
                onDeleted = { navController.popBackStack() }
            )
        }

        // Статистика офиса
        composable(
            route = Routes.OFFICE_STATS,
            arguments = listOf(navArgument(NavArgs.OFFICE_ID) { type = NavType.LongType })
        ) {
            OfficeStatsScreen(onBack = { navController.popBackStack() })
        }

        // Советы по офису
        composable(
            route = Routes.OFFICE_TIPS,
            arguments = listOf(navArgument(NavArgs.OFFICE_ID) { type = NavType.LongType })
        ) {
            OfficeTipsScreen(onBack = { navController.popBackStack() })
        }

        // Создание/редактирование расхода офиса
        composable(
            route = Routes.OFFICE_EXPENSE_EDIT,
            arguments = listOf(
                navArgument(NavArgs.OFFICE_ID) { type = NavType.LongType },
                navArgument(NavArgs.ITEM_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            OfficeExpenseEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
    }
}
