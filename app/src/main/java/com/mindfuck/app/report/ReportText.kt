package com.mindfuck.app.report

import com.mindfuck.app.report.RuText.Gender
import java.time.LocalDate
import java.util.Locale

/** Language of the PDF report. Russian is the original layout; English mirrors it. */
enum class ReportLanguage(val label: String) {
    RU("Русский"),
    EN("English");

    val text: ReportText
        get() = when (this) {
            RU -> RuReportText
            EN -> EnReportText
        }
}

/** Every piece of wording the PDF needs. [Gender] only matters for Russian. */
interface ReportText {
    val title: String
    fun periodLine(from: LocalDate, to: LocalDate): String
    fun page(n: Int): String

    val legendHeading: String
    val legendValue: String
    val legendLevel: String
    val legendMarked: String
    val legendLevels: List<String>
    val legendMarks: List<String>

    val yes: String
    val no: String

    val dayHeader: String
    fun weekday(d: LocalDate): String
    fun rangeShort(from: LocalDate, to: LocalDate): String
    fun dayLong(d: LocalDate): String
    fun days(n: Int): String
    fun hours(v: Float): String

    val depression: String
    val elevated: String
    val irritability: String
    val anxiety: String
    val sleepRow: String
    val psychotic: String
    val therapy: String
    val dayTableHeading: String

    val summaryHeading: String
    val summaryIndicator: String
    val summaryMarked: String
    fun levelSummary(days: List<LocalDate>, values: List<Int?>, gender: Gender): String
    fun yesNoSummary(days: List<LocalDate>, values: List<Boolean?>): String

    val sleepHeading: String
    val sleepNoData: String
    fun sleepAverage(h: String): String
    fun sleepMinMax(min: String, minDay: LocalDate, max: String, maxDay: LocalDate): String
    fun sleepLogged(logged: Int, total: Int): String

    val medsHeading: String
    val medsNone: String
    fun medDoses(parts: List<Pair<String, Int>>): String
    fun medTaken(name: String, taken: Int, total: Int, pct: Int, doses: String): String

    val chartsHeading: String
    val noData: String
    fun sleepChartTitle(avg: String?): String
    val symptomsChartTitle: String
    val chartLevels: List<String>
    val yesNoChartTitle: String
    val medsChartTitle: String
    val medTakenMark: String
    val medNotTakenMark: String
    val doseLower: String
    val doseHigher: String
    val doseChange: String

    val notesHeading: String
    fun notesDay(d: LocalDate): String
    fun notesLastDay(d: LocalDate): String
}

object RuReportText : ReportText {
    override val title = "Дневник настроения и симптомов"
    override fun periodLine(from: LocalDate, to: LocalDate) = "Период: ${RuText.period(from, to)}"
    override fun page(n: Int) = "стр. $n"

    override val legendHeading = "Шкала значений в таблице"
    override val legendValue = "Значение"
    override val legendLevel = "Уровень"
    override val legendMarked = "Как отмечено"
    override val legendLevels = listOf("нет симптома", "лёгкая степень", "умеренная степень", "тяжёлая степень")
    override val legendMarks = listOf("белая клетка", "светло-серая клетка", "серая клетка", "чёрная клетка")

    override val yes = "да"
    override val no = "нет"

    override val dayHeader = "День"
    override fun weekday(d: LocalDate) = RuText.weekday(d)
    override fun rangeShort(from: LocalDate, to: LocalDate) = RuText.rangeShort(from, to)
    override fun dayLong(d: LocalDate) = RuText.dayLong(d)
    override fun days(n: Int) = RuText.days(n)
    override fun hours(v: Float) = RuText.hours(v)

    override val depression = "Депрессия"
    override val elevated = "Повышенное настроение"
    override val irritability = "Раздражительность"
    override val anxiety = "Тревога"
    override val sleepRow = "Сон, ч"
    override val psychotic = "Психотические симптомы"
    override val therapy = "Психотерапия"
    override val dayTableHeading = "Таблица по дням"

