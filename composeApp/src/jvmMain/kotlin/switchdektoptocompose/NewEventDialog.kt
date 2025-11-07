package switchdektoptocompose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEventDialog(
    viewModel: NewEventViewModel,
    onDismissRequest: () -> Unit,
    onAddEvent: (List<MacroEventState>) -> Unit
) {
    val dialogState = rememberDialogState(width = 500.dp, height = 700.dp)

    DialogWindow(
        onCloseRequest = onDismissRequest,
        state = dialogState,
        title = "Add New Macro Event"
    ) {
        // Collect all states
        val isTrigger by viewModel.isTriggerEvent.collectAsState()
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
        val useDelay by viewModel.useDelay.collectAsState()
        val delayText by viewModel.delayText.collectAsState()
        val delayBetweenActions by viewModel.delayBetweenActions.collectAsState()

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isTrigger, onCheckedChange = { viewModel.isTriggerEvent.value = it })
                        Text("Is Trigger Event")
                    }
                    Divider()
                    
                    ActionDropdown(selectedAction, viewModel)
                    
                    CheckableTextFieldRow("Key(s):", useKeys, { viewModel.useKeys.value = it }, keysText, { viewModel.keysText.value = it })
                    CheckableTextFieldRow("Mouse Button(s):", useMouseButtons, { viewModel.useMouseButtons.value = it }, mouseButtonsText, { viewModel.mouseButtonsText.value = it })
                    CheckableTextFieldRow("Mouse Scroll:", useMouseScroll, { viewModel.useMouseScroll.value = it }, mouseScrollText, { viewModel.mouseScrollText.value = it })
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = useMouseLocation, onCheckedChange = { viewModel.useMouseLocation.value = it })
                        Text("Mouse Location:", modifier = Modifier.weight(1f))
                        OutlinedTextField(value = mouseX, onValueChange = { viewModel.mouseX.value = it }, label = { Text("X") }, modifier = Modifier.width(90.dp), enabled = useMouseLocation)
                        Spacer(Modifier.width(8.dp))
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
                }

                Button(
                    onClick = {
                        val events = viewModel.createEvents()
                        onAddEvent(events)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Add")
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