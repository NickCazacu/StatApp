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

/**
 * v4 → v5:
 *  - новые таблицы `vehicles` и `vehicle_expenses` — учёт помесячных расходов
 *    по автомобилям (по образцу офисов). Существующие данные не затрагиваются.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `vehicles` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `plate` TEXT NOT NULL,
                `currency` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `vehicle_expenses` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `vehicleId` INTEGER NOT NULL,
                `year` INTEGER NOT NULL,
                `month` INTEGER NOT NULL,
                `category` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `note` TEXT NOT NULL,
                FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_vehicle_expenses_vehicleId` ON `vehicle_expenses` (`vehicleId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_vehicle_expenses_vehicleId_year_month` " +
                "ON `vehicle_expenses` (`vehicleId`, `year`, `month`)"
        )
    }
}

/**
 * v5 → v6:
 *  - `office_expenses` и `vehicle_expenses`: новая колонка `currency` — валюта
 *    конкретной траты (для конвертации в базовую валюту по курсу НБМ).
 *    Существующим тратам проставляется валюта их офиса/автомобиля.
 *    Приём «create → copy → drop → rename» — как в остальных миграциях.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- office_expenses: + currency (из родительского офиса) ---
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `office_expenses_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `officeId` INTEGER NOT NULL,
                `year` INTEGER NOT NULL,
                `month` INTEGER NOT NULL,
                `category` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `currency` TEXT NOT NULL,
                `note` TEXT NOT NULL,
                FOREIGN KEY(`officeId`) REFERENCES `offices`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `office_expenses_new`
                (`id`, `officeId`, `year`, `month`, `category`, `amount`, `currency`, `note`)
            SELECT e.`id`, e.`officeId`, e.`year`, e.`month`, e.`category`, e.`amount`,
                   COALESCE(o.`currency`, 'MDL'), e.`note`
            FROM `office_expenses` e
            LEFT JOIN `offices` o ON e.`officeId` = o.`id`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `office_expenses`")
        db.execSQL("ALTER TABLE `office_expenses_new` RENAME TO `office_expenses`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_office_expenses_officeId` ON `office_expenses` (`officeId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_office_expenses_officeId_year_month` " +
                "ON `office_expenses` (`officeId`, `year`, `month`)"
        )

        // --- vehicle_expenses: + currency (из родительского автомобиля) ---
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `vehicle_expenses_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `vehicleId` INTEGER NOT NULL,
                `year` INTEGER NOT NULL,
                `month` INTEGER NOT NULL,
                `category` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `currency` TEXT NOT NULL,
                `note` TEXT NOT NULL,
                FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `vehicle_expenses_new`
                (`id`, `vehicleId`, `year`, `month`, `category`, `amount`, `currency`, `note`)
            SELECT e.`id`, e.`vehicleId`, e.`year`, e.`month`, e.`category`, e.`amount`,
                   COALESCE(v.`currency`, 'MDL'), e.`note`
            FROM `vehicle_expenses` e
            LEFT JOIN `vehicles` v ON e.`vehicleId` = v.`id`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `vehicle_expenses`")
        db.execSQL("ALTER TABLE `vehicle_expenses_new` RENAME TO `vehicle_expenses`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_vehicle_expenses_vehicleId` ON `vehicle_expenses` (`vehicleId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_vehicle_expenses_vehicleId_year_month` " +
                "ON `vehicle_expenses` (`vehicleId`, `year`, `month`)"
        )
    }
}

/**
 * v6 → v7:
 *  - `fuel_entries`: новая колонка `currency` — валюта конкретной заправки
 *    (для конвертации в валюту рейса по курсу НБМ, как у расходов).
 *    Существующим заправкам проставляется валюта их рейса.
 *    Приём «create → copy → drop → rename» — как в остальных миграциях.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `fuel_entries_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `tripId` INTEGER NOT NULL,
                `date` INTEGER NOT NULL,
                `cost` REAL NOT NULL,
                `currency` TEXT NOT NULL,
                `fuelType` TEXT NOT NULL,
                FOREIGN KEY(`tripId`) REFERENCES `trips`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `fuel_entries_new` (`id`, `tripId`, `date`, `cost`, `currency`, `fuelType`)
            SELECT f.`id`, f.`tripId`, f.`date`, f.`cost`,
                   COALESCE(t.`currency`, 'EUR'), f.`fuelType`
            FROM `fuel_entries` f
            LEFT JOIN `trips` t ON f.`tripId` = t.`id`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `fuel_entries`")
        db.execSQL("ALTER TABLE `fuel_entries_new` RENAME TO `fuel_entries`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_fuel_entries_tripId` ON `fuel_entries` (`tripId`)"
        )
    }
}

/** Все миграции приложения — передаются в RoomDatabase.Builder. */
val ALL_MIGRATIONS: Array<Migration> =
    arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