    override val summaryHeading = "Краткое описание по периодам"
    override val summaryIndicator = "Показатель"
    override val summaryMarked = "Что отмечено"
    override fun levelSummary(days: List<LocalDate>, values: List<Int?>, gender: Gender) =
        RuText.levelSummary(days, values, gender)
    override fun yesNoSummary(days: List<LocalDate>, values: List<Boolean?>) = RuText.yesNoSummary(days, values)

    override val sleepHeading = "Сон"
    override val sleepNoData = "Данные о сне за период не вносились."
    override fun sleepAverage(h: String) = "Средняя продолжительность сна: $h ч."
    override fun sleepMinMax(min: String, minDay: LocalDate, max: String, maxDay: LocalDate) =
        "Минимум: $min ч (${dayLong(minDay)}). Максимум: $max ч (${dayLong(maxDay)})."
    override fun sleepLogged(logged: Int, total: Int) = "Данные внесены за $logged из ${days(total)}."

    override val medsHeading = "Приём лекарств"
    override val medsNone = "Лекарства не добавлены."
    override fun medDoses(parts: List<Pair<String, Int>>) =
        "; дозировки: " + parts.joinToString(", ") { (d, n) -> "$d ${RuText.DASH} ${days(n)}" }
    override fun medTaken(name: String, taken: Int, total: Int, pct: Int, doses: String) =
        "• $name: принято $taken из ${days(total)} ($pct%)$doses"

    override val chartsHeading = "Графики"
    override val noData = RuText.NO_DATA
    override fun sleepChartTitle(avg: String?) =
        "Сон, часы" + if (avg == null) "" else " (среднее $avg ч, пунктир)"
    override val symptomsChartTitle = "Симптомы (от низкого к высокому)"
    override val chartLevels = listOf("нет", "лёгкая", "умеренная", "тяжёлая")
    override val yesNoChartTitle = "Да / нет"
    override val medsChartTitle = "Лекарства"
    override val medTakenMark = "принято"
    override val medNotTakenMark = "не принято"
    override val doseLower = "меньшая доза"
    override val doseHigher = "большая доза"
    override val doseChange = "смена дозировки"

    override val notesHeading = "Записи самонаблюдения"
    override fun notesDay(d: LocalDate) = "${dayLong(d)}, ${weekday(d).lowercase()}"
    override fun notesLastDay(d: LocalDate) = "Записи самонаблюдения за ${dayLong(d)}"
}

