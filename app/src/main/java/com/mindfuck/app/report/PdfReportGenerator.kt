package com.mindfuck.app.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.mindfuck.app.charts.ChartPalette
import com.mindfuck.app.charts.ChartRenderer
import com.mindfuck.app.charts.LegendMark
import com.mindfuck.app.charts.MedRow
import com.mindfuck.app.data.CustomTracker
import com.mindfuck.app.data.Medication
import com.mindfuck.app.data.RangeData
import com.mindfuck.app.report.RuText.Gender
import com.mindfuck.app.ui.theme.AppFonts
import java.io.File
import java.time.LocalDate

class ReportInput(
    val from: LocalDate,
    val to: LocalDate,
    val range: RangeData,
    val meds: List<Medication>,
    val trackers: List<CustomTracker>,
)

/**
 * Builds "Дневник настроения и симптомов" (or its English version) as an A4 PDF.
 * The base layout replicates the sample report; [ReportOptions] picks the language and adds extra sections.
 */
class PdfReportGenerator(private val context: Context) {

    private companion object {
        const val PAGE_W = 595
        const val PAGE_H = 842
        const val MARGIN_X = 40f
        const val MARGIN_TOP = 34f
        const val MARGIN_BOTTOM = 44f
        const val CONTENT_W = PAGE_W - 2 * MARGIN_X
        const val LABEL_COL = 150f
        const val DAYS_PER_BLOCK = 7

        val HEADER_FILL = Color.rgb(0xCC, 0xCC, 0xCC)
        val LEVEL_FILL = intArrayOf(Color.WHITE, Color.rgb(0xE0, 0xE0, 0xE0), Color.rgb(0x9E, 0x9E, 0x9E), Color.BLACK)
        val LEVEL_TEXT = intArrayOf(Color.BLACK, Color.BLACK, Color.BLACK, Color.WHITE)
        val BORDER = Color.rgb(0x44, 0x44, 0x44)
        val MUTED = Color.rgb(0x88, 0x88, 0x88)

    }

    /** Wording of the report being generated; set from [ReportOptions.language]. */
    private var t: ReportText = RuReportText

    private val baseRows
        get() = listOf(
            t.depression to Gender.FEMININE,
            t.elevated to Gender.NEUTER,
            t.irritability to Gender.FEMININE,
            t.anxiety to Gender.FEMININE,
        )

    private val regular = AppFonts.typeface(context, AppFonts.REGULAR)
    private val bold = AppFonts.typeface(context, AppFonts.BOLD)

