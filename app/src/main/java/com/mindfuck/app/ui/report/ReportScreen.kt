package com.mindfuck.app.ui.report

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.mindfuck.app.report.ReportExporter
import com.mindfuck.app.report.ReportOptions
import com.mindfuck.app.ui.ConfirmDialog
import com.mindfuck.app.ui.app
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DAY_MS = 86_400_000L
private val DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
private val FILE_TIME_FMT = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.ENGLISH)

private enum class Preset(val label: String) {
    LAST_7("Last 7 days"), LAST_14("Last 14 days"), LAST_30("Last 30 days"),
    THIS_MONTH("This month"), PREV_MONTH("Previous month"), CUSTOM("Custom…");

    fun range(today: LocalDate): Pair<LocalDate, LocalDate>? = when (this) {
        LAST_7 -> today.minusDays(6) to today
        LAST_14 -> today.minusDays(13) to today
        LAST_30 -> today.minusDays(29) to today
        THIS_MONTH -> today.withDayOfMonth(1) to today
        PREV_MONTH -> YearMonth.from(today).minusMonths(1).let { it.atDay(1) to it.atEndOfMonth() }
        CUSTOM -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportScreen() {
    val app = app()
    val context = LocalContext.current
    val exporter = remember { ReportExporter(context.applicationContext, app.repository) }
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()

    var preset by rememberSaveable { mutableStateOf(Preset.LAST_7) }
    var customFrom by rememberSaveable { mutableLongStateOf(today.minusDays(6).toEpochDay()) }
    var customTo by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var pickRange by rememberSaveable { mutableStateOf(false) }
    var askOptions by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var toDelete by remember { mutableStateOf<File?>(null) }

    val (from, to) = preset.range(today) ?: (LocalDate.ofEpochDay(customFrom) to LocalDate.ofEpochDay(customTo))
    val files = remember(refresh) { exporter.list() }

    fun open(f: File) = try {
        context.startActivity(exporter.openIntent(f))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No PDF viewer installed", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Report period", style = MaterialTheme.typography.titleLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Preset.entries.forEach { p ->
                        FilterChip(
                            selected = preset == p,
                            onClick = { if (p == Preset.CUSTOM) pickRange = true else preset = p },
                            label = { Text(p.label) },
                        )
                    }
                }
                Text(
                    "${from.format(DATE_FMT)} – ${to.format(DATE_FMT)}  ·  ${to.toEpochDay() - from.toEpochDay() + 1} days",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "The PDF is in Russian and follows the «Дневник настроения и симптомов» layout. " +
                        "You'll pick extra sections in the next step.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { askOptions = true },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("Generate PDF report", style = MaterialTheme.typography.titleMedium)
                }
            }
            HorizontalDivider()
            Text(
                "Generated reports",
                Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Copies are also saved to Downloads/Mindfuck",
                Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (files.isEmpty()) {
                Text(
                    "Nothing yet.",
                    Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(files, key = { it.name }) { f ->
            ListItem(
                modifier = Modifier.animateItem().clickable { open(f) },
                leadingContent = { Icon(Icons.Outlined.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary) },
                headlineContent = { Text(f.name, fontWeight = FontWeight.Medium) },
                supportingContent = {
                    val t = java.time.Instant.ofEpochMilli(f.lastModified()).atZone(java.time.ZoneId.systemDefault())
                    Text("${t.format(FILE_TIME_FMT)} · ${f.length() / 1024} KB")
                },
                trailingContent = {
                    Row {
                        IconButton(onClick = { context.startActivity(exporter.shareIntent(f)) }) {
                            Icon(Icons.Outlined.Share, "Share")
                        }
                        IconButton(onClick = { toDelete = f }) { Icon(Icons.Outlined.Delete, "Delete") }
                    }
                },
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (pickRange) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = customFrom * DAY_MS,
            initialSelectedEndDateMillis = customTo * DAY_MS,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis / DAY_MS <= today.toEpochDay()
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickRange = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null,
                    onClick = {
                        val s = state.selectedStartDateMillis!! / DAY_MS
                        val e = (state.selectedEndDateMillis ?: state.selectedStartDateMillis!!) / DAY_MS
                        customFrom = s; customTo = e; preset = Preset.CUSTOM
                        pickRange = false
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickRange = false }) { Text("Cancel") } },
        ) {
            DateRangePicker(state, Modifier.weight(1f), title = { Text("Report period", Modifier.padding(start = 24.dp, top = 16.dp)) })
        }
    }

    if (askOptions) {
        OptionsDialog(
            initial = remember { ReportOptions.load(context) },
            onDismiss = { askOptions = false },
        ) { options ->
            askOptions = false
            options.save(context)
            busy = true
            scope.launch {
                try {
                    val file = exporter.export(from, to, options)
                    refresh++
                    Toast.makeText(context, "Saved ${file.name}", Toast.LENGTH_SHORT).show()
                    open(file)
                } catch (t: Throwable) {
                    Toast.makeText(context, "Export failed: ${t.message}", Toast.LENGTH_LONG).show()
                } finally {
                    busy = false
                }
            }
        }
    }

    toDelete?.let { f ->
        ConfirmDialog(
            title = "Delete ${f.name}?",
            text = "The copy in Downloads/Mindfuck stays.",
            confirm = "Delete",
            onDismiss = { toDelete = null },
        ) { exporter.delete(f); refresh++ }
    }
}

/** Asked before every export: which sections to add on top of the base report. */
@Composable
private fun OptionsDialog(initial: ReportOptions, onDismiss: () -> Unit, onGenerate: (ReportOptions) -> Unit) {
    var o by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What goes into the report?") },
        text = {
            Column {
                OptionRow(
                    "Base report", "Scale, day table, period summary, notes of the last day",
                    checked = true, enabled = false,
                ) {}
                OptionRow("Sleep", "Hours row in the table + averages", o.sleep) { o = o.copy(sleep = it) }
                OptionRow("Psychotic symptoms & therapy", "Yes/no rows", o.yesNo) { o = o.copy(yesNo = it) }
                OptionRow("Medications", "Intake table + adherence %", o.meds) { o = o.copy(meds = it) }
                OptionRow("Custom tracking points", "Your own trackers", o.custom) { o = o.copy(custom = it) }
                OptionRow("Charts", "Same graphs as in the app", o.charts) { o = o.copy(charts = it) }
                OptionRow("Notes for every day", "Not only the last day with notes", o.allNotes) { o = o.copy(allNotes = it) }
            }
        },
        confirmButton = { TextButton(onClick = { onGenerate(o) }) { Text("Generate") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun OptionRow(title: String, hint: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange, enabled = enabled)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
