package com.nichita.myvoyage

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nichita.myvoyage.data.db.AppDatabase
import com.nichita.myvoyage.data.db.MIGRATION_1_2
import com.nichita.myvoyage.data.db.MIGRATION_2_3
import com.nichita.myvoyage.data.db.MIGRATION_3_4
import com.nichita.myvoyage.data.db.MIGRATION_4_5
import com.nichita.myvoyage.data.db.MIGRATION_5_6
import com.nichita.myvoyage.data.db.MIGRATION_6_7
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Проверяет, что миграции Room сохраняют данные пользователя (не пересоздают БД).
 * Использует экспортированные схемы из `app/schemas` (подключены как ассеты теста).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate1To2_preservesData_andComputesFuelCost() {
        // --- Создаём БД версии 1 и наполняем данными старой схемы ---
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO trips (id, destination, startDate, endDate, budget, currency) " +
                    "VALUES (1, 'Кишинёв → Бухарест', 1000, 2000, 500.0, 'EUR')"
            )
            // cost должен получиться liters * pricePerLiter = 40 * 1.5 = 60.0
            execSQL(
                "INSERT INTO fuel_entries (id, tripId, date, liters, pricePerLiter, odometer, fuelType) " +
                    "VALUES (1, 1, 1500, 40.0, 1.5, 123456.0, 'DIESEL')"
            )
            execSQL(
                "INSERT INTO expenses (id, tripId, amount, currency, category, date, note) " +
                    "VALUES (1, 1, 25.0, 'EUR', 'FOOD', 1600, 'обед')"
            )
            close()
        }

        // --- Применяем миграцию и валидируем против схемы версии 2 ---
        val db = helper.runMigrationsAndValidate(testDb, 2, true, MIGRATION_1_2)

        // Рейс сохранился (и колонка budget исчезла — валидация это уже проверила).
        db.query("SELECT destination, currency FROM trips WHERE id = 1").use { c ->
            assertTrue("Рейс должен сохраниться", c.moveToFirst())
            assertEquals("Кишинёв → Бухарест", c.getString(0))
            assertEquals("EUR", c.getString(1))
        }

        // Заправка: стоимость пересчитана из литров × цена за литр.
        db.query("SELECT cost, fuelType FROM fuel_entries WHERE id = 1").use { c ->
            assertTrue("Заправка должна сохраниться", c.moveToFirst())
            assertEquals(60.0, c.getDouble(0), 0.001)
            assertEquals("DIESEL", c.getString(1))
        }

        // Расход не затронут.
        db.query("SELECT amount, note FROM expenses WHERE id = 1").use { c ->
            assertTrue("Расход должен сохраниться", c.moveToFirst())
            assertEquals(25.0, c.getDouble(0), 0.001)
            assertEquals("обед", c.getString(1))
        }

        db.close()
    }

    @Test
    fun migrate2To3_preservesData_andAddsRatesTable() {
        // --- Версия 2: наполняем данными (без таблицы курсов) ---
        helper.createDatabase(testDb, 2).apply {
            execSQL(
                "INSERT INTO trips (id, destination, startDate, endDate, currency) " +
                    "VALUES (1, 'Кишинёв → Варшава', 1000, 2000, 'PLN')"
            )
            execSQL(
                "INSERT INTO expenses (id, tripId, amount, currency, category, date, note) " +
                    "VALUES (1, 1, 50.0, 'EUR', 'FOOD', 1600, 'ужин')"
            )
            close()
        }

        // --- Применяем миграцию и валидируем против схемы версии 3 ---
        val db = helper.runMigrationsAndValidate(testDb, 3, true, MIGRATION_2_3)

        // Данные пользователя сохранились.
        db.query("SELECT destination, currency FROM trips WHERE id = 1").use { c ->
            assertTrue("Рейс должен сохраниться", c.moveToFirst())
            assertEquals("Кишинёв → Варшава", c.getString(0))
            assertEquals("PLN", c.getString(1))
        }
        db.query("SELECT amount, currency FROM expenses WHERE id = 1").use { c ->
            assertTrue("Расход должен сохраниться", c.moveToFirst())
            assertEquals(50.0, c.getDouble(0), 0.001)
            assertEquals("EUR", c.getString(1))
        }

        // Новая таблица курсов доступна для записи.
        db.execSQL("INSERT INTO exchange_rates (code, mdlPerUnit, updatedAt) VALUES ('EUR', 20.0, 123)")
        db.query("SELECT mdlPerUnit FROM exchange_rates WHERE code = 'EUR'").use { c ->
            assertTrue("Курс должен записаться", c.moveToFirst())
            assertEquals(20.0, c.getDouble(0), 0.001)
        }

        db.close()
    }

    @Test
    fun migrate3To4_preservesData_andAddsOfficeTables() {
        // --- Версия 3: наполняем данными (без таблиц офисов) ---
        helper.createDatabase(testDb, 3).apply {
            execSQL(
                "INSERT INTO trips (id, destination, startDate, endDate, currency) " +
                    "VALUES (1, 'Кишинёв → Прага', 1000, 2000, 'EUR')"
            )
            execSQL(
                "INSERT INTO expenses (id, tripId, amount, currency, category, date, note) " +
                    "VALUES (1, 1, 30.0, 'EUR', 'FOOD', 1600, 'обед')"
            )
            close()
        }

        // --- Применяем миграцию и валидируем против схемы версии 4 ---
        val db = helper.runMigrationsAndValidate(testDb, 4, true, MIGRATION_3_4)

        // Данные пользователя сохранились.
        db.query("SELECT destination FROM trips WHERE id = 1").use { c ->
            assertTrue("Рейс должен сохраниться", c.moveToFirst())
            assertEquals("Кишинёв → Прага", c.getString(0))
        }
        db.query("SELECT amount FROM expenses WHERE id = 1").use { c ->
            assertTrue("Расход должен сохраниться", c.moveToFirst())
            assertEquals(30.0, c.getDouble(0), 0.001)
        }

        // Новые таблицы офисов доступны для записи (включая каскадную связь).
        db.execSQL(
            "INSERT INTO offices (id, name, address, currency) " +
                "VALUES (1, 'Офис Кишинёв', 'бул. Штефан чел Маре 1', 'MDL')"
        )
        db.execSQL(
            "INSERT INTO office_expenses (id, officeId, year, month, category, amount, note) " +
                "VALUES (1, 1, 2026, 7, 'RENT', 15000.0, 'аренда за июль')"
        )
        db.query(
            "SELECT amount, category FROM office_expenses WHERE officeId = 1"
        ).use { c ->
            assertTrue("Расход офиса должен записаться", c.moveToFirst())
            assertEquals(15000.0, c.getDouble(0), 0.001)
            assertEquals("RENT", c.getString(1))
        }

        db.close()
    }

    @Test
    fun migrate4To5_preservesData_andAddsVehicleTables() {
        // --- Версия 4: наполняем данными (без таблиц автомобилей) ---
        helper.createDatabase(testDb, 4).apply {
            execSQL(
                "INSERT INTO trips (id, destination, startDate, endDate, currency) " +
                    "VALUES (1, 'Кишинёв → Вена', 1000, 2000, 'EUR')"
            )
            execSQL(
                "INSERT INTO offices (id, name, address, currency) " +
                    "VALUES (1, 'Офис Кишинёв', '', 'MDL')"
            )
            execSQL(
                "INSERT INTO office_expenses (id, officeId, year, month, category, amount, note) " +
                    "VALUES (1, 1, 2026, 6, 'RENT', 12000.0, '')"
            )
            close()
        }

        // --- Применяем миграцию и валидируем против схемы версии 5 ---
        val db = helper.runMigrationsAndValidate(testDb, 5, true, MIGRATION_4_5)

        // Данные пользователя сохранились.
        db.query("SELECT destination FROM trips WHERE id = 1").use { c ->
            assertTrue("Рейс должен сохраниться", c.moveToFirst())
            assertEquals("Кишинёв → Вена", c.getString(0))
        }
        db.query("SELECT amount FROM office_expenses WHERE id = 1").use { c ->
            assertTrue("Расход офиса должен сохраниться", c.moveToFirst())
            assertEquals(12000.0, c.getDouble(0), 0.001)
        }

        // Новые таблицы автомобилей доступны для записи (включая каскадную связь).
        db.execSQL(
            "INSERT INTO vehicles (id, name, plate, currency) " +
                "VALUES (1, 'Mercedes Sprinter', 'ABC 123', 'MDL')"
        )
        db.execSQL(
            "INSERT INTO vehicle_expenses (id, vehicleId, year, month, category, amount, note) " +
                "VALUES (1, 1, 2026, 7, 'FUEL', 5000.0, 'дизель за июль')"
        )
        db.query(
            "SELECT amount, category FROM vehicle_expenses WHERE vehicleId = 1"
        ).use { c ->
            assertTrue("Расход автомобиля должен записаться", c.moveToFirst())
            assertEquals(5000.0, c.getDouble(0), 0.001)
            assertEquals("FUEL", c.getString(1))
        }

        db.close()
    }

    @Test
    fun migrate5To6_addsCurrency_backfilledFromParent() {
        // --- Версия 5: траты офисов/авто ещё без собственной валюты ---
        helper.createDatabase(testDb, 5).apply {
            execSQL(
                "INSERT INTO offices (id, name, address, currency) " +
                    "VALUES (1, 'Офис Кишинёв', '', 'EUR')"
            )
            execSQL(
                "INSERT INTO office_expenses (id, officeId, year, month, category, amount, note) " +
                    "VALUES (1, 1, 2026, 6, 'RENT', 700.0, 'аренда')"
            )
            execSQL(
                "INSERT INTO vehicles (id, name, plate, currency) " +
                    "VALUES (1, 'Mercedes Sprinter', '', 'MDL')"
            )
            execSQL(
                "INSERT INTO vehicle_expenses (id, vehicleId, year, month, category, amount, note) " +
                    "VALUES (1, 1, 2026, 7, 'FUEL', 5000.0, '')"
            )
            close()
        }

        // --- Применяем миграцию и валидируем против схемы версии 6 ---
        val db = helper.runMigrationsAndValidate(testDb, 6, true, MIGRATION_5_6)

        // Существующим тратам проставлена валюта их офиса/автомобиля.
        db.query(
            "SELECT amount, currency, note FROM office_expenses WHERE id = 1"
        ).use { c ->
            assertTrue("Расход офиса должен сохраниться", c.moveToFirst())
            assertEquals(700.0, c.getDouble(0), 0.001)
            assertEquals("EUR", c.getString(1))
            assertEquals("аренда", c.getString(2))
        }
        db.query(
            "SELECT amount, currency FROM vehicle_expenses WHERE id = 1"
        ).use { c ->
            assertTrue("Расход автомобиля должен сохраниться", c.moveToFirst())
            assertEquals(5000.0, c.getDouble(0), 0.001)
            assertEquals("MDL", c.getString(1))
        }

        // Новые траты пишутся уже со своей валютой.
        db.execSQL(
            "INSERT INTO office_expenses (id, officeId, year, month, category, amount, currency, note) " +
                "VALUES (2, 1, 2026, 7, 'UTILITIES', 3000.0, 'MDL', 'свет')"
        )
        db.query("SELECT currency FROM office_expenses WHERE id = 2").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("MDL", c.getString(0))
        }

        db.close()
    }

    @Test
    fun migrate6To7_addsFuelCurrency_backfilledFromTrip() {
        // --- Версия 6: заправки ещё без собственной валюты ---
        helper.createDatabase(testDb, 6).apply {
            execSQL(
                "INSERT INTO trips (id, destination, startDate, endDate, currency) " +
                    "VALUES (1, 'Кишинёв → Варшава', 1000, 2000, 'PLN')"
            )
            execSQL(
                "INSERT INTO fuel_entries (id, tripId, date, cost, fuelType) " +
                    "VALUES (1, 1, 1500, 250.0, 'DIESEL')"
            )
            close()
        }

        // --- Применяем миграцию и валидируем против схемы версии 7 ---
        val db = helper.runMigrationsAndValidate(testDb, 7, true, MIGRATION_6_7)

        // Существующей заправке проставлена валюта её рейса.
        db.query("SELECT cost, currency, fuelType FROM fuel_entries WHERE id = 1").use { c ->
            assertTrue("Заправка должна сохраниться", c.moveToFirst())
            assertEquals(250.0, c.getDouble(0), 0.001)
            assertEquals("PLN", c.getString(1))
            assertEquals("DIESEL", c.getString(2))
        }

        // Новые заправки пишутся уже со своей валютой.
        db.execSQL(
            "INSERT INTO fuel_entries (id, tripId, date, cost, currency, fuelType) " +
                "VALUES (2, 1, 1600, 60.0, 'EUR', 'DIESEL')"
        )
        db.query("SELECT currency FROM fuel_entries WHERE id = 2").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("EUR", c.getString(0))
        }

        db.close()
    }
}
