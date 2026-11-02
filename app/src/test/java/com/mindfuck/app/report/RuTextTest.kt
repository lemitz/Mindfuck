package com.mindfuck.app.report

import com.mindfuck.app.report.RuText.Gender
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RuTextTest {
    private val days = (18..24).map { LocalDate.of(2026, 10, it) }

    // Made-up week. The wording format follows the paper "Дневник настроения и симптомов" report
    // the app reproduces; never put real diary data in tests.
    @Test fun depression() = assertEquals(
        "18–19 окт: не отмечалась. 20–21 окт: тяжёлая. 22 окт: не отмечалась. 23 окт: лёгкая. 24 окт: не отмечалась.",
        RuText.levelSummary(days, listOf(0, 0, 3, 3, 0, 1, 0), Gender.FEMININE),
    )

    @Test fun elevated() = assertEquals(
        "18 окт: не отмечалось. 19 окт: лёгкая. 20 окт: тяжёлая. 21 окт: не отмечалось. 22–23 окт: лёгкая. 24 окт: не отмечалось.",
        RuText.levelSummary(days, listOf(0, 1, 3, 0, 1, 1, 0), Gender.NEUTER),
    )

    @Test fun irritability() = assertEquals(
        "18–19 окт: лёгкая. 20 окт: не отмечалась. 21–23 окт: лёгкая. 24 окт: не отмечалась.",
        RuText.levelSummary(days, listOf(1, 1, 0, 1, 1, 1, 0), Gender.FEMININE),
    )

    @Test fun anxiety() = assertEquals(
        "18 окт: тяжёлая. 19–20 окт: умеренная. 21 окт: тяжёлая. 22 окт: лёгкая. 23 окт: не отмечалась. 24 окт: лёгкая.",
        RuText.levelSummary(days, listOf(3, 2, 2, 3, 1, 0, 1), Gender.FEMININE),
    )

    @Test fun missingDays() = assertEquals(
        "18–20 окт: нет данных. 21–24 окт: лёгкая.",
        RuText.levelSummary(days, listOf(null, null, null, 1, 1, 1, 1), Gender.FEMININE),
    )

    @Test fun crossMonthRange() = assertEquals(
        "30 окт – 2 ноя: тяжёлая.",
        RuText.levelSummary(
            (0..3).map { LocalDate.of(2026, 10, 30).plusDays(it.toLong()) }, listOf(3, 3, 3, 3), Gender.FEMININE,
        ),
    )

    @Test fun periods() {
        assertEquals("18–24 октября 2026 года", RuText.period(days.first(), days.last()))
        assertEquals("18 октября 2026 года", RuText.period(days.first(), days.first()))
        assertEquals(
            "30 октября – 5 ноября 2026 года",
            RuText.period(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 5)),
        )
        assertEquals(
            "30 декабря 2026 – 5 января 2027 года",
            RuText.period(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 5)),
        )
    }

    @Test fun weekdays() {
        assertEquals(listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"), days.map(RuText::weekday))
    }

    @Test fun pluralAndHours() {
        assertEquals("1 день", RuText.days(1))
        assertEquals("3 дня", RuText.days(3))
        assertEquals("11 дней", RuText.days(11))
        assertEquals("21 день", RuText.days(21))
        assertEquals("7,5", RuText.hours(7.5f))
        assertEquals("8", RuText.hours(8f))
    }

    @Test fun bullets() = assertEquals(
        listOf("выспался, с утра много сил", "дошёл пешком до работы", "вечером созвонился с другом"),
        RuText.bullets("• выспался, с утра много сил\n\n- дошёл пешком до работы\n  вечером созвонился с другом  "),
    )

    @Test fun fileNames() {
        val d = LocalDate.of(2026, 11, 2)
        assertEquals("mindfuck_021126.pdf", ReportFileNamer.fileName(d, 1))
        assertEquals("mindfuck_021126_v2.pdf", ReportFileNamer.fileName(d, 2))
        assertEquals("mindfuck_021126_v13.pdf", ReportFileNamer.fileName(d, 13))
    }
}
