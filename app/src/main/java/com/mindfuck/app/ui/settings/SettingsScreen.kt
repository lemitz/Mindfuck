package com.mindfuck.app.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mindfuck.app.data.Backup
import com.mindfuck.app.data.Downloads
import com.mindfuck.app.data.EmoodsImport
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.mindfuck.app.data.CustomTracker
import com.mindfuck.app.data.Medication
import com.mindfuck.app.ui.ConfirmDialog
import com.mindfuck.app.ui.MedicationDialog
import com.mindfuck.app.ui.TrackerDialog
import com.mindfuck.app.ui.app
import com.mindfuck.app.ui.theme.LocalMoodColors
import com.mindfuck.app.ui.theme.ThemeSettings
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.LightMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = app()
    val repo = app.repository
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val meds by remember { repo.meds }.collectAsState(initial = emptyList())
    val trackers by remember { repo.trackers }.collectAsState(initial = emptyList())

    var editMed by remember { mutableStateOf<Medication?>(null) }
    var newMed by remember { mutableStateOf(false) }
    var editTracker by remember { mutableStateOf<CustomTracker?>(null) }
    var newTracker by remember { mutableStateOf(false) }
    var deleteMed by remember { mutableStateOf<Medication?>(null) }
    var deleteTracker by remember { mutableStateOf<CustomTracker?>(null) }
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching {
                withContext(Dispatchers.IO) {
                    val json = Backup.export(repo.dao)
                    context.contentResolver.openOutputStream(uri)!!.use { it.write(json.toByteArray()) }
                }
            }.isSuccess
            Toast.makeText(context, if (ok) "Backup saved" else "Backup failed", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri
    }
    var pendingEmoods by remember { mutableStateOf<android.net.Uri?>(null) }
    var emoodsBusy by remember { mutableStateOf(false) }
    var emoodsResult by remember { mutableStateOf<Pair<String, String>?>(null) } // title to text
    val emoodsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingEmoods = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LocalMoodColors.current.topBar,
                    titleContentColor = LocalMoodColors.current.onTopBar,
                    navigationIconContentColor = LocalMoodColors.current.onTopBar,
                ),
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Text("Appearance", Modifier.padding(start = 20.dp, top = 16.dp), style = MaterialTheme.typography.titleMedium)
                ListItem(
                    modifier = Modifier.clickable { ThemeSettings.setLight(context, !ThemeSettings.light) },
                    leadingContent = { Icon(Icons.Outlined.LightMode, null) },
                    headlineContent = { Text("Light theme") },
                    supportingContent = { Text("Dark is the default") },
                    trailingContent = {
                        Switch(checked = ThemeSettings.light, onCheckedChange = { ThemeSettings.setLight(context, it) })
                    },
                )
                HorizontalDivider(Modifier.padding(top = 4.dp))
            }
            item { Header("Medications") { newMed = true } }
            if (meds.isEmpty()) item { Empty("No medications") }
            items(meds, key = { "m${it.id}" }) { m ->
                ListItem(
                    modifier = Modifier.animateItem(),
                    headlineContent = { Text(m.name) },
                    supportingContent = {
                        Text(
                            listOfNotNull(
                                m.dose.ifBlank { null }?.let { "default $it" },
                                if (m.active) null else "hidden",
                            ).joinToString(" · ").ifEmpty { "—" },
                        )
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = m.active, onCheckedChange = { a -> app.write { updateMed(m.copy(active = a)) } })
                            IconButton(onClick = { editMed = m }) { Icon(Icons.Outlined.Edit, "Edit") }
                            IconButton(onClick = { deleteMed = m }) { Icon(Icons.Outlined.Delete, "Delete") }
                        }
                    },
                )
            }
            item { HorizontalDivider(Modifier.padding(top = 8.dp)) }

            item { Header("Custom tracking points") { newTracker = true } }
            if (trackers.isEmpty()) item { Empty("No custom tracking points") }
            items(trackers, key = { "t${it.id}" }) { t ->
                ListItem(
                    modifier = Modifier.animateItem(),
                    headlineContent = { Text(t.name) },
                    supportingContent = { Text((if (t.isScale) "None → Severe" else "Yes / No") + if (t.active) "" else " · hidden") },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = t.active, onCheckedChange = { a -> app.write { updateTracker(t.copy(active = a)) } })
                            IconButton(onClick = { editTracker = t }) { Icon(Icons.Outlined.Edit, "Edit") }
                            IconButton(onClick = { deleteTracker = t }) { Icon(Icons.Outlined.Delete, "Delete") }
                        }
                    },
                )
            }
            item { HorizontalDivider(Modifier.padding(top = 8.dp)) }

            item {
                Text("Backup", Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp), style = MaterialTheme.typography.titleMedium)
                ListItem(
                    modifier = Modifier.padding(0.dp),
                    leadingContent = { Icon(Icons.Outlined.Download, null) },
                    headlineContent = {
                        TextButton(onClick = { exportLauncher.launch("mindfuck_backup_${LocalDate.now()}.json") }) {
                            Text("Export all data (JSON)")
                        }
                    },
                )
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Upload, null) },
                    headlineContent = {
                        TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                            Text("Restore from backup")
                        }
                    },
                )
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.History, null) },
                    headlineContent = {
                        TextButton(
                            onClick = { emoodsLauncher.launch(arrayOf("*/*")) },
                            enabled = !emoodsBusy,
                        ) { Text(if (emoodsBusy) "Importing…" else "Import from eMoods (.emoods)") }
                    },
                    supportingContent = {
                        Text("Adds your eMoods history. Days you already logged here are kept; only empty fields are filled in.")
                    },
                )
                Text(
                    "All data stays on this device. Nothing is sent anywhere.",
                    Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val version = remember {
                    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
                }
                Text(
                    "Mindfuck ${version ?: ""}",
                    Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (newMed) MedicationDialog(onDismiss = { newMed = false }) { n, d -> app.write { addMed(n, d) }; newMed = false }
    editMed?.let { m ->
        MedicationDialog(m.name, m.dose, "Edit medication", onDismiss = { editMed = null }) { n, d ->
            app.write { updateMed(m.copy(name = n.trim(), dose = d.trim())) }; editMed = null
        }
    }
    if (newTracker) TrackerDialog(onDismiss = { newTracker = false }) { n, t -> app.write { addTracker(n, t) }; newTracker = false }
    editTracker?.let { t ->
        TrackerDialog(t.name, t.type, allowTypeChange = false, title = "Rename tracking point", onDismiss = { editTracker = null }) { n, _ ->
            app.write { updateTracker(t.copy(name = n.trim())) }; editTracker = null
        }
    }
    deleteMed?.let { m ->
        ConfirmDialog("Delete ${m.name}?", "All logged intakes of this medication will be removed too. To keep history, hide it instead.", "Delete", { deleteMed = null }) {
            app.write { deleteMed(m.id) }
        }
    }
    deleteTracker?.let { t ->
        ConfirmDialog("Delete ${t.name}?", "All logged values will be removed too. To keep history, hide it instead.", "Delete", { deleteTracker = null }) {
            app.write { deleteTracker(t.id) }
        }
    }
    pendingEmoods?.let { uri ->
        ConfirmDialog(
            title = "Import from eMoods?",
            text = "Days from the eMoods export are added to your diary. Nothing is deleted: days you already logged " +
                "keep their values and only get fields that are still empty. A backup of your current diary is " +
                "saved to Downloads/Mindfuck first.",
            confirm = "Import",
            onDismiss = { pendingEmoods = null },
        ) {
            emoodsBusy = true
            scope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        // 1. parse first: a wrong file fails here, before anything is touched
                        val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                        val data = EmoodsImport.read(bytes.inputStream())
                        require(data.entries.isNotEmpty()) { "The export contains no days" }
                        // 2. safety backup of the current diary; no backup, no import
                        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"))
                        val backupName = "mindfuck_backup_before_emoods_$stamp.json"
                        val json = Backup.export(repo.dao)
                        check(Downloads.save(context, backupName, "application/json", json.toByteArray())) {
                            "Could not save the safety backup, nothing was imported"
                        }
                        // 3. merge
                        val plan = repo.importEmoods(data)
                        val range = "${data.entries.first().date} – ${data.entries.last().date}"
                        "eMoods export ($range): ${plan.summary}\n\nBackup of the diary before the import: Downloads/Mindfuck/$backupName"
                    }
                }
                emoodsBusy = false
                emoodsResult = result.fold(
                    { "Import finished" to it },
                    { "Import failed" to (it.message ?: it.toString()) },
                )
            }
        }
    }
    emoodsResult?.let { (title, text) ->
        AlertDialog(
            onDismissRequest = { emoodsResult = null },
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { emoodsResult = null }) { Text("OK") } },
        )
    }

    pendingImport?.let { uri ->
        ConfirmDialog("Restore backup?", "This replaces ALL current data with the backup contents.", "Restore", { pendingImport = null }) {
            scope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        val json = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                        Backup.import(repo.dao, json)
                    }
                }
                Toast.makeText(
                    context,
                    result.fold({ "Backup restored" }, { "Restore failed: ${it.message}" }),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }
}

@Composable
private fun Header(title: String, onAdd: () -> Unit) {
    Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onAdd) { Text("Add") }
    }
}

@Composable
private fun Empty(text: String) {
    Text(text, Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
}
