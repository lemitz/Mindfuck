package com.mindfuck.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.mindfuck.app.data.CustomTracker

@Composable
fun MedicationDialog(
    initialName: String = "",
    initialDose: String = "",
    title: String = "Add medication",
    onDismiss: () -> Unit,
    onSave: (name: String, dose: String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var dose by remember { mutableStateOf(initialDose) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    name, { name = it }, label = { Text("Name") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    dose, { dose = it }, label = { Text("Default dose (e.g. 150 mg)") },
                    supportingText = { Text("Can be changed for any single day in Log") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, dose) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun TrackerDialog(
    initialName: String = "",
    initialType: Int = CustomTracker.SCALE,
    allowTypeChange: Boolean = true,
    title: String = "Add custom tracking point",
    onDismiss: () -> Unit,
    onSave: (name: String, type: Int) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var type by remember { mutableIntStateOf(initialType) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    name, { name = it },
                    label = { Text("Name") },
                    supportingText = { Text("Shown as-is in the PDF, so name it in Russian if you want a fully Russian report") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (allowTypeChange) {
                    val types = listOf(CustomTracker.SCALE to "None → Severe", CustomTracker.YES_NO to "Yes / No")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        types.forEachIndexed { i, (t, label) ->
                            SegmentedButton(
                                selected = type == t,
                                onClick = { type = t },
                                shape = SegmentedButtonDefaults.itemShape(i, types.size),
                            ) { Text(label) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, type) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
