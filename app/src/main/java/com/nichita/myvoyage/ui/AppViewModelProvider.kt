package com.nichita.myvoyage.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.nichita.myvoyage.MyVoyageApp
import com.nichita.myvoyage.data.repository.OfficeRepository
import com.nichita.myvoyage.data.repository.RatesRepository
import com.nichita.myvoyage.data.repository.VehicleRepository
import com.nichita.myvoyage.data.repository.VoyageRepository

/**
 * Достаёт репозиторий из Application внутри фабрик ViewModel.
 * Используется в `viewModelFactory { initializer { ... } }` на экранах.
 */
fun CreationExtras.voyageRepository(): VoyageRepository {
    val app = this[APPLICATION_KEY] as MyVoyageApp
    return app.repository
}

/** Репозиторий курсов валют для фабрик ViewModel. */
fun CreationExtras.ratesRepository(): RatesRepository {
    val app = this[APPLICATION_KEY] as MyVoyageApp
    return app.ratesRepository
}

/** Репозиторий офисов для фабрик ViewModel. */
fun CreationExtras.officeRepository(): OfficeRepository {
    val app = this[APPLICATION_KEY] as MyVoyageApp
    return app.officeRepository
}

/** Репозиторий автомобилей для фабрик ViewModel. */
fun CreationExtras.vehicleRepository(): VehicleRepository {
    val app = this[APPLICATION_KEY] as MyVoyageApp
    return app.vehicleRepository
}
