package com.nichita.myvoyage.ui.nav

/** Имена аргументов навигации (используются и в ViewModel через SavedStateHandle). */
object NavArgs {
    const val TRIP_ID = "tripId"
    const val ITEM_ID = "itemId"
}

/**
 * Маршруты навигации. Для каждого экрана есть:
 *  - константа с шаблоном (для регистрации в NavHost),
 *  - функция-билдер с подстановкой аргументов (для перехода).
 */
object Routes {
    const val TRIPS = "trips"

    const val CURRENCY_RATES = "currency_rates"
    fun currencyRates() = CURRENCY_RATES

    const val TRIP_EDIT = "trip_edit?tripId={tripId}"
    fun tripEdit(tripId: Long = 0L) = "trip_edit?tripId=$tripId"

    const val TRIP_DETAIL = "trip/{tripId}"
    fun tripDetail(tripId: Long) = "trip/$tripId"

    const val STATS = "trip/{tripId}/stats"
    fun stats(tripId: Long) = "trip/$tripId/stats"

    const val TIPS = "trip/{tripId}/tips"
    fun tips(tripId: Long) = "trip/$tripId/tips"

    const val EXPORT = "trip/{tripId}/export"
    fun export(tripId: Long) = "trip/$tripId/export"

    const val EXPENSE_EDIT = "trip/{tripId}/expense?itemId={itemId}"
    fun expenseEdit(tripId: Long, itemId: Long = 0L) = "trip/$tripId/expense?itemId=$itemId"

    const val FUEL_EDIT = "trip/{tripId}/fuel?itemId={itemId}"
    fun fuelEdit(tripId: Long, itemId: Long = 0L) = "trip/$tripId/fuel?itemId=$itemId"
}
