package com.nichita.myvoyage

import android.app.Application
import com.nichita.myvoyage.data.db.AppDatabase
import com.nichita.myvoyage.data.repository.OfficeRepository
import com.nichita.myvoyage.data.repository.RatesRepository
import com.nichita.myvoyage.data.repository.VehicleRepository
import com.nichita.myvoyage.data.repository.VoyageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application — точка ручного DI (без Hilt, чтобы держать минимум зависимостей).
 * БД и репозитории создаются лениво и живут всё время работы приложения.
 */
class MyVoyageApp : Application() {

    private val database by lazy { AppDatabase.getInstance(this) }

    /** Долгоживущая область для фоновых задач уровня приложения. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val repository: VoyageRepository by lazy {
        VoyageRepository(
            tripDao = database.tripDao(),
            expenseDao = database.expenseDao(),
            fuelDao = database.fuelDao()
        )
    }

    /** Курсы валют: онлайн-обновление НБМ + офлайн-кэш. */
    val ratesRepository: RatesRepository by lazy {
        RatesRepository(dao = database.exchangeRateDao())
    }

    /** Офисы и их помесячные расходы. */
    val officeRepository: OfficeRepository by lazy {
        OfficeRepository(dao = database.officeDao())
    }

    /** Автомобили и их помесячные расходы. */
    val vehicleRepository: VehicleRepository by lazy {
        VehicleRepository(dao = database.vehicleDao())
    }

    override fun onCreate() {
        super.onCreate()
        // Пытаемся обновить курсы при запуске. Есть интернет — курсы свежие,
        // нет — тихо остаётся последний сохранённый кэш (см. RatesRepository).
        appScope.launch { ratesRepository.refresh() }
    }
}
