package com.mindfuck.app.ui.log

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mindfuck.app.data.CustomTracker
import com.mindfuck.app.data.DayEntry
import com.mindfuck.app.data.Medication
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import com.mindfuck.app.ui.MedicationDialog
import com.mindfuck.app.ui.LevelSelector
import com.mindfuck.app.ui.PagerHeader
import com.mindfuck.app.ui.Question
import com.mindfuck.app.ui.TrackerDialog
import com.mindfuck.app.ui.YesNoSelector
import com.mindfuck.app.ui.app
import com.mindfuck.app.ui.theme.LocalMoodColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DOSE_DATE_FMT = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
private val TITLE_FMT = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
private val TITLE_FMT_YEAR = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH)
private const val DAY_MS = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(day: Long, onDayChange: (Long) -> Unit) {
    val today = LocalDate.now().toEpochDay()
    var pickDate by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        val date = LocalDate.ofEpochDay(day)
        PagerHeader(
            title = date.format(if (date.year == LocalDate.now().year) TITLE_FMT else TITLE_FMT_YEAR),
            onPrev = { onDayChange(day - 1) },
            onNext = { onDayChange(day + 1) },
            nextEnabled = day < today,
            titleColor = MaterialTheme.colorScheme.primary,
            onTitleClick = { pickDate = true },
        )
        HorizontalDivider()

        // The form slides in from the side of the day you moved to.
        AnimatedContent(
            targetState = day,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(260)) { dir * it / 4 } + fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(260)) { -dir * it / 4 } + fadeOut(tween(140)))
            },
            label = "logDay",
        ) { d -> DayForm(d) }
    }

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = day * DAY_MS,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis / DAY_MS <= today
                override fun isSelectableYear(year: Int) = year <= LocalDate.now().year
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDayChange(it / DAY_MS) }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { onDayChange(today); pickDate = false }) { Text("Today") }
            },
        ) { DatePicker(state) }
    }
}

