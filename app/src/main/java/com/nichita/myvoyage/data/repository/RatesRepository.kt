package com.nichita.myvoyage.data.repository

import com.nichita.myvoyage.data.db.ExchangeRateDao
import com.nichita.myvoyage.data.model.Currency
import com.nichita.myvoyage.data.model.ExchangeRate
import com.nichita.myvoyage.data.remote.BnmRatesService
import com.nichita.myvoyage.domain.CurrencyRates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Курсы валют: онлайн-обновление из НБМ + офлайн-кэш в Room.
 *
 * [observeRates] всегда отдаёт полный набор курсов (сохранённые поверх
 * [CurrencyRates.DEFAULTS]), поэтому конвертация в UI работает сразу, ещё до
 * первого успешного обновления. [refresh] тихо игнорирует отсутствие сети —
 * тогда остаётся последний сохранённый курс.
 */
class RatesRepository(
    private val dao: ExchangeRateDao,
    private val service: BnmRatesService = BnmRatesService()
) {

    /** Реактивный снимок курсов для конвертации сумм. */
    fun observeRates(): Flow<CurrencyRates> =
        dao.observeAll().map { rows -> toRates(rows) }

    /** Разовый снимок курсов (для расчётов вне реактивного UI — советы, экспорт). */
    suspend fun currentRates(): CurrencyRates = toRates(dao.getAll())

    /**
     * Пытается обновить курсы из сети. true — курсы обновлены, false — сеть
     * недоступна/ответ пуст (кэш не тронут).
     */
    suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        val fetched = service.fetchRates() ?: return@withContext false
        val now = System.currentTimeMillis()
        val rows = fetched.mapNotNull { (code, mdlPerUnit) ->
            if (Currency.fromCodeOrNull(code) == null) null
            else ExchangeRate(code = code, mdlPerUnit = mdlPerUnit, updatedAt = now)
        }
        if (rows.isEmpty()) return@withContext false
        dao.upsertAll(rows)
        true
    }

    private fun toRates(rows: List<ExchangeRate>): CurrencyRates {
        val map = CurrencyRates.DEFAULTS.toMutableMap()
        var latest: Long? = null
        rows.forEach { row ->
            val currency = Currency.fromCodeOrNull(row.code) ?: return@forEach
            map[currency] = row.mdlPerUnit
            latest = maxOf(latest ?: 0L, row.updatedAt)
        }
        return CurrencyRates(map, latest)
    }
}