    private fun paint(size: Float, isBold: Boolean = false, color: Int = Color.BLACK) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = if (isBold) bold else regular
            textSize = size
            this.color = color
        }

    private val pTitle = paint(17f, true)
    private val pSubtitle = paint(11.5f)
    private val pH2 = paint(13f, true)
    private val pH3 = paint(10.5f, true)
    private val pBody = paint(9.5f)
    private val pCell = paint(9.2f)
    private val pCellBold = paint(9.2f, true)
    private val pCellSmall = paint(8.4f)
    private val pDayNum = paint(11.5f)
    private val pValue = paint(12f, true)
    private val pFooter = paint(8.5f)

    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 0.6f; color = BORDER
    }
    private val fill = Paint().apply { style = Paint.Style.FILL }

    fun generate(input: ReportInput, options: ReportOptions, out: File) {
        t = options.language.text
        val doc = PdfDocument()
        try {
            Writer(doc).apply {
                newPage()
                header(input)
                legend()
                dayTable(input, options)
                summary(input, options)
                if (options.sleep) sleepSummary(input)
                if (options.meds) medications(input)
                if (options.charts) charts(input, options)
                notes(input, options)
                finish()
            }
            out.outputStream().use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }

    // ===================================================================== layout engine

    private class Cell(
        val text: String,
        val paint: TextPaint,
        val fill: Int = Color.WHITE,
        val align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        val color: Int? = null,
    )

    private inner class Writer(val doc: PdfDocument) {
        private var page: PdfDocument.Page? = null
        lateinit var c: Canvas
        var y = 0f
        private var pageNo = 0
        val bottom get() = PAGE_H - MARGIN_BOTTOM

        fun newPage() {
            closePage()
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            c = page!!.canvas
            y = MARGIN_TOP
        }

        private fun closePage() {
            val p = page ?: return
            val text = t.page(pageNo)
            c.drawText(text, PAGE_W - MARGIN_X - pFooter.measureText(text), PAGE_H - 30f, pFooter)
            doc.finishPage(p)
            page = null
        }

        fun finish() = closePage()

        /** Starts a new page if [h] does not fit. Returns true if it did. */
        fun ensure(h: Float): Boolean {
            if (y + h > bottom && y > MARGIN_TOP + 1f) {
                newPage(); return true
            }
            return false
        }

        fun layout(text: CharSequence, paint: TextPaint, width: Float, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(1))
                .setAlignment(align)
                .setIncludePad(false)
                .setLineSpacing(1.5f, 1.08f)
                .build()

        fun draw(l: StaticLayout, x: Float, top: Float) {
            c.save(); c.translate(x, top); l.draw(c); c.restore()
        }

        fun paragraph(text: String, paint: TextPaint = pBody, x: Float = MARGIN_X + 10f, width: Float = CONTENT_W - 10f, gapAfter: Float = 4f) {
            val l = layout(text, paint, width)
            // split by lines so long paragraphs can flow over pages
            var line = 0
            while (line < l.lineCount) {
                val lineTop = l.getLineTop(line).toFloat()
                val lineH = (l.getLineBottom(line) - l.getLineTop(line)).toFloat()
                ensure(lineH)
                c.save()
                c.translate(x, y - lineTop)
                c.clipRect(0f, lineTop, width, lineTop + lineH)
                l.draw(c)
                c.restore()
                y += lineH
                line++
            }
            y += gapAfter
        }

        fun heading(text: String, gapBefore: Float = 18f) {
            y += gapBefore
            ensure(60f)
            val l = layout(text, pH2, CONTENT_W - 10f)
            draw(l, MARGIN_X + 10f, y)
            y += l.height + 12f
        }

        fun subheading(text: String, gapBefore: Float = 10f) {
            y += gapBefore
            ensure(40f)
            val l = layout(text, pH3, CONTENT_W)
            draw(l, MARGIN_X + 10f, y)
            y += l.height + 6f
        }

        private fun rowHeight(row: List<Cell>, widths: FloatArray, padV: Float, minH: Float): Float {
            var h = minH
            row.forEachIndexed { i, cell ->
                val l = layout(cell.text, cell.paint, widths[i] - 2 * 6f, cell.align)
                h = maxOf(h, l.height + 2 * padV)
            }
            return h
        }

        fun tableHeight(rows: List<List<Cell>>, widths: FloatArray, minHeights: (Int) -> Float, padV: Float = 5f) =
            rows.indices.sumOf { rowHeight(rows[it], widths, padV, minHeights(it)).toDouble() }.toFloat()

        /**
         * Bordered table. Header rows are repeated when the table breaks over pages.
         * [minHeights] gives the minimum height of every row by index.
         */
        fun table(
            x: Float,
            widths: FloatArray,
            rows: List<List<Cell>>,
            headerRows: Int,
            minHeights: (Int) -> Float,
            padV: Float = 5f,
        ) {
            val heights = rows.indices.map { rowHeight(rows[it], widths, padV, minHeights(it)) }
            rows.forEachIndexed { r, row ->
                if (y + heights[r] > bottom) {
                    newPage()
                    if (r >= headerRows) for (h in 0 until headerRows) drawRow(x, widths, rows[h], heights[h])
                }
                drawRow(x, widths, row, heights[r])
            }
        }

        private fun drawRow(x0: Float, widths: FloatArray, row: List<Cell>, h: Float) {
            var x = x0
            row.forEachIndexed { i, cell ->
                val w = widths[i]
                fill.color = cell.fill
                c.drawRect(x, y, x + w, y + h, fill)
                val paint = if (cell.color != null) TextPaint(cell.paint).apply { color = cell.color } else cell.paint
                val l = layout(cell.text, paint, w - 12f, cell.align)
                draw(l, x + 6f, y + (h - l.height) / 2f)
                c.drawRect(x, y, x + w, y + h, border)
                x += w
            }
            y += h
        }
    }

    // ===================================================================== sections

    private fun Writer.header(input: ReportInput) {
        val titleLayout = layout(t.title, pTitle, CONTENT_W, Layout.Alignment.ALIGN_CENTER)
        draw(titleLayout, MARGIN_X, y + 6f)
        y += titleLayout.height + 12f
        val s = layout(t.periodLine(input.from, input.to), pSubtitle, CONTENT_W, Layout.Alignment.ALIGN_CENTER)
        draw(s, MARGIN_X, y)
        y += s.height + 4f
    }

    private fun Writer.legend() {
        heading(t.legendHeading)
        val widths = floatArrayOf(80f, 150f, 170f)
        val x = MARGIN_X + (CONTENT_W - widths.sum()) / 2f
        val center = Layout.Alignment.ALIGN_CENTER
        val marks = t.legendMarks
        val levels = t.legendLevels
        val rows = buildList {
            add(listOf(
                Cell(t.legendValue, pCellBold, HEADER_FILL, center),
                Cell(t.legendLevel, pCellBold, HEADER_FILL, center),
                Cell(t.legendMarked, pCellBold, HEADER_FILL, center),
            ))
            for (v in 0..3) add(listOf(
                Cell(v.toString(), pCellBold, LEVEL_FILL[v], center, LEVEL_TEXT[v]),
                Cell(levels[v], pCell),
                Cell(marks[v], pCell),
            ))
        }
        table(x, widths, rows, headerRows = 1, minHeights = { 17f }, padV = 3f)
    }

    private fun levelCell(v: Int?): Cell = if (v == null) {
        Cell(RuText.DASH, pValue, Color.WHITE, Layout.Alignment.ALIGN_CENTER, MUTED)
    } else {
        val lvl = v.coerceIn(0, 3)
        Cell(lvl.toString(), pValue, LEVEL_FILL[lvl], Layout.Alignment.ALIGN_CENTER, LEVEL_TEXT[lvl])
    }

    private fun yesNoCell(v: Boolean?): Cell = when (v) {
        null -> Cell(RuText.DASH, pCell, Color.WHITE, Layout.Alignment.ALIGN_CENTER, MUTED)
        true -> Cell(t.yes, pCellBold, LEVEL_FILL[1], Layout.Alignment.ALIGN_CENTER)
        false -> Cell(t.no, pCell, Color.WHITE, Layout.Alignment.ALIGN_CENTER)
    }

    private fun textCell(text: String?, paint: TextPaint = pCell): Cell =
        if (text == null) Cell(RuText.DASH, pCell, Color.WHITE, Layout.Alignment.ALIGN_CENTER, MUTED)
        else Cell(text, paint, Color.WHITE, Layout.Alignment.ALIGN_CENTER)

    private fun reportTrackers(input: ReportInput): List<CustomTracker> = input.trackers.filter { tr ->
        tr.active || input.range.custom.values.any { it.containsKey(tr.id) }
    }

    private fun reportMeds(input: ReportInput): List<Medication> = input.meds.filter { m ->
        m.active || input.range.hasMed(m.id)
    }

    private fun dates(input: ReportInput): List<LocalDate> =
        (input.from.toEpochDay()..input.to.toEpochDay()).map(LocalDate::ofEpochDay)

    /** Rows of a day table: label + one cell per day. */
    private class DayRow(val label: String, val tall: Boolean, val cell: (LocalDate) -> Cell)

    private fun Writer.dayBlocks(title: String?, days: List<LocalDate>, rows: List<DayRow>) {
        if (title != null) heading(title)
        val colW = (CONTENT_W - LABEL_COL) / DAYS_PER_BLOCK
        days.chunked(DAYS_PER_BLOCK).forEachIndexed { b, block ->
            val widths = FloatArray(block.size + 1) { if (it == 0) LABEL_COL else colW }
            val center = Layout.Alignment.ALIGN_CENTER
            val cells = buildList {
                add(listOf(Cell(t.dayHeader, pCell, HEADER_FILL)) + block.map { Cell(it.dayOfMonth.toString(), pDayNum, HEADER_FILL, center) })
                // second header row: weekdays; month name on the left for multi-week tables
                val month = if (days.size > DAYS_PER_BLOCK) t.rangeShort(block.first(), block.last()) else ""
                add(listOf(Cell(month, pCellSmall, HEADER_FILL)) +
                    block.map { Cell(t.weekday(it), pCellSmall, HEADER_FILL, center) })
                rows.forEach { row -> add(listOf(Cell(row.label, pCell)) + block.map(row.cell)) }
            }
            val minH: (Int) -> Float = { i ->
                when {
                    i == 0 -> 18f
                    i == 1 -> 14f
                    rows[i - 2].tall -> 26f
                    else -> 20f
                }
            }
            if (b > 0) y += 12f
            // keep a week block together when it fits on one page
            val h = tableHeight(cells, widths, minH, 3f)
            if (h < bottom - MARGIN_TOP) ensure(h)
            table(MARGIN_X, widths, cells, headerRows = 2, minHeights = minH, padV = 3f)
        }
    }

    private fun Writer.dayTable(input: ReportInput, o: ReportOptions) {
        val r = input.range
        val rows = ArrayList<DayRow>()
        val picks = listOf<(LocalDate) -> Int?>(
            { r.entry(it.toEpochDay())?.depression },
            { r.entry(it.toEpochDay())?.elevated },
            { r.entry(it.toEpochDay())?.irritability },
            { r.entry(it.toEpochDay())?.anxiety },
        )
        baseRows.forEachIndexed { i, (label, _) -> rows += DayRow(label, true) { levelCell(picks[i](it)) } }
        if (o.sleep) rows += DayRow(t.sleepRow, false) { d ->
            textCell(r.entry(d.toEpochDay())?.sleepHours?.let(t::hours))
        }
        if (o.yesNo) {
            rows += DayRow(t.psychotic, false) { yesNoCell(r.entry(it.toEpochDay())?.psychotic) }
            rows += DayRow(t.therapy, false) { yesNoCell(r.entry(it.toEpochDay())?.therapy) }
        }
        if (o.custom) reportTrackers(input).forEach { tr ->
            rows += if (tr.isScale) DayRow(tr.name, true) { levelCell(r.custom(it.toEpochDay(), tr.id)) }
            else DayRow(tr.name, false) { yesNoCell(r.custom(it.toEpochDay(), tr.id)?.let { v -> v == 1 }) }
        }
        dayBlocks(t.dayTableHeading, dates(input), rows)
    }

    private fun Writer.summary(input: ReportInput, o: ReportOptions) {
        heading(t.summaryHeading)
        val r = input.range
        val days = dates(input)
        fun levels(pick: (Long) -> Int?) = days.map { pick(it.toEpochDay()) }

        val items = ArrayList<Pair<String, String>>()
        items += t.depression to t.levelSummary(days, levels { r.entry(it)?.depression }, Gender.FEMININE)
        items += t.elevated to t.levelSummary(days, levels { r.entry(it)?.elevated }, Gender.NEUTER)
        items += t.irritability to t.levelSummary(days, levels { r.entry(it)?.irritability }, Gender.FEMININE)
        items += t.anxiety to t.levelSummary(days, levels { r.entry(it)?.anxiety }, Gender.FEMININE)
        if (o.yesNo) {
            items += t.psychotic to t.yesNoSummary(days, days.map { r.entry(it.toEpochDay())?.psychotic })
            items += t.therapy to t.yesNoSummary(days, days.map { r.entry(it.toEpochDay())?.therapy })
        }
        if (o.custom) reportTrackers(input).forEach { tr ->
            val values = days.map { r.custom(it.toEpochDay(), tr.id) }
            items += tr.name to if (tr.isScale) t.levelSummary(days, values, Gender.NEUTER)
            else t.yesNoSummary(days, values.map { v -> v?.let { it == 1 } })
        }

        val widths = floatArrayOf(LABEL_COL, CONTENT_W - LABEL_COL)
        val center = Layout.Alignment.ALIGN_CENTER
        val rows = buildList {
            add(listOf(Cell(t.summaryIndicator, pCellBold, HEADER_FILL, center), Cell(t.summaryMarked, pCellBold, HEADER_FILL, center)))
            items.forEach { (k, v) -> add(listOf(Cell(k, pCell), Cell(v, pCell))) }
        }
        table(MARGIN_X, widths, rows, headerRows = 1, minHeights = { if (it == 0) 18f else 28f }, padV = 4f)
    }

    private fun Writer.sleepSummary(input: ReportInput) {
        heading(t.sleepHeading)
        val total = dates(input).size
        val logged = dates(input).mapNotNull { d -> input.range.entry(d.toEpochDay())?.sleepHours?.let { d to it } }
        if (logged.isEmpty()) {
            paragraph(t.sleepNoData)
            return
        }
        val avg = logged.map { it.second }.average().toFloat()
        val min = logged.minBy { it.second }
        val max = logged.maxBy { it.second }
        paragraph(t.sleepAverage(t.hours(Math.round(avg * 10f) / 10f)))
        paragraph(t.sleepMinMax(t.hours(min.second), min.first, t.hours(max.second), max.first))
        paragraph(t.sleepLogged(logged.size, total))
    }

    private fun Writer.medications(input: ReportInput) {
        val meds = reportMeds(input)
        heading(t.medsHeading)
        if (meds.isEmpty()) {
            paragraph(t.medsNone)
            return
        }
        val r = input.range
        val days = dates(input)
        val center = Layout.Alignment.ALIGN_CENTER
        val rows = meds.map { m ->
            val doses = days.map { r.doseOn(it.toEpochDay(), m) }
            val distinct = doses.filterNotNull().filter { it.isNotBlank() }.distinct()
            // One dose all period: put it in the row label, cells just show ✓.
            // Several doses: every cell shows the dose actually taken that day.
            val varying = distinct.size > 1
            val label = if (!varying && distinct.size == 1) "${m.name} ${distinct[0]}" else m.name
            DayRow(label, false) { d ->
                val dose = doses[(d.toEpochDay() - input.from.toEpochDay()).toInt()]
                when {
                    dose == null -> textCell(null)
                    varying && dose.isNotBlank() -> Cell(dose, pCellSmall, LEVEL_FILL[1], center)
                    else -> Cell("✓", pCellBold, LEVEL_FILL[1], center)
                }
            }
        }
        dayBlocks(null, days, rows)
        y += 8f
        meds.forEach { m ->
            val doses = days.mapNotNull { r.doseOn(it.toEpochDay(), m) }
            val pct = Math.round(doses.size * 100f / days.size)
            val byDose = doses.filter { it.isNotBlank() }.groupingBy { it }.eachCount()
            val doseText = if (byDose.size > 1) t.medDoses(byDose.map { (d, n) -> d to n }) else ""
            val name = if (byDose.size == 1) "${m.name} ${byDose.keys.first()}" else m.name
            paragraph(t.medTaken(name, doses.size, days.size, pct, doseText), gapAfter = 2f)
        }
    }

    private fun Writer.charts(input: ReportInput, o: ReportOptions) {
        heading(t.chartsHeading)
        val u = 0.72f
        val palette = ChartPalette(
            text = Color.rgb(0x55, 0x55, 0x55),
            grid = Color.rgb(0xDD, 0xDD, 0xDD),
            missing = Color.rgb(0x9D, 0xB7, 0xCC),
            sleepBar = Color.rgb(0x8F, 0xAE, 0xC6),
            avgLine = Color.rgb(0x77, 0x77, 0x77),
            levels = intArrayOf(Color.rgb(0xB2, 0xEB, 0xF2), Color.rgb(0x4D, 0xD8, 0xE6), Color.rgb(0x3B, 0x8D, 0xAE), Color.rgb(0x2E, 0x3B, 0x66)),
            yes = Color.rgb(0x3B, 0x8D, 0xAE),
            no = Color.rgb(0x88, 0x88, 0x88),
        )
        val chart = ChartRenderer(u, regular, palette)
        val r = input.range
        val days = dates(input)
        val left = MARGIN_X + 4f
        val width = CONTENT_W - 8f

        val levelLabels = baseRows.map { it.first }.toMutableList()
        val levelRows = mutableListOf(
            days.map { r.entry(it.toEpochDay())?.depression },
            days.map { r.entry(it.toEpochDay())?.elevated },
            days.map { r.entry(it.toEpochDay())?.irritability },
            days.map { r.entry(it.toEpochDay())?.anxiety },
        )
        val ynLabels = mutableListOf<String>()
        val ynRows = mutableListOf<List<Boolean?>>()
        if (o.yesNo) {
            ynLabels += t.psychotic; ynRows += days.map { r.entry(it.toEpochDay())?.psychotic }
            ynLabels += t.therapy; ynRows += days.map { r.entry(it.toEpochDay())?.therapy }
        }
        if (o.custom) reportTrackers(input).forEach { tr ->
            val values = days.map { r.custom(it.toEpochDay(), tr.id) }
            if (tr.isScale) { levelLabels += tr.name; levelRows += values }
            else { ynLabels += tr.name; ynRows += values.map { v -> v?.let { it == 1 } } }
        }
        val medRows = if (o.meds) reportMeds(input).map { m ->
            MedRow(m.name, days.map { r.doseOn(it.toEpochDay(), m) })
        } else emptyList()
        val logged = days.map { r.isLogged(it.toEpochDay()) }

        val labelW = chart.labelColumnWidth(levelLabels + ynLabels + medRows.map { it.label }, LABEL_COL)
        val missing = LegendMark.MISSING to t.noData

        fun block(title: String, h: Float, legend: List<Pair<LegendMark, String>>, drawIt: (Float) -> Unit) {
            val legendH = chart.legendHeight(legend, width)
            ensure(h + legendH + 26f)
            val l = layout(title, pH3, width)
            draw(l, MARGIN_X + 10f, y)
            y += l.height + 6f
            drawIt(y)
            if (legend.isNotEmpty()) chart.drawLegend(c, left, y + h, width, legend)
            y += h + legendH + 12f
        }

        if (o.sleep) {
            val sleep = days.map { r.entry(it.toEpochDay())?.sleepHours }
            val known = sleep.filterNotNull()
            val avg = if (known.isEmpty()) null else t.hours(Math.round(known.average().toFloat() * 10f) / 10f)
            block(t.sleepChartTitle(avg), chart.sleepHeight(), emptyList()) { top ->
                chart.drawSleep(c, left, top, width, chart.sleepAxisWidth(), days, sleep)
            }
        }
        block(
            t.symptomsChartTitle, chart.levelsHeight(levelRows.size),
            listOf(
                LegendMark.NONE to t.chartLevels[0], LegendMark.MILD to t.chartLevels[1],
                LegendMark.MODERATE to t.chartLevels[2], LegendMark.SEVERE to t.chartLevels[3], missing,
            ),
        ) { top ->
            chart.drawLevels(c, left, top, width, labelW, days, levelLabels, levelRows)
        }
        if (ynRows.isNotEmpty()) block(
            t.yesNoChartTitle, chart.yesNoHeight(ynRows.size),
            listOf(LegendMark.YES to t.yes, LegendMark.NO to t.no, missing),
        ) { top ->
            chart.drawYesNo(c, left, top, width, labelW, days, ynLabels, ynRows)
        }
        if (medRows.isNotEmpty()) block(
            t.medsChartTitle, chart.medsHeight(medRows.size),
            listOf(
                LegendMark.YES to t.medTakenMark, LegendMark.NO to t.medNotTakenMark, missing,
                LegendMark.DOSE_LOW to t.doseLower, LegendMark.DOSE_HIGH to t.doseHigher,
                LegendMark.DOSE to t.doseChange,
            ),
        ) { top ->
            chart.drawMeds(c, left, top, width, labelW, days, medRows, logged)
        }
    }

    private fun Writer.notes(input: ReportInput, o: ReportOptions) {
        val withNotes = dates(input).mapNotNull { d ->
            val lines = RuText.bullets(input.range.entry(d.toEpochDay())?.notes.orEmpty())
            if (lines.isEmpty()) null else d to lines
        }
        if (withNotes.isEmpty()) return

        if (o.allNotes && withNotes.size > 1) {
            heading(t.notesHeading)
            withNotes.forEachIndexed { i, (d, lines) ->
                subheading(t.notesDay(d), gapBefore = if (i == 0) 0f else 10f)
                lines.forEach { paragraph("• $it", gapAfter = 3f) }
            }
        } else {
            val (d, lines) = withNotes.last()
            heading(t.notesLastDay(d))
            lines.forEach { paragraph("• $it", gapAfter = 3f) }
        }
    }
}
