package com.nichita.myvoyage.export

/** Таблица отчёта: заголовок, шапка, веса колонок (для ширины) и строки. */
data class ReportTable(
    val title: String,
    val headers: List<String>,
    val weights: List<Float>,
    val rows: List<List<String>>
)

/**
 * Готовый к выводу отчёт — общий для PDF/Word/Excel. Изначально делался для
 * рейсов (отсюда имя), но структура универсальная: те же рендеры используются
 * для отчётов по офисам и автомобилям.
 * Все значения уже отформатированы строками, чтобы рендеры были простыми.
 */
data class TripReport(
    val title: String,
    val period: String,
    val totalText: String,
    val summaryLines: List<String>,
    val tables: List<ReportTable>,
    val generatedAt: String,
    /** Надзаголовок над названием: «Отчёт по рейсу/офису/автомобилю». */
    val kicker: String = "Отчёт по рейсу"
)

/** Безопасное имя файла на основе названия отчёта. */
fun TripReport.fileName(ext: String): String {
    val safe = title.replace(Regex("[^\\p{L}\\p{N}]+"), "_").trim('_').take(40)
        .ifEmpty { "trip" }
    return "MyVoyage_$safe.$ext"
}
