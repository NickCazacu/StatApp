package com.nichita.myvoyage.data.model

/**
 * Валюта. Набор ориентирован на еженедельные авто-рейсы по Европе/региону.
 * [code] — ISO-код (хранится в БД), [symbol] — символ для отображения.
 *
 * Расходы можно вводить в любой из этих валют. Итоги рейса сводятся в валюту
 * рейса по официальному курсу НБМ (см. [com.nichita.myvoyage.domain.CurrencyRates]):
 * курс обновляется при наличии интернета и кэшируется для офлайна.
 */
enum class Currency(val code: String, val symbol: String) {
    EUR("EUR", "€"),   // Еврозона (в т.ч. Словакия)
    RON("RON", "lei"), // Румыния
    MDL("MDL", "L"),   // Молдова
    USD("USD", "$"),
    PLN("PLN", "zł"),  // Польша
    HUF("HUF", "Ft"),  // Венгрия
    BYN("BYN", "Br");  // Беларусь

    companion object {
        val DEFAULT = EUR

        /** По коду с откатом на [DEFAULT] (для чтения из БД, где код всегда валиден). */
        fun fromCode(value: String?): Currency =
            entries.firstOrNull { it.code == value } ?: DEFAULT

        /** По коду без отката — null для неизвестных валют (при разборе ответа НБМ). */
        fun fromCodeOrNull(value: String?): Currency? =
            entries.firstOrNull { it.code == value }
    }
}
