package com.kapcode.open.macropad.kmps.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.kapcode.open.macropad.kmps.models.GridWidget
import com.kapcode.open.macropad.kmps.models.MacroPack
import com.kapcode.open.macropad.kmps.desktop.model.MacroFileState
import com.kapcode.open.macropad.kmps.desktop.ui.components.AppTooltipArea
import com.kapcode.open.macropad.kmps.desktop.viewmodel.MacroManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackEditorDialog(
    pack: MacroPack,
    macroManagerViewModel: MacroManagerViewModel,
    availableMacros: List<MacroFileState>,
    onDismissRequest: () -> Unit,
    onSave: (MacroPack) -> Unit,
    onOpenInJsonEditor: (MacroPack) -> Unit
) {
    LaunchedEffect(Unit) {
        println("[PackEditor] Dialog launched for pack: ${pack.name} (${pack.id})")
    }

    var name by remember { mutableStateOf(pack.name) }
    var author by remember { mutableStateOf(pack.author) }
    var version by remember { mutableStateOf(pack.version) }

    var targetProcess by remember { mutableStateOf(pack.targetProcess ?: "") }
    var targetWindowTitle by remember { mutableStateOf(pack.targetWindowTitle ?: "") }
    var switchMode by remember { mutableStateOf(if (pack.targetWindowTitle != null) "TITLE" else "PROCESS") }

    var isActive by remember { mutableStateOf(pack.isActive) }
    var widgets by remember { mutableStateOf(pack.widgets) }
    var routines by remember { mutableStateOf(pack.routines) }

    var widgetToEdit by remember { mutableStateOf<GridWidget?>(null) }
    var showWidgetEditor by remember { mutableStateOf(false) }
    
    var routineToEdit by remember { mutableStateOf<com.kapcode.open.macropad.kmps.models.AutomationRoutine?>(null) }
    var showRoutineEditor by remember { mutableStateOf(false) }

    DialogWindow(
        onCloseRequest = onDismissRequest,
        state = rememberDialogState(width = 800.dp, height = 600.dp),
        title = "Edit Pack: $name"
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pack Editor") },
                    actions = {
                        val currentPack = pack.copy(
                            name = name,
                            author = author,
                            version = version,
                            targetProcess = if (switchMode == "PROCESS" && targetProcess.isNotBlank()) targetProcess else null,
                            targetWindowTitle = if (switchMode == "TITLE" && targetWindowTitle.isNotBlank()) targetWindowTitle else null,
                            isActive = isActive,
                            widgets = widgets,
                            routines = routines
                        )
                        TextButton(onClick = { onOpenInJsonEditor(currentPack) }) {
                            Text("Open in JSON Editor")
                        }
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )
            },
            bottomBar = {
                Surface(tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismissRequest) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                println("[PackEditor] Save button clicked.")
                                onSave(pack.copy(
                                    name = name,
                                    author = author,
                                    version = version,
                                    targetProcess = if (switchMode == "PROCESS" && targetProcess.isNotBlank()) targetProcess else null,
                                    targetWindowTitle = if (switchMode == "TITLE" && targetWindowTitle.isNotBlank()) targetWindowTitle else null,
                                    isActive = isActive,
                                    widgets = widgets,
                                    routines = routines
                                ))
                            },
                            enabled = name.isNotBlank()
                        ) {
                            Text("Save Pack")
                        }
                    }
                }
            }
        ) { padding ->
            Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Left side: Metadata
                val leftScrollState = rememberScrollState()
                val focusManager = LocalFocusManager.current
                
                Column(
                    modifier = Modifier.weight(1f).padding(16.dp).verticalScroll(leftScrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val commonKeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    val commonKeyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    )

                    fun Modifier.tabFocus(): Modifier = this.onPreviewKeyEvent {
                        if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                            focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                            true
                        } else {
                            false
                        }
                    }
                    
                    Text("Metadata", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = name, 
                        onValueChange = { name = it }, 
                        label = { Text("Pack Name") }, 
                        modifier = Modifier.fillMaxWidth().tabFocus(), 
                        keyboardOptions = commonKeyboardOptions,
                        keyboardActions = commonKeyboardActions
                    )
                    OutlinedTextField(
                        value = author, 
                        onValueChange = { author = it }, 
                        label = { Text("Author") }, 
                        modifier = Modifier.fillMaxWidth().tabFocus(), 
                        keyboardOptions = commonKeyboardOptions,
                        keyboardActions = commonKeyboardActions
                    )
                    OutlinedTextField(
                        value = version, 
                        onValueChange = { version = it }, 
                        label = { Text("Version") }, 
                        modifier = Modifier.fillMaxWidth().tabFocus(), 
                        keyboardOptions = commonKeyboardOptions,
                        keyboardActions = commonKeyboardActions
                    )
                    
                    AppTooltipArea(
                        tooltipText = if (isActive) 
                            "ENABLED: This pack will automatically activate on your phone when its target window is focused." 
                            else "DISABLED: This pack will never auto-switch, even if its target window is open."
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Active Pack (Auto-switch)")
                            Spacer(Modifier.weight(1f))
                            Switch(checked = isActive, onCheckedChange = { isActive = it })
                        }
                    }
                    
                    if (isActive) {
                        Text("Switch Trigger", style = MaterialTheme.typography.titleSmall)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = switchMode == "PROCESS", onClick = { switchMode = "PROCESS" })
                            Text("By Process Name")
                            Spacer(Modifier.width(16.dp))
                            RadioButton(selected = switchMode == "TITLE", onClick = { switchMode = "TITLE" })
                            Text("By Window Title")
                        }

                        if (switchMode == "PROCESS") {
                            OutlinedTextField(
                                value = targetProcess,
                                onValueChange = { targetProcess = it },
                                label = { Text("Process Name") },
                                modifier = Modifier.fillMaxWidth().tabFocus(),
                                placeholder = { Text("e.g. photoshop.exe") },
                                keyboardOptions = commonKeyboardOptions,
                                keyboardActions = commonKeyboardActions
                            )
                        } else {
                            OutlinedTextField(
                                value = targetWindowTitle,
                                onValueChange = { targetWindowTitle = it },
                                label = { Text("Window Title (Partial)") },
                                modifier = Modifier.fillMaxWidth().tabFocus(),
                                placeholder = { Text("e.g. Google Chrome") },
                                keyboardOptions = commonKeyboardOptions,
                                keyboardActions = commonKeyboardActions
                            )
                        }
                    }
                }

                VerticalDivider()

                // Right side: Widgets and Routines
                Column(
                    modifier = Modifier.weight(1f).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Widgets Section
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Widgets", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = {
                                println("[PackEditor] Add Widget clicked.")
                                widgetToEdit = null
                                showWidgetEditor = true
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Widget")
                            }
                        }

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(widgets) { widget ->
                                ListItem(
                                    headlineContent = { Text(widget.label) },
                                    supportingContent = { Text("Macro: ${availableMacros.find { it.id == widget.macroId }?.name ?: "Unknown"}") },
                                    trailingContent = {
                                        Row {
                                            IconButton(onClick = {
                                                println("[PackEditor] Edit Widget clicked for: ${widget.id}")
                                                widgetToEdit = widget
                                                showWidgetEditor = true
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                                            }
                                            IconButton(onClick = {
                                                widgets = widgets.filter { it.id != widget.id }
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                                            }
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }

                    HorizontalDivider()

                    // Routines Section
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Advanced Routines", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = {
                                println("[PackEditor] Add Routine clicked.")
                                routineToEdit = null
                                showRoutineEditor = true
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Routine")
                            }
                        }

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(routines) { routine ->
                                ListItem(
                                    headlineContent = { Text(routine.name) },
                                    supportingContent = { Text("${routine.triggers.size} Trigger(s)") },
                                    trailingContent = {
                                        Row {
                                            IconButton(onClick = {
                                                println("[PackEditor] Edit Routine clicked for: ${routine.id}")
                                                routineToEdit = routine
                                                showRoutineEditor = true
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                                            }
                                            IconButton(onClick = {
                                                routines = routines.filter { it.id != routine.id }
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                                            }
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }

        if (showWidgetEditor) {
            println("[PackEditor] Launching WidgetEditorDialog.")
            WidgetEditorDialog(
                initialWidget = widgetToEdit,
                availableMacros = availableMacros,
                onDismissRequest = { showWidgetEditor = false },
                onSave = { newWidget ->
                    widgets = if (widgetToEdit == null) {
                        widgets + newWidget
                    } else {
                        widgets.map { if (it.id == newWidget.id) newWidget else it }
                    }
                    showWidgetEditor = false
                }
            )
        }

        if (showRoutineEditor) {
            println("[PackEditor] Launching RoutineEditorDialog.")
            RoutineEditorDialog(
                initialRoutine = routineToEdit,
                macroManagerViewModel = macroManagerViewModel,
                onDismissRequest = { showRoutineEditor = false },
                onSave = { updatedRoutine ->
                    routines = if (routineToEdit == null) {
                        routines + updatedRoutine
                    } else {
                        routines.map { if (it.id == updatedRoutine.id) updatedRoutine else it }
                    }
                    showRoutineEditor = false
                }
            )
        }
    }
}
