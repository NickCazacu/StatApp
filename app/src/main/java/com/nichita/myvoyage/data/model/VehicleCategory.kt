package com.nichita.myvoyage.data.model

/**
 * Категория расхода автомобиля.
 * [title] — отображаемое название на русском (используется в UI).
 *
 * Хранится в Room как строка (имя константы) через TypeConverter,
 * поэтому переименовывать константы без миграции нельзя.
 */
enum class VehicleCategory(val title: String) {
    FUEL("Топливо"),
    MAINTENANCE("ТО/ремонт"),
    INSURANCE("Страховка"),
    TAXES("Налоги/сборы"),
    PARKING("Парковка"),
    TIRES("Шины"),
    WASHING("Мойка"),
    OTHER("Прочее");

    companion object {
        /** Безопасный разбор из строки БД (на случай неизвестного значения). */
        fun fromName(value: String?): VehicleCategory =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}
