package com.nichita.myvoyage.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nichita.myvoyage.data.model.ExchangeRate
import com.nichita.myvoyage.data.model.Expense
import com.nichita.myvoyage.data.model.FuelEntry
import com.nichita.myvoyage.data.model.Trip

/**
 * Единая офлайн-БД приложения (Room).
 *
 * Создаётся как синглтон, чтобы не открывать несколько соединений
 * (важно для производительности на слабых устройствах).
 */
@Database(
    entities = [Trip::class, Expense::class, FuelEntry::class, ExchangeRate::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun fuelDao(): FuelDao
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myvoyage.db"
                )
                    // Нормальные миграции: данные пользователя сохраняются при
                    // обновлении приложения (см. Migrations.kt). Для каждой новой
                    // версии схемы нужно добавить соответствующую Migration.
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
