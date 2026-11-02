package com.mindfuck.app.ui.graph

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mindfuck.app.charts.ChartRenderer
import com.mindfuck.app.charts.LegendMark
import com.mindfuck.app.charts.MedRow
import com.mindfuck.app.data.CustomTracker
import com.mindfuck.app.data.Medication
import com.mindfuck.app.data.RangeData
import com.mindfuck.app.ui.ChartCard
import com.mindfuck.app.ui.PagerHeader
import com.mindfuck.app.ui.app
import com.mindfuck.app.ui.rememberChartRenderer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SPANS = listOf(7, 30, 90)
private val RANGE_FMT = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)

val LEVEL_LEGEND = listOf(
    LegendMark.NONE to "None", LegendMark.MILD to "Mild", LegendMark.MODERATE to "Moderate",
    LegendMark.SEVERE to "Severe", LegendMark.MISSING to "No data",
)
val YES_NO_LEGEND = listOf(LegendMark.YES to "Yes", LegendMark.NO to "No", LegendMark.MISSING to "No data")
val MEDS_LEGEND = listOf(
    LegendMark.YES to "Taken", LegendMark.NO to "Not taken", LegendMark.MISSING to "No data",
    LegendMark.DOSE_LOW to "Lower dose", LegendMark.DOSE_HIGH to "Higher dose", LegendMark.DOSE to "Dose change",
)

@Composable
fun GraphScreen() {
    val repo = app().repository
    var span by rememberSaveable { mutableIntStateOf(30) }
    var pagesBack by rememberSaveable { mutableIntStateOf(0) }

    val today = LocalDate.now()
    val to = today.minusDays((pagesBack * span).toLong())
    val from = to.minusDays((span - 1).toLong())

    // Keep showing the previous period until the new one is loaded: no empty flash in between.
    var range by remember { mutableStateOf<RangeData?>(null) }
    LaunchedEffect(from, to) { repo.observeRange(from.toEpochDay(), to.toEpochDay()).collect { range = it } }
    val meds by remember { repo.meds }.collectAsState(initial = emptyList())
    val trackers by remember { repo.trackers }.collectAsState(initial = emptyList())
    val chart = rememberChartRenderer()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PagerHeader(
            title = if (pagesBack == 0) "Last $span Days" else "${from.format(RANGE_FMT)} – ${to.format(RANGE_FMT)}",
            onPrev = { pagesBack++ },
            onNext = { pagesBack-- },
            nextEnabled = pagesBack > 0,
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
            SPANS.forEachIndexed { i, s ->
                SegmentedButton(
                    selected = span == s,
                    onClick = { span = s; pagesBack = 0 },
                    shape = SegmentedButtonDefaults.itemShape(i, SPANS.size),
                    icon = {},
                ) { Text("$s days") }
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()

        AnimatedContent(
            targetState = range,
            contentKey = { it?.from to it?.to },
            transitionSpec = {
                val older = (targetState?.from ?: 0) < (initialState?.from ?: 0)
                val dir = if (older) -1 else 1
                (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { dir * it / 6 }) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally(tween(260)) { -dir * it / 6 })
            },
            label = "graphRange",
        ) { r ->
            if (r != null) Charts(r, meds, trackers, chart)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Charts(r: RangeData, meds: List<Medication>, trackers: List<CustomTracker>, chart: ChartRenderer) {
    val days = remember(r.from, r.to) { (r.from..r.to).map(LocalDate::ofEpochDay) }
    val maxLabelPx = with(LocalDensity.current) { 120.dp.toPx() }
    fun e(d: LocalDate) = r.entry(d.toEpochDay())

    val levelLabels = mutableListOf("Depressed", "Elevated", "Irritability", "Anxiety")
    val levelRows = mutableListOf(
        days.map { e(it)?.depression }, days.map { e(it)?.elevated },
        days.map { e(it)?.irritability }, days.map { e(it)?.anxiety },
    )
    val ynLabels = mutableListOf("Psychotic Symptoms", "Talk Therapy")
    val ynRows = mutableListOf(days.map { e(it)?.psychotic }, days.map { e(it)?.therapy })
    trackers.filter { t -> t.active || r.custom.values.any { it.containsKey(t.id) } }.forEach { t ->
        val values = days.map { r.custom(it.toEpochDay(), t.id) }
        if (t.isScale) { levelLabels += t.name; levelRows += values }
        else { ynLabels += t.name; ynRows += values.map { v -> v?.let { it == 1 } } }
    }
    val shownMeds = meds.filter { m -> m.active || r.hasMed(m.id) }
    val medRows = shownMeds.map { m -> MedRow(m.name, days.map { r.doseOn(it.toEpochDay(), m) }) }
    val logged = days.map { r.isLogged(it.toEpochDay()) }
    val labelW = chart.labelColumnWidth(levelLabels + ynLabels + shownMeds.map { it.name }, maxLabelPx)
    val sleepW = chart.sleepAxisWidth()
    val sleep = days.map { e(it)?.sleepHours }
    val known = sleep.filterNotNull()

    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        ChartCard(
            "Sleep", chart.sleepHeight(), chart,
            subtitle = if (known.isEmpty()) "No sleep logged" else
                "Average %.1f h (dashed line)".format(Locale.US, known.average()),
        ) { c, w ->
            chart.drawSleep(c, 0f, 0f, w, sleepW, days, sleep)
        }
        ChartCard("Low to High Tracking Points", chart.levelsHeight(levelRows.size), chart, LEVEL_LEGEND) { c, w ->
            chart.drawLevels(c, 0f, 0f, w, labelW, days, levelLabels, levelRows)
        }
        ChartCard("Yes/No Tracking Points", chart.yesNoHeight(ynRows.size), chart, YES_NO_LEGEND) { c, w ->
            chart.drawYesNo(c, 0f, 0f, w, labelW, days, ynLabels, ynRows)
        }
        if (medRows.isNotEmpty()) {
            ChartCard("Medications", chart.medsHeight(medRows.size), chart, MEDS_LEGEND) { c, w ->
                chart.drawMeds(c, 0f, 0f, w, labelW, days, medRows, logged)
            }
        }
    }
}
