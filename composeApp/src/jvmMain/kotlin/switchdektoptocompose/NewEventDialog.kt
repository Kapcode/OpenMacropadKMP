package switchdektoptocompose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NewEventDialog(
    viewModel: NewEventViewModel,
    selectedTheme: String,
    onDismissRequest: () -> Unit,
    onAddEvent: () -> Unit
) {
    val dialogState = rememberDialogState(width = 550.dp, height = 850.dp)

    DialogWindow(
        onCloseRequest = onDismissRequest,
        state = dialogState,
        title = "Add New Macro Event"
    ) {
        AppTheme(useDarkTheme = selectedTheme == "Dark Blue") {
            val isTrigger by viewModel.isTriggerEvent.collectAsState()
            val allowedClients by viewModel.allowedClientsText.collectAsState()
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
            val delayBetweenActions by viewModel.delayBetweenActions.collectAsState()
            val useAutoDelay by viewModel.useAutoDelay.collectAsState()
            val autoDelayText by viewModel.autoDelayText.collectAsState()

            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(checked = isTrigger, onCheckedChange = { viewModel.isTriggerEvent.value = it })
                            Text("Is Trigger Event")
                            OutlinedTextField(
                                value = allowedClients,
                                onValueChange = { viewModel.allowedClientsText.value = it },
                                label = { Text("Allowed Clients") },
                                modifier = Modifier.weight(1f),
                                enabled = isTrigger
                            )
                        }
                        Divider()
                        
                        ActionDropdown(selectedAction, viewModel)
                        
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
                        
                        Divider()

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = delayBetweenActions, onCheckedChange = { viewModel.delayBetweenActions.value = it })
                            Text("Delay between each action in this event")
                        }
                        
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

                    Box(modifier = Modifier.fillMaxWidth()) {
                        TooltipArea(
                            tooltip = { Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp){ Text("Add Event", modifier = Modifier.padding(4.dp)) } },
                            modifier = Modifier.align(Alignment.BottomEnd),
                            delayMillis = 0
                        ) {
                            FloatingActionButton(onClick = onAddEvent) {
                                Icon(Icons.Default.Done, contentDescription = "Add Event")
                            }
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
                modifier = Modifier.menuAnchor()
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