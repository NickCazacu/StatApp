package com.nichita.myvoyage.domain

import com.nichita.myvoyage.data.db.MonthTotal
import com.nichita.myvoyage.data.db.OfficeCategorySum
import com.nichita.myvoyage.data.model.Office
import com.nichita.myvoyage.data.model.OfficeCategory
import kotlin.math.roundToInt

/**
 * Входные данные для анализа советов по офису.
 * [monthTotals] — помесячные итоги в хронологическом порядке (как из DAO).
 */
data class OfficeTipsInput(
    val office: Office,
    val monthTotals: List<MonthTotal>,
    val categorySums: List<OfficeCategorySum>
)

/**
 * Rule-based анализатор расходов офиса. Никакого AI и сети —
 * только пороги и арифметика (по образцу TipsAnalyzer для рейсов).
 */
object OfficeTipsAnalyzer {

    // Пороги срабатывания правил
    private const val MONTH_GROWTH = 0.15          // рост месяц к месяцу на 15%
    private const val MONTH_DROP = -0.15           // экономия месяц к месяцу на 15%
    private const val ABOVE_AVERAGE = 0.20         // месяц дороже среднего на 20%
    private const val DOMINANT_CATEGORY = 0.50     // категория «съедает» больше половины
    private const val RISING_MONTHS = 3            // подряд растущих месяцев для тренда

    fun analyze(input: OfficeTipsInput): List<Tip> {
        val tips = mutableListOf<Tip>()
        val symbol = input.office.currency.symbol

        ruleMonthOverMonth(input, symbol, tips)
        ruleRisingTrend(input, tips)
        ruleAboveAverage(input, symbol, tips)
        ruleDominantCategory(input, tips)

        if (tips.isEmpty()) {
            tips += Tip(
                TipSeverity.POSITIVE,
                "Всё в порядке",
                "Явных проблем с расходами офиса не обнаружено. Так держать!"
            )
        }
        return tips
    }

    /** Последний месяц заметно дороже/дешевле предыдущего. */
    private fun ruleMonthOverMonth(input: OfficeTipsInput, symbol: String, out: MutableList<Tip>) {
        val months = input.monthTotals
        if (months.size < 2) return
        val prev = months[months.size - 2]
        val last = months.last()
        if (prev.total <= 0.0) return

        val diff = (last.total - prev.total) / prev.total
        when {
            diff >= MONTH_GROWTH -> out += Tip(
                TipSeverity.WARNING,
                "Расходы выросли на ${(diff * 100).roundToInt()}%",
                "За последний месяц офис потратил ${money(last.total, symbol)} — " +
                    "на ${(diff * 100).roundToInt()}% больше, чем месяцем ранее " +
                    "(${money(prev.total, symbol)}). Проверьте, какая категория дала рост."
            )
            diff <= MONTH_DROP -> out += Tip(
                TipSeverity.POSITIVE,
                "Экономия ${(-diff * 100).roundToInt()}% за месяц",
                "Расходы за последний месяц (${money(last.total, symbol)}) ниже " +
                    "предыдущего (${money(prev.total, symbol)}). Отличная динамика!"
            )
        }
    }

    /** Расходы растут несколько месяцев подряд — устойчивый тренд. */
    private fun ruleRisingTrend(input: OfficeTipsInput, out: MutableList<Tip>) {
        val months = input.monthTotals
        if (months.size < RISING_MONTHS + 1) return
        val tail = months.takeLast(RISING_MONTHS + 1)
        val rising = tail.zipWithNext().all { (a, b) -> b.total > a.total }
        if (rising) {
            out += Tip(
                TipSeverity.ALERT,
                "Расходы растут $RISING_MONTHS месяца подряд",
                "Затраты офиса устойчиво увеличиваются. Стоит пересмотреть " +
                    "регулярные статьи: договор аренды, тарифы на связь и коммунальные, " +
                    "подписки и обслуживание."
            )
        }
    }

    /** Последний месяц дороже среднего по всей истории. */
    private fun ruleAboveAverage(input: OfficeTipsInput, symbol: String, out: MutableList<Tip>) {
        val months = input.monthTotals
        if (months.size < 3) return
        val last = months.last()
        val others = months.dropLast(1).map { it.total }.filter { it > 0.0 }
        if (others.isEmpty()) return
        val avg = others.average()
        if (avg <= 0.0) return

        val diff = (last.total - avg) / avg
        if (diff >= ABOVE_AVERAGE) {
            out += Tip(
                TipSeverity.WARNING,
                "Дороже среднего на ${(diff * 100).roundToInt()}%",
                "Последний месяц дороже среднего по офису " +
                    "(${money(avg, symbol)}) на ${(diff * 100).roundToInt()}%."
            )
        }
    }

    /** Одна категория «съедает» больше половины бюджета — подсказка по оптимизации. */
    private fun ruleDominantCategory(input: OfficeTipsInput, out: MutableList<Tip>) {
        val sums = input.categorySums.filter { it.total > 0.0 }
        val total = sums.sumOf { it.total }
        if (total <= 0.0) return
        val top = sums.maxByOrNull { it.total } ?: return
        val share = top.total / total
        if (share < DOMINANT_CATEGORY) return

        val advice = when (top.category) {
            OfficeCategory.RENT ->
                "Попробуйте пересогласовать ставку аренды, рассмотреть помещение " +
                    "поменьше или сдать часть площади в субаренду."
            OfficeCategory.UTILITIES ->
                "Проверьте тарифы и энергопотребление: LED-освещение, режим " +
                    "отопления/кондиционера вне рабочих часов заметно снижают счета."
            OfficeCategory.SALARIES ->
                "Зарплаты — основная статья. Убедитесь, что нагрузка распределена " +
                    "эффективно; возможно, часть задач выгоднее отдать на аутсорс."
            OfficeCategory.INTERNET ->
                "Сравните тарифы провайдеров и операторов — пакетные корпоративные " +
                    "предложения часто дешевле текущих."
            OfficeCategory.SUPPLIES ->
                "Закупайте расходники оптом и сравнивайте поставщиков — на канцелярии " +
                    "легко экономить 10–20%."
            OfficeCategory.MAINTENANCE ->
                "Частый ремонт — повод заключить сервисный договор или обновить " +
                    "проблемное оборудование вместо постоянных починок."
            OfficeCategory.CLEANING ->
                "Сравните стоимость клининговых компаний или пересмотрите график уборки."
            OfficeCategory.OTHER ->
                "Разберите «Прочее» на конкретные категории — так проще увидеть, " +
                    "куда уходят деньги."
        }
        out += Tip(
            TipSeverity.INFO,
            "«${top.category.title}» — ${(share * 100).roundToInt()}% всех расходов",
            advice
        )
    }

    // --- Форматирование (без зависимостей от Android, чтобы было тестируемо) ---
    private fun money(value: Double, symbol: String): String =
        "${(value * 100).roundToInt() / 100.0} $symbol"
}