object EnReportText : ReportText {
    private val MONTHS = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )
    private val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private const val DASH = RuText.DASH

    private fun month(d: LocalDate) = MONTHS[d.monthValue - 1]
    private fun monthShort(d: LocalDate) = month(d).take(3)

    /** "October 24" */
    override fun dayLong(d: LocalDate) = "${month(d)} ${d.dayOfMonth}"

    /**
     * "October 18–24, 2026", "October 30 – November 5, 2026",
     * "December 30, 2026 – January 5, 2027", "October 18, 2026".
     */
    fun period(from: LocalDate, to: LocalDate): String = when {
        from == to -> "${dayLong(from)}, ${from.year}"
        from.year != to.year -> "${dayLong(from)}, ${from.year} $DASH ${dayLong(to)}, ${to.year}"
        from.monthValue != to.monthValue -> "${dayLong(from)} $DASH ${dayLong(to)}, ${to.year}"
        else -> "${month(from)} ${from.dayOfMonth}$DASH${to.dayOfMonth}, ${to.year}"
    }

    override val title = "Mood and Symptom Diary"
    override fun periodLine(from: LocalDate, to: LocalDate) = "Period: ${period(from, to)}"
    override fun page(n: Int) = "p. $n"

    override val legendHeading = "Table value scale"
    override val legendValue = "Value"
    override val legendLevel = "Level"
    override val legendMarked = "Shown as"
    override val legendLevels = listOf("no symptom", "mild", "moderate", "severe")
    override val legendMarks = listOf("white cell", "light grey cell", "grey cell", "black cell")

    override val yes = "yes"
    override val no = "no"

    override val dayHeader = "Day"
    override fun weekday(d: LocalDate) = WEEKDAYS[d.dayOfWeek.value - 1]

    /** "Oct 24", "Oct 18–19", "Oct 30 – Nov 2" */
    override fun rangeShort(from: LocalDate, to: LocalDate): String = when {
        from == to -> "${monthShort(from)} ${from.dayOfMonth}"
        from.monthValue == to.monthValue && from.year == to.year ->
            "${monthShort(from)} ${from.dayOfMonth}$DASH${to.dayOfMonth}"
        else -> "${monthShort(from)} ${from.dayOfMonth} $DASH ${monthShort(to)} ${to.dayOfMonth}"
    }

    override fun days(n: Int) = "$n ${if (n == 1) "day" else "days"}"

    /** 7.5 -> "7.5", 8.0 -> "8" */
    override fun hours(v: Float): String =
        if (v % 1f == 0f) v.toInt().toString() else String.format(Locale.US, "%.1f", v)

    override val depression = "Depression"
    override val elevated = "Elevated mood"
    override val irritability = "Irritability"
    override val anxiety = "Anxiety"
    override val sleepRow = "Sleep, h"
    override val psychotic = "Psychotic symptoms"
    override val therapy = "Psychotherapy"
    override val dayTableHeading = "Day by day"

    override val summaryHeading = "Summary by period"
    override val summaryIndicator = "Indicator"
    override val summaryMarked = "What was logged"

    private val LEVELS = listOf("none", "mild", "moderate", "severe")

    private fun <T> summary(days: List<LocalDate>, values: List<T>, word: (T) -> String): String {
        require(days.size == values.size)
        val parts = ArrayList<String>()
        var start = 0
        for (i in 1..days.size) {
            if (days.isEmpty()) break
            val boundary = i == days.size || values[i] != values[start] ||
                days[i].toEpochDay() - days[i - 1].toEpochDay() != 1L
            if (boundary) {
                parts += "${rangeShort(days[start], days[i - 1])}: ${word(values[start])}."
                start = i
            }
        }
        return parts.joinToString(" ")
    }

    override fun levelSummary(days: List<LocalDate>, values: List<Int?>, gender: Gender) =
        summary(days, values) { v -> if (v == null) noData else LEVELS[v.coerceIn(0, 3)] }

    override fun yesNoSummary(days: List<LocalDate>, values: List<Boolean?>) =
        summary(days, values) { v -> when (v) { null -> noData; true -> yes; false -> no } }

    override val sleepHeading = "Sleep"
    override val sleepNoData = "No sleep data was logged for this period."
    override fun sleepAverage(h: String) = "Average sleep: $h h."
    override fun sleepMinMax(min: String, minDay: LocalDate, max: String, maxDay: LocalDate) =
        "Minimum: $min h (${dayLong(minDay)}). Maximum: $max h (${dayLong(maxDay)})."
    override fun sleepLogged(logged: Int, total: Int) = "Logged on $logged of ${days(total)}."

    override val medsHeading = "Medications"
    override val medsNone = "No medications added."
    override fun medDoses(parts: List<Pair<String, Int>>) =
        "; doses: " + parts.joinToString(", ") { (d, n) -> "$d $DASH ${days(n)}" }
    override fun medTaken(name: String, taken: Int, total: Int, pct: Int, doses: String) =
        "• $name: taken on $taken of ${days(total)} ($pct%)$doses"

    override val chartsHeading = "Charts"
    override val noData = "no data"
    override fun sleepChartTitle(avg: String?) =
        "Sleep, hours" + if (avg == null) "" else " (average $avg h, dashed)"
    override val symptomsChartTitle = "Symptoms (low to high)"
    override val chartLevels = LEVELS
    override val yesNoChartTitle = "Yes / no"
    override val medsChartTitle = "Medications"
    override val medTakenMark = "taken"
    override val medNotTakenMark = "not taken"
    override val doseLower = "lower dose"
    override val doseHigher = "higher dose"
    override val doseChange = "dose change"

    override val notesHeading = "Self-observation notes"
    override fun notesDay(d: LocalDate) = "${dayLong(d)}, ${weekday(d)}"
    override fun notesLastDay(d: LocalDate) = "Self-observation notes for ${dayLong(d)}"
}
