package switchdektoptocompose.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.kapcode.open.macropad.kmps.models.GridWidget
import com.kapcode.open.macropad.kmps.models.MacroPack
import switchdektoptocompose.model.MacroFileState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackEditorDialog(
    pack: MacroPack,
    availableMacros: List<MacroFileState>,
    onDismissRequest: () -> Unit,
    onSave: (MacroPack) -> Unit,
    onOpenInJsonEditor: (MacroPack) -> Unit
) {
    var name by remember { mutableStateOf(pack.name) }
    var author by remember { mutableStateOf(pack.author) }
    var version by remember { mutableStateOf(pack.version) }
    var targetProcess by remember { mutableStateOf(pack.targetProcess ?: "") }
    var isActive by remember { mutableStateOf(pack.isActive) }
    var widgets by remember { mutableStateOf(pack.widgets) }

    var widgetToEdit by remember { mutableStateOf<GridWidget?>(null) }
    var showWidgetEditor by remember { mutableStateOf(false) }

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
                        TextButton(onClick = { onOpenInJsonEditor(pack.copy(name = name, author = author, version = version, targetProcess = if (targetProcess.isBlank()) null else targetProcess, isActive = isActive, widgets = widgets)) }) {
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
                                onSave(pack.copy(
                                    name = name,
                                    author = author,
                                    version = version,
                                    targetProcess = if (targetProcess.isBlank()) null else targetProcess,
                                    isActive = isActive,
                                    widgets = widgets
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
                Column(
                    modifier = Modifier.weight(1f).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Metadata", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Pack Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Author") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = version, onValueChange = { version = it }, label = { Text("Version") }, modifier = Modifier.fillMaxWidth())
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Active Pack (Auto-switch)")
                        Spacer(Modifier.weight(1f))
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                    }
                    
                    OutlinedTextField(
                        value = targetProcess,
                        onValueChange = { targetProcess = it },
                        label = { Text("Window/Process Name") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. photoshop.exe") },
                        enabled = isActive
                    )
                }

                VerticalDivider()

                // Right side: Widgets
                Column(
                    modifier = Modifier.weight(1f).padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Widgets", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {
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
            }
        }

        if (showWidgetEditor) {
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
    }
}
