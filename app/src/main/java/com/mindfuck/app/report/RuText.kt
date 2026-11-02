package com.mindfuck.app.report

import java.time.LocalDate
import java.util.Locale

/** Russian wording for the PDF report: dates, severity words, period summaries. */
object RuText {
    private val MONTHS_GEN = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря",
    )
    private val MONTHS_SHORT = listOf(
        "янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек",
    )
    private val WEEKDAYS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    const val DASH = "–"

    enum class Gender(val none: String) {
        FEMININE("не отмечалась"),
        NEUTER("не отмечалось"),
        MASCULINE("не отмечался"),
        PLURAL("не отмечались"),
    }

    /** "лёгкая степень" etc. — feminine because it agrees with "степень", as in the sample. */
    val LEVEL_WORDS = listOf("нет симптома", "лёгкая", "умеренная", "тяжёлая")
    const val NO_DATA = "нет данных"

    fun weekday(d: LocalDate): String = WEEKDAYS[d.dayOfWeek.value - 1]
    fun monthGen(d: LocalDate): String = MONTHS_GEN[d.monthValue - 1]
    fun monthShort(d: LocalDate): String = MONTHS_SHORT[d.monthValue - 1]

    /** "24 октября" */
    fun dayLong(d: LocalDate): String = "${d.dayOfMonth} ${monthGen(d)}"

    /** "24 октября 2026 года" */
    fun dayFull(d: LocalDate): String = "${dayLong(d)} ${d.year} года"

    /**
     * "18–24 октября 2026 года", "30 октября – 5 ноября 2026 года",
     * "30 декабря 2026 – 5 января 2027 года", "18 октября 2026 года".
     */
    fun period(from: LocalDate, to: LocalDate): String = when {
        from == to -> dayFull(from)
        from.year != to.year -> "${dayLong(from)} ${from.year} $DASH ${dayLong(to)} ${to.year} года"
        from.monthValue != to.monthValue -> "${dayLong(from)} $DASH ${dayLong(to)} ${to.year} года"
        else -> "${from.dayOfMonth}$DASH${to.dayOfMonth} ${monthGen(to)} ${to.year} года"
    }

    /** "24 окт", "18–19 окт", "30 окт – 2 ноя" */
    fun rangeShort(from: LocalDate, to: LocalDate): String = when {
        from == to -> "${from.dayOfMonth} ${monthShort(from)}"
        from.monthValue == to.monthValue && from.year == to.year ->
            "${from.dayOfMonth}$DASH${to.dayOfMonth} ${monthShort(to)}"
        else -> "${from.dayOfMonth} ${monthShort(from)} $DASH ${to.dayOfMonth} ${monthShort(to)}"
    }

    fun levelWord(level: Int?, gender: Gender): String = when (level) {
        null -> NO_DATA
        0 -> gender.none
        else -> LEVEL_WORDS[level.coerceIn(1, 3)]
    }

    fun yesNoWord(value: Boolean?): String = when (value) {
        null -> NO_DATA
        true -> "да"
        false -> "нет"
    }

    /**
     * Glue consecutive days with the same value:
     * "18–19 окт: умеренная. 20 окт: тяжёлая. 21 окт: не отмечалась."
     */
    fun <T> summary(days: List<LocalDate>, values: List<T>, word: (T) -> String): String {
        require(days.size == values.size)
        if (days.isEmpty()) return ""
        val parts = ArrayList<String>()
        var start = 0
        for (i in 1..days.size) {
            val boundary = i == days.size || values[i] != values[start] ||
                days[i].toEpochDay() - days[i - 1].toEpochDay() != 1L
            if (boundary) {
                parts += "${rangeShort(days[start], days[i - 1])}: ${word(values[start])}."
                start = i
            }
        }
        return parts.joinToString(" ")
    }

    fun levelSummary(days: List<LocalDate>, values: List<Int?>, gender: Gender) =
        summary(days, values) { levelWord(it, gender) }

    fun yesNoSummary(days: List<LocalDate>, values: List<Boolean?>) =
        summary(days, values) { yesNoWord(it) }

    /** 1 день, 2 дня, 5 дней */
    fun plural(n: Int, one: String, few: String, many: String): String {
        val m100 = n % 100
        val m10 = n % 10
        return when {
            m100 in 11..14 -> many
            m10 == 1 -> one
            m10 in 2..4 -> few
            else -> many
        }
    }

    fun days(n: Int) = "$n ${plural(n, "день", "дня", "дней")}"

    /** 7.5 -> "7,5", 8.0 -> "8" */
    fun hours(v: Float): String =
        if (v % 1f == 0f) v.toInt().toString() else String.format(Locale.US, "%.1f", v).replace('.', ',')

    /** Split notes into bullet lines, dropping user-typed bullet markers. */
    fun bullets(notes: String): List<String> = notes.lines()
        .map { it.trim().trimStart('•', '-', '*', '—', '–', '·').trim() }
        .filter { it.isNotEmpty() }
}
