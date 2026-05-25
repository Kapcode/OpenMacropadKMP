package switchdektoptocompose.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import switchdektoptocompose.viewmodel.*
import switchdektoptocompose.model.*
import switchdektoptocompose.ui.components.TriggerTypeDropdown
import switchdektoptocompose.ui.components.ClientMultiSelect
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.kapcode.open.macropad.kmps.ui.components.KeyValidationField
import switchdektoptocompose.logic.KeyParser
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme
import switchdektoptocompose.viewmodel.ConsoleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordMacroDialog(
    viewModel: RecordMacroViewModel,
    selectedTheme: String,
    consoleViewModel: ConsoleViewModel,
    onDismissRequest: () -> Unit,
    onStartRecording: () -> Unit
) {
    AppDialog(
        onCloseRequest = onDismissRequest,
        state = rememberWindowState(width = 800.dp, height = 600.dp),
        title = "Record a New Macro",
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel
    ) {
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

        val triggerType by viewModel.triggerType.collectAsState()
        val holdDurationMs by viewModel.holdDurationMs.collectAsState()
        val multiTapCount by viewModel.multiTapCount.collectAsState()
        val tapWindowMs by viewModel.tapWindowMs.collectAsState()
        val sequenceWindowMs by viewModel.sequenceWindowMs.collectAsState()
        val triggerKeysText by viewModel.triggerKeysText.collectAsState()
        val confirmationRequired by viewModel.confirmationRequired.collectAsState()
        val selectedClients by viewModel.selectedClients.collectAsState()
        val isAllTrusted by viewModel.isAllTrustedSelected.collectAsState()
        val trustedDevices by viewModel.trustedDevices.collectAsState()
        
        val validationState by viewModel.validationState.collectAsState()
        val isValid = validationState.first
        val validationMessage = validationState.second

        val scrollState = rememberScrollState()
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 12.dp) // Space for scrollbar
                    .padding(16.dp)
                    .verticalScroll(scrollState),
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
                        
                        HorizontalDivider()
                        
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

                        HorizontalDivider()
                        
                        Text("Trigger Settings", style = MaterialTheme.typography.titleMedium)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = confirmationRequired, onCheckedChange = { viewModel.confirmationRequired.value = it })
                            Text("Require GUI Confirmation", style = MaterialTheme.typography.labelLarge)
                        }

                        KeyValidationField(
                            label = "Trigger Key(s)",
                            value = triggerKeysText,
                            onValueChange = { viewModel.triggerKeysText.value = it },
                            isValidKey = { KeyParser.isValidKey(it) },
                            getSuggestions = { input ->
                                val query = input.split('+', ',').lastOrNull()?.trim()?.uppercase() ?: ""
                                if (query.isEmpty()) emptyList()
                                else KeyParser.getAllValidKeyNames().filter { it.startsWith(query) }.take(5)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        TriggerTypeDropdown(triggerType) { viewModel.triggerType.value = it }
                        
                        when (triggerType) {
                            TriggerType.HOLD -> {
                                OutlinedTextField(
                                    value = holdDurationMs,
                                    onValueChange = { viewModel.holdDurationMs.value = it },
                                    label = { Text("Hold Duration (ms)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            TriggerType.MULTI_TAP -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = multiTapCount,
                                        onValueChange = { viewModel.multiTapCount.value = it },
                                        label = { Text("Tap Count") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = tapWindowMs,
                                        onValueChange = { viewModel.tapWindowMs.value = it },
                                        label = { Text("Tap Window (ms)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            TriggerType.SEQUENCE -> {
                                OutlinedTextField(
                                    value = sequenceWindowMs,
                                    onValueChange = { viewModel.sequenceWindowMs.value = it },
                                    label = { Text("Sequence Window (ms)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            else -> {}
                        }

                        ClientMultiSelect(
                            selectedClients = selectedClients,
                            isAllTrusted = isAllTrusted,
                            trustedDevices = trustedDevices,
                            onClientsChanged = { viewModel.selectedClients.value = it },
                            onAllTrustedChanged = { viewModel.isAllTrustedSelected.value = it }
                        )

                        HorizontalDivider()

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

                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(scrollState),
                        style = ScrollbarStyle(
                            minimalHeight = 16.dp,
                            thickness = 8.dp,
                            shape = MaterialTheme.shapes.small,
                            hoverDurationMillis = 300,
                            unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f)
                        )
                    )
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
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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