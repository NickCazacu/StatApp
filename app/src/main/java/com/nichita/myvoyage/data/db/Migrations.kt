package com.nichita.myvoyage.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Миграции схемы Room. Каждая переносит данные пользователя без потерь —
 * в отличие от разрушающего fallback, который обнулял БД при обновлении.
 *
 * Применяемый приём «create → copy → drop → rename» портативен и работает на
 * всех версиях SQLite (в т.ч. на старых Android с minSdk 24, где нет
 * `ALTER TABLE DROP COLUMN`). Во время миграции Room держит внешние ключи
 * выключенными (PRAGMA foreign_keys включается уже после, в onOpen), поэтому
 * пересоздание родительской таблицы `trips` не вызывает каскадного удаления
 * дочерних строк.
 */

/**
 * v1 → v2:
 *  - `trips`: удалена колонка `budget` (бюджет рейса больше не хранится).
 *  - `fuel_entries`: вместо `liters` + `pricePerLiter` + `odometer` — одна
 *    колонка `cost`; стоимость заправки переносится как `liters * pricePerLiter`.
 *    Индекс по `odometer` удалён (колонки больше нет).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- trips: убираем колонку budget ---
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `trips_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `destination` TEXT NOT NULL,
                `startDate` INTEGER NOT NULL,
                `endDate` INTEGER,
                `currency` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `trips_new` (`id`, `destination`, `startDate`, `endDate`, `currency`)
            SELECT `id`, `destination`, `startDate`, `endDate`, `currency` FROM `trips`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `trips`")
        db.execSQL("ALTER TABLE `trips_new` RENAME TO `trips`")

        // --- fuel_entries: liters+pricePerLiter+odometer -> cost ---
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `fuel_entries_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `tripId` INTEGER NOT NULL,
                `date` INTEGER NOT NULL,
                `cost` REAL NOT NULL,
                `fuelType` TEXT NOT NULL,
                FOREIGN KEY(`tripId`) REFERENCES `trips`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `fuel_entries_new` (`id`, `tripId`, `date`, `cost`, `fuelType`)
            SELECT `id`, `tripId`, `date`, `liters` * `pricePerLiter`, `fuelType` FROM `fuel_entries`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `fuel_entries`")
        db.execSQL("ALTER TABLE `fuel_entries_new` RENAME TO `fuel_entries`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_fuel_entries_tripId` ON `fuel_entries` (`tripId`)"
        )
    }
}

/**
 * v2 → v3:
 *  - новая таблица `exchange_rates` — офлайн-кэш курсов валют (НБМ).
 *    Существующие данные пользователя не затрагиваются.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exchange_rates` (
                `code` TEXT NOT NULL,
                `mdlPerUnit` REAL NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`code`)
            )
            """.trimIndent()
        )
    }
}

/**
 * v3 → v4:
 *  - новые таблицы `offices` и `office_expenses` — учёт помесячных расходов
 *    по офисам. Существующие данные пользователя не затрагиваются.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `offices` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `address` TEXT NOT NULL,
                `currency` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `office_expenses` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `officeId` INTEGER NOT NULL,
                `year` INTEGER NOT NULL,
                `month` INTEGER NOT NULL,
                `category` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `note` TEXT NOT NULL,
                FOREIGN KEY(`officeId`) REFERENCES `offices`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_office_expenses_officeId` ON `office_expenses` (`officeId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_office_expenses_officeId_year_month` " +
                "ON `office_expenses` (`officeId`, `year`, `month`)"
        )
    }
}

/** Все миграции приложения — передаются в RoomDatabase.Builder. */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
