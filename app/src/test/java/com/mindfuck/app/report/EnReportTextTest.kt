package com.mindfuck.app.report

import com.mindfuck.app.report.RuText.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.LocalDate

/** Made-up data only. */
class EnReportTextTest {
    private val t = EnReportText
    private val days = (18..24).map { LocalDate.of(2026, 10, it) }

    @Test fun levelSummary() = assertEquals(
        "Oct 18–19: none. Oct 20–21: severe. Oct 22: no data. Oct 23: mild. Oct 24: moderate.",
        t.levelSummary(days, listOf(0, 0, 3, 3, null, 1, 2), Gender.FEMININE),
    )

    @Test fun yesNoSummary() = assertEquals(
        "Oct 18–20: no. Oct 21: yes. Oct 22–24: no data.",
        t.yesNoSummary(days, listOf(false, false, false, true, null, null, null)),
    )

    @Test fun crossMonthRange() = assertEquals("Oct 30 – Nov 2", t.rangeShort(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 2)))

    @Test fun periods() {
        assertEquals("Period: October 18–24, 2026", t.periodLine(days.first(), days.last()))
        assertEquals("Period: October 18, 2026", t.periodLine(days.first(), days.first()))
        assertEquals("Period: October 30 – November 5, 2026", t.periodLine(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 5)))
        assertEquals(
            "Period: December 30, 2026 – January 5, 2027",
            t.periodLine(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 5)),
        )
    }

    @Test fun weekdaysDaysHours() {
        assertEquals(listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"), days.map(t::weekday))
        assertEquals("1 day", t.days(1))
        assertEquals("21 days", t.days(21))
        assertEquals("7.5", t.hours(7.5f))
        assertEquals("8", t.hours(8f))
    }

    @Test fun medLine() = assertEquals(
        "• Testamine: taken on 5 of 7 days (71%); doses: 100 mg – 2 days, 150 mg – 3 days",
        t.medTaken("Testamine", 5, 7, 71, t.medDoses(listOf("100 mg" to 2, "150 mg" to 3))),
    )

    @Test fun russianIsStillTheDefault() {
        assertSame(RuReportText, ReportOptions().language.text)
        assertEquals("Период: 18–24 октября 2026 года", RuReportText.periodLine(days.first(), days.last()))
        assertEquals("стр. 2", RuReportText.page(2))
    }
}
