package com.nichita.myvoyage.domain

import com.nichita.myvoyage.data.db.MonthTotal
import com.nichita.myvoyage.data.db.VehicleCategorySum
import com.nichita.myvoyage.data.model.Vehicle
import com.nichita.myvoyage.data.model.VehicleCategory
import kotlin.math.roundToInt

/**
 * Входные данные для анализа советов по автомобилю.
 * [monthTotals] — помесячные итоги в хронологическом порядке (как из DAO).
 */
data class VehicleTipsInput(
    val vehicle: Vehicle,
    val monthTotals: List<MonthTotal>,
    val categorySums: List<VehicleCategorySum>
)

/**
 * Rule-based анализатор расходов автомобиля. Никакого AI и сети —
 * только пороги и арифметика (по образцу OfficeTipsAnalyzer).
 */
object VehicleTipsAnalyzer {

    // Пороги срабатывания правил
    private const val MONTH_GROWTH = 0.15          // рост месяц к месяцу на 15%
    private const val MONTH_DROP = -0.15           // экономия месяц к месяцу на 15%
    private const val ABOVE_AVERAGE = 0.20         // месяц дороже среднего на 20%
    private const val DOMINANT_CATEGORY = 0.50     // категория «съедает» больше половины
    private const val RISING_MONTHS = 3            // подряд растущих месяцев для тренда

    fun analyze(input: VehicleTipsInput): List<Tip> {
        val tips = mutableListOf<Tip>()
        val symbol = input.vehicle.currency.symbol

        ruleMonthOverMonth(input, symbol, tips)
        ruleRisingTrend(input, tips)
        ruleAboveAverage(input, symbol, tips)
        ruleDominantCategory(input, tips)

        if (tips.isEmpty()) {
            tips += Tip(
                TipSeverity.POSITIVE,
                "Всё в порядке",
                "Явных проблем с расходами на автомобиль не обнаружено. Так держать!"
            )
        }
        return tips
    }

    /** Последний месяц заметно дороже/дешевле предыдущего. */
    private fun ruleMonthOverMonth(
        input: VehicleTipsInput,
        symbol: String,
        out: MutableList<Tip>
    ) {
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
                "За последний месяц на автомобиль ушло ${money(last.total, symbol)} — " +
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
    private fun ruleRisingTrend(input: VehicleTipsInput, out: MutableList<Tip>) {
        val months = input.monthTotals
        if (months.size < RISING_MONTHS + 1) return
        val tail = months.takeLast(RISING_MONTHS + 1)
        val rising = tail.zipWithNext().all { (a, b) -> b.total > a.total }
        if (rising) {
            out += Tip(
                TipSeverity.ALERT,
                "Расходы растут $RISING_MONTHS месяца подряд",
                "Затраты на автомобиль устойчиво увеличиваются. Если растут ремонт " +
                    "и обслуживание — возможно, машина стареет и стоит просчитать " +
                    "замену; если топливо — проверьте расход и маршруты."
            )
        }
    }

    /** Последний месяц дороже среднего по всей истории. */
    private fun ruleAboveAverage(
        input: VehicleTipsInput,
        symbol: String,
        out: MutableList<Tip>
    ) {
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
                "Последний месяц дороже среднего по этому автомобилю " +
                    "(${money(avg, symbol)}) на ${(diff * 100).roundToInt()}%."
            )
        }
    }

    /** Одна категория «съедает» больше половины бюджета — подсказка по оптимизации. */
    private fun ruleDominantCategory(input: VehicleTipsInput, out: MutableList<Tip>) {
        val sums = input.categorySums.filter { it.total > 0.0 }
        val total = sums.sumOf { it.total }
        if (total <= 0.0) return
        val top = sums.maxByOrNull { it.total } ?: return
        val share = top.total / total
        if (share < DOMINANT_CATEGORY) return

        val advice = when (top.category) {
            VehicleCategory.FUEL ->
                "Топливо — основная статья. Сравнивайте цены АЗС по маршруту, " +
                    "используйте топливные карты со скидкой и следите за расходом — " +
                    "рост может говорить о неисправности."
            VehicleCategory.MAINTENANCE ->
                "Много уходит на ремонт. Плановое ТО дешевле аварийного; если починки " +
                    "постоянные — просчитайте, не выгоднее ли обновить автомобиль."
            VehicleCategory.INSURANCE ->
                "Сравните предложения страховых компаний перед продлением — разница " +
                    "по КАСКО/ОСАГО между компаниями бывает существенной."
            VehicleCategory.TAXES ->
                "Налоги и сборы — фиксированная статья, но проверьте, нет ли льгот " +
                    "или переплат по срокам."
            VehicleCategory.PARKING ->
                "Парковка обходится дорого. Месячный абонемент обычно дешевле " +
                    "почасовой оплаты."
            VehicleCategory.TIRES ->
                "Шины: покупка в межсезонье и своевременная перестановка/балансировка " +
                    "продлевают срок службы."
            VehicleCategory.WASHING ->
                "Мойка: абонементы и пакеты обычно выгоднее разовых визитов."
            VehicleCategory.OTHER ->
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
