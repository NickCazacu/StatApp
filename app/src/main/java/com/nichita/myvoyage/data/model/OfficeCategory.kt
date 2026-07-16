package com.nichita.myvoyage.data.model

/**
 * Категория офисного расхода.
 * [title] — отображаемое название на русском (используется в UI).
 *
 * Хранится в Room как строка (имя константы) через TypeConverter,
 * поэтому переименовывать константы без миграции нельзя.
 */
enum class OfficeCategory(val title: String) {
    RENT("Аренда"),
    UTILITIES("Коммунальные услуги"),
    SALARIES("Зарплаты"),
    INTERNET("Интернет/связь"),
    SUPPLIES("Канцелярия/расходники"),
    MAINTENANCE("Ремонт/обслуживание"),
    CLEANING("Уборка"),
    OTHER("Прочее");

    companion object {
        /** Безопасный разбор из строки БД (на случай неизвестного значения). */
        fun fromName(value: String?): OfficeCategory =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}