@Composable
private fun DayForm(day: Long) {
    val app = app()
    val repo = app.repository
    val bundle by remember(day) { repo.observeDay(day) }.collectAsState(initial = null)
    val meds by remember { repo.meds }.collectAsState(initial = emptyList())
    val trackers by remember { repo.trackers }.collectAsState(initial = emptyList())
    val entry = bundle?.entry ?: DayEntry(day)

    fun update(transform: (DayEntry) -> DayEntry) = app.write { updateEntry(day, transform) }

    var addTracker by rememberSaveable { mutableStateOf(false) }
    var addMed by rememberSaveable { mutableStateOf(false) }
    var editDose by remember { mutableStateOf<Medication?>(null) }

    run {
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(6.dp))
            SleepQuestion(day = day, value = entry.sleepHours) { v -> update { it.copy(sleepHours = v) } }

            Question("Today's most extreme depressed mood:") {
                LevelSelector(entry.depression, { v -> update { it.copy(depression = v) } })
            }
            Question("Today's most extreme elevated mood:") {
                LevelSelector(entry.elevated, { v -> update { it.copy(elevated = v) } })
            }
            Question("Today's most extreme irritability:") {
                LevelSelector(entry.irritability, { v -> update { it.copy(irritability = v) } })
            }
            Question("Today's most extreme anxiety:") {
                LevelSelector(entry.anxiety, { v -> update { it.copy(anxiety = v) } })
            }
            Question("Psychotic symptoms today:") {
                YesNoSelector(entry.psychotic, { v -> update { it.copy(psychotic = v) } })
            }
            Question("Talk therapy today:") {
                YesNoSelector(entry.therapy, { v -> update { it.copy(therapy = v) } })
            }

            Spacer(Modifier.height(10.dp))
            SectionBox("Custom tracking points:") {
                trackers.filter { it.active }.forEach { t ->
                    val v = bundle?.custom?.get(t.id)
                    Question(t.name, horizontalPadding = 0.dp) {
                        if (t.type == CustomTracker.SCALE) {
                            LevelSelector(v, { nv -> app.write { setCustom(day, t.id, nv) } })
                        } else {
                            YesNoSelector(v?.let { it == 1 }, { nv -> app.write { setCustom(day, t.id, nv?.let { if (it) 1 else 0 }) } })
                        }
                    }
                }
                TextButton(onClick = { addTracker = true }, Modifier.fillMaxWidth()) {
                    Text("Add custom tracking point", style = MaterialTheme.typography.titleMedium)
                }
            }

            Question("Medications (check off those you took today):") {
                val active = meds.filter { it.active }
                if (active.isEmpty()) {
                    Text(
                        "No medications yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                active.forEach { m ->
                    val taken = bundle?.takenMeds?.containsKey(m.id) == true
                    val dose = if (taken) bundle?.takenMeds?.get(m.id) ?: m.dose else m.dose
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { app.write { setIntake(day, m, !taken) } }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = taken, onCheckedChange = { c -> app.write { setIntake(day, m, c) } })
                        Text(m.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (taken) {
                            AssistChip(
                                onClick = { editDose = m },
                                label = { Text(dose.ifBlank { "Set dose" }) },
                                trailingIcon = { Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp)) },
                            )
                        } else if (dose.isNotBlank()) {
                            Text(
                                dose,
                                Modifier.padding(end = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                TextButton(onClick = { addMed = true }) { Text("Add medication") }
            }

            NotesField(day)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (addTracker) {
        TrackerDialog(onDismiss = { addTracker = false }) { name, type ->
            app.write { addTracker(name, type) }
            addTracker = false
        }
    }
    if (addMed) {
        MedicationDialog(onDismiss = { addMed = false }) { name, dose ->
            app.write { addMed(name, dose) }
            addMed = false
        }
    }
    editDose?.let { m ->
        val current = bundle?.takenMeds?.get(m.id) ?: m.dose
        DoseDialog(
            med = m,
            date = LocalDate.ofEpochDay(day),
            current = current,
            onDismiss = { editDose = null },
        ) { newDose, makeDefault ->
            app.write {
                setIntakeDose(day, m.id, newDose)
                if (makeDefault) updateMed(m.copy(dose = newDose.trim()))
            }
            editDose = null
        }
    }
}

/** Dose actually taken on one day, with quick picks from recently used doses. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DoseDialog(
    med: Medication,
    date: LocalDate,
    current: String,
    onDismiss: () -> Unit,
    onSave: (dose: String, makeDefault: Boolean) -> Unit,
) {
    val app = app()
    var dose by remember { mutableStateOf(current) }
    var makeDefault by remember { mutableStateOf(false) }
    var recent by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(med.id) {
        recent = (app.repository.recentDoses(med.id) + med.dose).filter { it.isNotBlank() }.distinct()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${med.name} · ${date.format(DOSE_DATE_FMT)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    dose, { dose = it },
                    label = { Text("Dose taken this day") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (recent.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        recent.forEach { d ->
                            FilterChip(selected = dose.trim() == d, onClick = { dose = d }, label = { Text(d) })
                        }
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { makeDefault = !makeDefault },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = makeDefault, onCheckedChange = { makeDefault = it })
                    Text("Use as default from now on", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "Past days keep the dose they were logged with.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(dose, makeDefault) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SectionBox(title: String, content: @Composable () -> Unit) {
    Surface(color = LocalMoodColors.current.chartBackground, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

/**
 * Slider keeps a local value while dragging and only writes to the DB when the gesture ends,
 * so dragging stays at full frame rate.
 */
@Composable
private fun SleepQuestion(day: Long, value: Float?, onCommit: (Float?) -> Unit) {
    var local by remember(day) { mutableStateOf<Float?>(null) }
    LaunchedEffect(value) { local = null }
    val shown = local ?: value

    Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Last night's sleep (hrs):", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                shown?.let { if (it % 1f == 0f) "${it.toInt()} h" else "$it h" } ?: "not logged",
                style = MaterialTheme.typography.titleMedium,
                color = if (shown == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            )
            if (value != null) {
                TextButton(onClick = { onCommit(null) }) { Text("Clear") }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onCommit(((shown ?: 0f) - 0.5f).coerceIn(0f, 16f)) }) {
                Icon(Icons.Filled.Remove, "Less")
            }
            Slider(
                value = shown ?: 0f,
                // snap to 0.5 h ourselves: `steps` would draw 31 tick dots on the track
                onValueChange = { local = Math.round(it * 2f) / 2f },
                onValueChangeFinished = { onCommit(local) },
                valueRange = 0f..16f,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onCommit(((shown ?: 0f) + 0.5f).coerceIn(0f, 16f)) }) {
                Icon(Icons.Filled.Add, "More")
            }
        }
    }
}

/** Local text state per day; every change is persisted immediately in the app scope. */
@Composable
private fun NotesField(day: Long) {
    val app = app()
    var text by remember(day) { mutableStateOf("") }
    var loaded by remember(day) { mutableStateOf(false) }
    LaunchedEffect(day) {
        text = app.repository.getEntry(day)?.notes.orEmpty()
        loaded = true
    }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text("Self-observation notes:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { v ->
                text = v
                app.write { updateEntry(day) { it.copy(notes = v) } }
            },
            enabled = loaded,
            minLines = 4,
            placeholder = { Text("One thought per line…") },
            supportingText = { Text("Each line becomes a bullet point in the PDF report") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
