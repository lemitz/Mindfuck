package com.mindfuck.app.charts

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class ChartPalette(
    val text: Int,
    val grid: Int,
    val missing: Int,
    val sleepBar: Int,
    val avgLine: Int,
    /** none, mild, moderate, severe */
    val levels: IntArray,
    val yes: Int,
    /** ring color for an explicit "no" — deliberately muted so it never reads as "yes" */
    val no: Int,
)

/** Glyphs used in chart legends. */
enum class LegendMark { NONE, MILD, MODERATE, SEVERE, YES, NO, MISSING, DOSE, DOSE_LOW, DOSE_HIGH }

/** One medication row: per day the dose taken ("" = taken, no dose set) or null = not taken. */
class MedRow(val label: String, val doses: List<String?>)

/**
 * eMoods-style charts drawn on a plain android.graphics.Canvas, so the exact same code
 * paints the in-app graphs (via Compose nativeCanvas) and the PDF report pages.
 *
 * [u] is the size of one design unit: density on screen, ~0.72 pt in the PDF.
 * Paints are preallocated: nothing heavy is allocated per frame.
 */
class ChartRenderer(
    private val u: Float,
    typeface: Typeface,
    private val p: ChartPalette,
) {
    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface; textSize = 11.5f * u; color = p.text
    }
    private val axisPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface; textSize = 10.5f * u; color = p.text; textAlign = Paint.Align.CENTER
    }
    private val yAxisPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface; textSize = 10f * u; color = p.text; textAlign = Paint.Align.RIGHT
    }
    private val missPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface; color = p.missing; textAlign = Paint.Align.CENTER
    }
    private val dosePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface; textSize = 9f * u; color = p.text; textAlign = Paint.Align.CENTER
    }
    private val legendPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface; textSize = 10.5f * u; color = p.text
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 0.8f * u; color = p.grid
    }
    private val avgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1f * u; color = p.avgLine
        pathEffect = DashPathEffect(floatArrayOf(4f * u, 3f * u), 0f)
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rect = RectF()

    val axisHeight get() = 22f * u
    val levelRowHeight get() = 34f * u
    val yesNoRowHeight get() = 26f * u
    val medRowHeight get() = 48f * u
    private val sleepPlotHeight get() = 132f * u

    fun sleepHeight() = sleepPlotHeight + 10f * u + axisHeight
    fun levelsHeight(rows: Int) = rows * levelRowHeight + axisHeight
    fun yesNoHeight(rows: Int) = rows * yesNoRowHeight + axisHeight
    fun medsHeight(rows: Int) = rows * medRowHeight + axisHeight

    /** Width of the left label column that fits [labels], capped to [maxWidth]. */
    fun labelColumnWidth(labels: List<String>, maxWidth: Float): Float {
        val w = labels.maxOfOrNull { labelPaint.measureText(it) } ?: 0f
        return (w + 10f * u).coerceIn(30f * u, maxWidth)
    }

    /** The sleep chart only needs room for its own hour ticks. */
    fun sleepAxisWidth(): Float = yAxisPaint.measureText("10") + 9f * u

    // ------------------------------------------------------------------ sleep

    fun drawSleep(c: Canvas, left: Float, top: Float, width: Float, labelW: Float, days: List<LocalDate>, values: List<Float?>) {
        val n = days.size
        if (n == 0) return
        val x0 = left + labelW
        val plotW = width - labelW
        val colW = plotW / n
        val plotTop = top + 10f * u
        val plotBottom = plotTop + sleepPlotHeight
        val plotH = plotBottom - plotTop

        val known = values.filterNotNull()
        val maxY = max(8f, ceil((known.maxOrNull() ?: 0f) / 2f) * 2f)

        var y = 2f
        while (y <= maxY) {
            val yy = plotBottom - y / maxY * plotH
            c.drawLine(x0, yy, x0 + plotW, yy, gridPaint)
            c.drawText(fmtHours(y), x0 - 6f * u, yy + yAxisPaint.textSize * 0.35f, yAxisPaint)
            y += 2f
        }
        drawVerticalGuides(c, x0, colW, n, plotTop, plotBottom)
        c.drawLine(x0, plotBottom, x0 + plotW, plotBottom, gridPaint)

        val barW = min(colW * 0.72f, 14f * u)
        fill.color = p.sleepBar
        for (i in 0 until n) {
            val cx = x0 + colW * (i + 0.5f)
            val v = values[i]
            if (v == null) {
                drawMissing(c, cx, plotBottom - 5f * u, colW)
            } else if (v > 0f) {
                val h = max(v / maxY * plotH, barW)
                rect.set(cx - barW / 2, plotBottom - h, cx + barW / 2, plotBottom)
                c.drawRoundRect(rect, barW / 2, barW / 2, fill)
            }
        }

        if (known.isNotEmpty()) {
            val ay = plotBottom - known.average().toFloat() / maxY * plotH
            c.drawLine(x0, ay, x0 + plotW, ay, avgPaint)
        }
        drawDayAxis(c, x0, colW, days, plotBottom)
    }

    // ------------------------------------------------------------------ levels

    fun drawLevels(c: Canvas, left: Float, top: Float, width: Float, labelW: Float, days: List<LocalDate>, labels: List<String>, rows: List<List<Int?>>) {
        val n = days.size
        if (n == 0 || rows.isEmpty()) return
        val x0 = left + labelW
        val plotW = width - labelW
        val colW = plotW / n
        val rh = levelRowHeight
        val bottom = top + rows.size * rh
        drawVerticalGuides(c, x0, colW, n, top, bottom)

        rows.forEachIndexed { r, row ->
            val cy = top + r * rh + rh / 2
            drawRowLabel(c, labels[r], left, labelW, cy)
            c.drawLine(x0, cy, x0 + plotW, cy, gridPaint)
            for (i in 0 until n) {
                val cx = x0 + colW * (i + 0.5f)
                val v = row.getOrNull(i)
                if (v == null) drawMissing(c, cx, cy, colW) else drawLevelMark(c, cx, cy, colW, rh, v)
            }
        }
        drawDayAxis(c, x0, colW, days, bottom)
    }

    private fun drawLevelMark(c: Canvas, cx: Float, cy: Float, colW: Float, rh: Float, v: Int) {
        if (v <= 0) {
            fill.color = p.levels[0]
            c.drawCircle(cx, cy, min(colW * 0.34f, 5.5f * u), fill)
            return
        }
        val lvl = v.coerceIn(1, 3)
        val pillW = min(colW * 0.78f, 13f * u)
        val h = max(rh * LEVEL_HEIGHT[lvl], pillW)
        fill.color = p.levels[lvl]
        rect.set(cx - pillW / 2, cy - h / 2, cx + pillW / 2, cy + h / 2)
        c.drawRoundRect(rect, pillW / 2, pillW / 2, fill)
    }

    // ------------------------------------------------------------------ yes / no

    fun drawYesNo(c: Canvas, left: Float, top: Float, width: Float, labelW: Float, days: List<LocalDate>, labels: List<String>, rows: List<List<Boolean?>>) {
        val n = days.size
        if (n == 0 || rows.isEmpty()) return
        val x0 = left + labelW
        val plotW = width - labelW
        val colW = plotW / n
        val rh = yesNoRowHeight
        val bottom = top + rows.size * rh
        drawVerticalGuides(c, x0, colW, n, top, bottom)

        rows.forEachIndexed { r, row ->
            val cy = top + r * rh + rh / 2
            drawRowLabel(c, labels[r], left, labelW, cy)
            for (i in 0 until n) {
                val cx = x0 + colW * (i + 0.5f)
                when (row.getOrNull(i)) {
                    null -> drawMissing(c, cx, cy, colW)
                    true -> drawYes(c, cx, cy, colW, p.yes)
                    false -> drawNo(c, cx, cy, colW)
                }
            }
        }
        drawDayAxis(c, x0, colW, days, bottom)
    }

    private fun yesRadius(colW: Float) = min(colW * 0.38f, 6f * u)

    private fun drawYes(c: Canvas, cx: Float, cy: Float, colW: Float, color: Int) {
        fill.color = color
        c.drawCircle(cx, cy, yesRadius(colW), fill)
    }

    /** Smaller, thin, muted ring: clearly "logged as no", clearly not "yes". */
    private fun drawNo(c: Canvas, cx: Float, cy: Float, colW: Float) {
        val r = yesRadius(colW) * 0.72f
        ring.color = p.no
        ring.strokeWidth = min(1.2f * u, r * 0.45f)
        c.drawCircle(cx, cy, r, ring)
    }

    // ------------------------------------------------------------------ medications

    /**
     * Taken = filled dot, shaded by dose when the dose varies (lighter = lower dose).
     * The dose is printed above the first intake and above every day the dose changes.
     * [logged] tells "not taken" (muted ring) apart from "no data that day" (x).
     */
    fun drawMeds(c: Canvas, left: Float, top: Float, width: Float, labelW: Float, days: List<LocalDate>, rows: List<MedRow>, logged: List<Boolean>) {
        val n = days.size
        if (n == 0 || rows.isEmpty()) return
        val x0 = left + labelW
        val plotW = width - labelW
        val colW = plotW / n
        val rh = medRowHeight
        val bottom = top + rows.size * rh
        drawVerticalGuides(c, x0, colW, n, top, bottom)

        rows.forEachIndexed { r, row ->
            val cy = top + r * rh + rh * 0.72f
            drawRowLabel(c, row.label, left, labelW, cy)
            val distinct = row.doses.filterNotNull().filter { it.isNotBlank() }.distinct()
                .sortedWith(compareBy({ doseNumber(it) ?: Double.MAX_VALUE }, { it }))
            var prevDose: String? = null
            // Two label lanes: a change label that would collide with the previous one goes one lane up,
            // so short-lived dose changes (e.g. 2 days of 100 mg, then back to 50 mg) stay readable.
            val laneRight = floatArrayOf(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
            val laneY = floatArrayOf(cy - yesRadius(colW) - 5f * u, cy - yesRadius(colW) - 5f * u - dosePaint.textSize - 2f * u)
            for (i in 0 until n) {
                val cx = x0 + colW * (i + 0.5f)
                val dose = row.doses.getOrNull(i)
                when {
                    dose != null -> {
                        drawYes(c, cx, cy, colW, doseColor(distinct, dose))
                        if (dose.isNotBlank() && dose != prevDose) {
                            val half = dosePaint.measureText(dose) / 2
                            val lx = cx.coerceIn(x0 + half, x0 + plotW - half)
                            val lane = (0..1).firstOrNull { lx - half > laneRight[it] + 3f * u }
                            if (lane != null) {
                                c.drawText(dose, lx, laneY[lane], dosePaint)
                                laneRight[lane] = lx + half
                            }
                        }
                        prevDose = dose
                    }
                    logged.getOrElse(i) { false } -> drawNo(c, cx, cy, colW)
                    else -> drawMissing(c, cx, cy, colW)
                }
            }
        }
        drawDayAxis(c, x0, colW, days, bottom)
    }

    private fun doseColor(distinct: List<String>, dose: String): Int {
        if (distinct.size < 2 || dose.isBlank()) return p.yes
        val t = distinct.indexOf(dose).coerceAtLeast(0) / (distinct.size - 1f)
        return lerpColor(p.levels[1], p.levels[3], t)
    }

    // ------------------------------------------------------------------ legend

    private val legendGlyphW get() = 14f * u
    private val legendGap get() = 5f * u
    private val legendSpacing get() = 16f * u
    private val legendLineH get() = 24f * u

    private fun legendItemW(text: String) = legendGlyphW + legendGap + legendPaint.measureText(text)

    /** Greedy line breaking of legend items into lines that fit [width]. */
    private fun legendLines(items: List<Pair<LegendMark, String>>, width: Float): List<List<Pair<LegendMark, String>>> {
        val lines = mutableListOf<MutableList<Pair<LegendMark, String>>>()
        var w = 0f
        for (item in items) {
            val iw = legendItemW(item.second)
            if (lines.isEmpty() || w + legendSpacing + iw > width) {
                lines += mutableListOf(item); w = iw
            } else {
                lines.last() += item; w += legendSpacing + iw
            }
        }
        return lines
    }

    fun legendHeight(items: List<Pair<LegendMark, String>>, width: Float): Float =
        if (items.isEmpty()) 0f else legendLines(items, width).size * legendLineH + 6f * u

    /** Centered, wrapping lines of "glyph label" pairs below a chart. */
    fun drawLegend(c: Canvas, left: Float, top: Float, width: Float, items: List<Pair<LegendMark, String>>) {
        val colW = 12f * u
        legendLines(items, width).forEachIndexed { li, line ->
            val total = line.sumOf { legendItemW(it.second).toDouble() }.toFloat() + legendSpacing * (line.size - 1)
            var x = left + max(0f, (width - total) / 2f)
            val cy = top + 6f * u + li * legendLineH + legendLineH / 2f
            for ((mark, text) in line) {
                val gx = x + legendGlyphW / 2
                when (mark) {
                    LegendMark.NONE -> drawLevelMark(c, gx, cy, colW, 0f, 0)
                    LegendMark.MILD, LegendMark.MODERATE, LegendMark.SEVERE -> {
                        // fixed-size pills: tiny legend rows would otherwise squash them into dots
                        val lvl = mark.ordinal
                        val w = 7f * u
                        val h = floatArrayOf(0f, 9f, 13.5f, 18f)[lvl] * u
                        fill.color = p.levels[lvl]
                        rect.set(gx - w / 2, cy - h / 2, gx + w / 2, cy + h / 2)
                        c.drawRoundRect(rect, w / 2, w / 2, fill)
                    }
                    LegendMark.YES -> drawYes(c, gx, cy, colW, p.yes)
                    LegendMark.NO -> drawNo(c, gx, cy, colW)
                    LegendMark.MISSING -> drawMissing(c, gx, cy, colW)
                    LegendMark.DOSE_LOW -> drawYes(c, gx, cy, colW, p.levels[1])
                    LegendMark.DOSE_HIGH -> drawYes(c, gx, cy, colW, p.levels[3])
                    LegendMark.DOSE -> c.drawText("mg", gx, cy + dosePaint.textSize * 0.35f, dosePaint)
                }
                x += legendGlyphW + legendGap
                c.drawText(text, x, cy + legendPaint.textSize * 0.35f, legendPaint)
                x += legendPaint.measureText(text) + legendSpacing
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private fun drawRowLabel(c: Canvas, label: String, left: Float, labelW: Float, cy: Float) {
        val text = TextUtils.ellipsize(label, labelPaint, labelW - 8f * u, TextUtils.TruncateAt.END)
        c.drawText(text, 0, text.length, left, cy + labelPaint.textSize * 0.35f, labelPaint)
    }

    private fun drawMissing(c: Canvas, cx: Float, cy: Float, colW: Float) {
        missPaint.textSize = min(10f * u, colW * 1.4f)
        c.drawText("×", cx, cy + missPaint.textSize * 0.34f, missPaint)
    }

    private fun labelStep(colW: Float, n: Int): Int {
        if (n <= 10 && colW >= 18f * u) return 1
        return intArrayOf(1, 2, 7, 14, 28, 56).firstOrNull { colW * it >= 26f * u } ?: 56
    }

    private fun isLabeled(i: Int, n: Int, step: Int) = (n - 1 - i) % step == 0

    private fun drawVerticalGuides(c: Canvas, x0: Float, colW: Float, n: Int, top: Float, bottom: Float) {
        val step = labelStep(colW, n)
        if (step == 1) return
        for (i in 0 until n) if (isLabeled(i, n, step)) {
            val x = x0 + colW * (i + 0.5f)
            c.drawLine(x, top, x, bottom, gridPaint)
        }
        c.drawLine(x0, top, x0, bottom, gridPaint)
    }

    private fun drawDayAxis(c: Canvas, x0: Float, colW: Float, days: List<LocalDate>, y: Float) {
        val n = days.size
        val step = labelStep(colW, n)
        for (i in 0 until n) if (isLabeled(i, n, step)) {
            c.drawText(days[i].dayOfMonth.toString(), x0 + colW * (i + 0.5f), y + 15f * u, axisPaint)
        }
    }

    private fun fmtHours(v: Float) = if (v % 1f == 0f) v.toInt().toString() else v.toString()

    companion object {
        private val LEVEL_HEIGHT = floatArrayOf(0f, 0.40f, 0.62f, 0.88f)
        private val NUMBER = Regex("""\d+(?:[.,]\d+)?""")

        /** "150 mg" -> 150.0, "0,5 мг" -> 0.5, "two pills" -> null */
        fun doseNumber(dose: String): Double? = NUMBER.find(dose)?.value?.replace(',', '.')?.toDoubleOrNull()

        private fun lerpColor(a: Int, b: Int, t: Float): Int {
            fun ch(shift: Int) = (((a shr shift) and 0xFF) + (((b shr shift) and 0xFF) - ((a shr shift) and 0xFF)) * t).toInt()
            return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
        }
    }
}
