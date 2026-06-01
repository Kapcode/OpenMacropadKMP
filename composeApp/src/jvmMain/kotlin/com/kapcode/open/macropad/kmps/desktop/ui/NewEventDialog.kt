package com.kapcode.open.macropad.kmps.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import com.kapcode.open.macropad.kmps.desktop.viewmodel.*
import com.kapcode.open.macropad.kmps.desktop.model.*
import com.kapcode.open.macropad.kmps.desktop.ui.components.AppTooltipArea
import com.kapcode.open.macropad.kmps.desktop.ui.components.TriggerTypeDropdown
import com.kapcode.open.macropad.kmps.desktop.ui.components.ClientMultiSelect
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.kapcode.open.macropad.kmps.ui.components.KeyValidationField
import com.kapcode.open.macropad.kmps.desktop.logic.KeyParser
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme
import com.kapcode.open.macropad.kmps.desktop.viewmodel.ConsoleViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NewEventDialog(
    viewModel: NewEventViewModel,
    selectedTheme: String,
    consoleViewModel: ConsoleViewModel,
    onDismissRequest: () -> Unit,
    onAddEvent: () -> Unit
) {
    AppDialog(
        onCloseRequest = onDismissRequest,
        state = rememberWindowState(width = 550.dp, height = 850.dp),
        title = "Add New Macro Event",
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel
    ) {
        val isTrigger by viewModel.isTriggerEvent.collectAsState()
        val triggerKeysText by viewModel.triggerKeysText.collectAsState()
        val allowedClients by viewModel.allowedClientsText.collectAsState()
        val selectedClients by viewModel.selectedClients.collectAsState()
        val isAllTrusted by viewModel.isAllTrustedSelected.collectAsState()
        val trustedDevices by viewModel.trustedDevices.collectAsState()
        val triggerType by viewModel.triggerType.collectAsState()
        val holdDurationMs by viewModel.holdDurationMs.collectAsState()
        val multiTapCount by viewModel.multiTapCount.collectAsState()
        val tapWindowMs by viewModel.tapWindowMs.collectAsState()
        val sequenceWindowMs by viewModel.sequenceWindowMs.collectAsState()
        val confirmationRequired by viewModel.confirmationRequired.collectAsState()

        val selectedAction by viewModel.selectedAction.collectAsState()
        val useKeys by viewModel.useKeys.collectAsState()
        val keysText by viewModel.keysText.collectAsState()
        val useMouseButtons by viewModel.useMouseButtons.collectAsState()
        val mouseButtonsText by viewModel.mouseButtonsText.collectAsState()
        val useMouseScroll by viewModel.useMouseScroll.collectAsState()
        val mouseScrollText by viewModel.mouseScrollText.collectAsState()
        val useMouseLocation by viewModel.useMouseLocation.collectAsState()
        val mouseX by viewModel.mouseX.collectAsState()
        val mouseY by viewModel.mouseY.collectAsState()
        val animateMouse by viewModel.animateMouseMovement.collectAsState()
        val useDelay by viewModel.useDelay.collectAsState()
        val delayText by viewModel.delayText.collectAsState()
        val useAutoDelay by viewModel.useAutoDelay.collectAsState()
        val autoDelayText by viewModel.autoDelayText.collectAsState()
        val scriptContent by viewModel.scriptContent.collectAsState()
        val isEditMode by viewModel.isEditMode.collectAsState()

        val validationState by viewModel.validationState.collectAsState()
        val isValid = validationState.first
        val validationMessage = validationState.second

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 12.dp) // Space for scrollbar
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(checked = isTrigger, onCheckedChange = { viewModel.isTriggerEvent.value = it })
                        Text("This event defines the Global Trigger for the macro", style = MaterialTheme.typography.titleMedium)
                    }

                    if (isTrigger) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Trigger Configuration", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = confirmationRequired, onCheckedChange = { viewModel.confirmationRequired.value = it })
                                    Text("Require GUI Confirmation before executing", style = MaterialTheme.typography.labelLarge)
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

                                OutlinedTextField(
                                    value = allowedClients,
                                    onValueChange = { viewModel.allowedClientsText.value = it },
                                    label = { Text("Allowed Clients (Manual Override)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Comma separated names/IDs") }
                                )
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Action Step Configuration", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.secondary)

                                ActionDropdown(selectedAction, viewModel)
                                
                                if (selectedAction == MacroAction.SCRIPT) {
                                    OutlinedTextField(
                                        value = scriptContent,
                                        onValueChange = { viewModel.scriptContent.value = it },
                                        label = { Text("JavaScript Content") },
                                        modifier = Modifier.fillMaxWidth().height(200.dp),
                                        placeholder = { Text("kap.pressKey('A'); kap.delay(100); kap.releaseKey('A');") }
                                    )
                                } else {
                                    CheckableTextFieldRow("Key(s):", useKeys, { viewModel.useKeys.value = it }, keysText, { viewModel.keysText.value = it })
                                    CheckableTextFieldRow("Mouse Button(s) (1=Left, 2=Middle, 3=Right):", useMouseButtons, { viewModel.useMouseButtons.value = it }, mouseButtonsText, { viewModel.mouseButtonsText.value = it })
                                    CheckableTextFieldRow("Mouse Scroll:", useMouseScroll, { viewModel.useMouseScroll.value = it }, mouseScrollText, { viewModel.mouseScrollText.value = it })
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Checkbox(checked = useMouseLocation, onCheckedChange = { viewModel.useMouseLocation.value = it })
                                        Text("Mouse Location")
                                        Spacer(Modifier.weight(1f))
                                        Text("Animate")
                                        Switch(checked = animateMouse, onCheckedChange = { viewModel.animateMouseMovement.value = it }, enabled = useMouseLocation)
                                        OutlinedTextField(value = mouseX, onValueChange = { viewModel.mouseX.value = it }, label = { Text("X") }, modifier = Modifier.width(90.dp), enabled = useMouseLocation)
                                        OutlinedTextField(value = mouseY, onValueChange = { viewModel.mouseY.value = it }, label = { Text("Y") }, modifier = Modifier.width(90.dp), enabled = useMouseLocation)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = useDelay, onCheckedChange = { viewModel.useDelay.value = it })
                                        OutlinedTextField(value = delayText, onValueChange = { viewModel.delayText.value = it }, label = { Text("Delay") }, modifier = Modifier.width(120.dp), enabled = useDelay)
                                        Spacer(Modifier.width(8.dp))
                                        Text("milliseconds (1000 = 1s)", style = MaterialTheme.typography.bodySmall)
                                    }
                                    
                                    HorizontalDivider()
                                    
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = useAutoDelay, onCheckedChange = { viewModel.useAutoDelay.value = it })
                                            Text("Auto Delay Declaration")
                                        }
                                        OutlinedTextField(
                                            value = autoDelayText,
                                            onValueChange = { viewModel.autoDelayText.value = it },
                                            label = { Text("Auto Delay Value (ms)") },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = useAutoDelay
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider()
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

            Column(modifier = Modifier.fillMaxWidth()) {
                if (!isValid) {
                    Text(
                        text = validationMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    AppTooltipArea(
                        tooltipText = if(isValid) (if (isEditMode) "Update Event" else "Add Event") else "Fix errors to add",
                        delayMillis = 0
                    ) {
                        FloatingActionButton(
                            onClick = { if (isValid) onAddEvent() },
                            containerColor = if (isValid) MaterialTheme.colorScheme.primaryContainer else Color.Gray,
                            contentColor = if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else Color.DarkGray,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        ) {
                            Icon(Icons.Default.Done, contentDescription = if (isEditMode) "Update Event" else "Add Event")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionDropdown(selectedAction: MacroAction, viewModel: NewEventViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Action:", modifier = Modifier.width(120.dp))
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedAction.name,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                viewModel.actionOptions.forEach { action ->
                    DropdownMenuItem(text = { Text(action.name) }, onClick = {
                        viewModel.selectedAction.value = action
                        expanded = false
                    })
                }
            }
        }
    }
}

@Composable
private fun CheckableTextFieldRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, text: String, onTextChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            enabled = checked
        )
    }
}
