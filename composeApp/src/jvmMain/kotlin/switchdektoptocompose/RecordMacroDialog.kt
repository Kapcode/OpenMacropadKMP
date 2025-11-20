package switchdektoptocompose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordMacroDialog(
    viewModel: RecordMacroViewModel,
    selectedTheme: String,
    onDismissRequest: () -> Unit,
    onStartRecording: () -> Unit
) {
    val dialogState = rememberDialogState(width = 800.dp, height = 600.dp)

    DialogWindow(
        onCloseRequest = onDismissRequest,
        state = dialogState,
        title = "Record a New Macro"
    ) {
        AppTheme(useDarkTheme = selectedTheme == "Dark Blue") {
            val recordKeys by viewModel.recordKeys.collectAsState()
            val recordMouseButtons by viewModel.recordMouseButtons.collectAsState()
            val recordMouseMoves by viewModel.recordMouseMoves.collectAsState()
            val recordMouseScroll by viewModel.recordMouseScroll.collectAsState()
            val recordDelays by viewModel.recordDelays.collectAsState()
            val useAutoDelay by viewModel.useAutoDelay.collectAsState()
            val autoDelayMs by viewModel.autoDelayMs.collectAsState()
            val macroName by viewModel.macroName.collectAsState()
            val useRecordingDuration by viewModel.useRecordingDuration.collectAsState()
            val recordingDurationMs by viewModel.recordingDurationMs.collectAsState()
            val selectedStopKey by viewModel.selectedStopKey.collectAsState()
            
            val validationState by viewModel.validationState.collectAsState()
            val isValid = validationState.first
            val validationMessage = validationState.second

            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("What are we recording?", style = MaterialTheme.typography.headlineSmall)
                    
                    // Recording options
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CheckboxWithLabel("Keys", recordKeys, { viewModel.recordKeys.value = it })
                        CheckboxWithLabel("Mouse Buttons", recordMouseButtons, { viewModel.recordMouseButtons.value = it })
                        CheckboxWithLabel("Mouse Moves", recordMouseMoves, { viewModel.recordMouseMoves.value = it })
                        CheckboxWithLabel("Mouse Scroll", recordMouseScroll, { viewModel.recordMouseScroll.value = it })
                    }
                    
                    Divider()
                    
                    // Delay options
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CheckboxWithLabel("Record Delays", recordDelays, { viewModel.recordDelays.value = it })
                        Spacer(Modifier.width(24.dp))
                        Checkbox(checked = useAutoDelay, onCheckedChange = { viewModel.useAutoDelay.value = it })
                        OutlinedTextField(
                            value = autoDelayMs,
                            onValueChange = { viewModel.autoDelayMs.value = it },
                            label = { Text("Auto Delay (ms)") },
                            modifier = Modifier.width(120.dp),
                            enabled = useAutoDelay
                        )
                    }

                    Divider()
                    
                    // Macro Name
                    OutlinedTextField(
                        value = macroName,
                        onValueChange = { viewModel.macroName.value = it },
                        label = { Text("Macro Name") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = !isValid
                    )
                    
                    // Recording Stop Condition
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(checked = useRecordingDuration, onCheckedChange = { viewModel.useRecordingDuration.value = it })
                        OutlinedTextField(
                            value = recordingDurationMs,
                            onValueChange = { viewModel.recordingDurationMs.value = it },
                            label = { Text("Recording Duration (timeout)") },
                            modifier = Modifier.width(200.dp),
                            enabled = useRecordingDuration
                        )
                        Spacer(Modifier.weight(1f))
                        StopKeyDropdown(viewModel)
                    }
                    
                    Spacer(Modifier.weight(1f)) // Push buttons to bottom
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         if (!isValid) {
                            Text(
                                text = validationMessage,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Button(onClick = onDismissRequest) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(16.dp))
                        Button(onClick = { if (isValid) onStartRecording() }, enabled = isValid) {
                            Text("Start Recording")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckboxWithLabel(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StopKeyDropdown(viewModel: RecordMacroViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val selectedStopKey by viewModel.selectedStopKey.collectAsState()

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = "Stop Key: $selectedStopKey",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            viewModel.availableStopKeys.forEach { key ->
                DropdownMenuItem(text = { Text(key) }, onClick = {
                    viewModel.selectedStopKey.value = key
                    expanded = false
                })
            }
        }
    }
}