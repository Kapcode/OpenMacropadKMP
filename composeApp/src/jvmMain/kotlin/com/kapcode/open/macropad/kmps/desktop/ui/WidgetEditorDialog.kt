package com.kapcode.open.macropad.kmps.desktop.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.rememberWindowState
import com.kapcode.open.macropad.kmps.desktop.ui.components.RedrawFix
import com.kapcode.open.macropad.kmps.desktop.ui.AppDialog
import com.kapcode.open.macropad.kmps.desktop.viewmodel.ConsoleViewModel
import com.kapcode.open.macropad.kmps.models.GridWidget
import com.kapcode.open.macropad.kmps.models.WidgetType
import com.kapcode.open.macropad.kmps.desktop.model.MacroFileState
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetEditorDialog(
    initialWidget: GridWidget?,
    availableMacros: List<MacroFileState>,
    onDismissRequest: () -> Unit,
    onSave: (GridWidget) -> Unit,
    selectedTheme: String,
    consoleViewModel: ConsoleViewModel,
    icon: androidx.compose.ui.graphics.painter.Painter? = null
) {
    var macroId by remember { mutableStateOf(initialWidget?.macroId ?: "") }
    var label by remember { mutableStateOf(initialWidget?.label ?: "") }
    var color by remember { mutableStateOf(initialWidget?.color ?: 0xFFBB86FC) }
    var iconName by remember { mutableStateOf(initialWidget?.icon ?: "") }
    var row by remember { mutableStateOf(initialWidget?.row ?: 0) }
    var col by remember { mutableStateOf(initialWidget?.col ?: 0) }
    var type by remember { mutableStateOf(initialWidget?.type ?: WidgetType.BUTTON) }

    var showMacroPicker by remember { mutableStateOf(false) }

    AppDialog(
        onCloseRequest = onDismissRequest,
        state = rememberWindowState(width = 500.dp, height = 700.dp),
        title = if (initialWidget == null) "Add Widget" else "Edit Widget",
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel,
        icon = icon
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (initialWidget == null) "Add Widget" else "Edit Widget") },
                    actions = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
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
                                val widget = GridWidget(
                                    id = initialWidget?.id ?: UUID.randomUUID().toString(),
                                    macroId = macroId,
                                    label = label,
                                    color = color,
                                    icon = if (iconName.isBlank()) null else iconName,
                                    row = row,
                                    col = col,
                                    type = type
                                )
                                onSave(widget)
                            },
                            enabled = macroId.isNotBlank() && label.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Macro Selection
                OutlinedTextField(
                    value = availableMacros.find { it.id == macroId }?.name ?: "Select Macro",
                    onValueChange = {},
                    label = { Text("Macro") },
                    modifier = Modifier.fillMaxWidth().clickable { showMacroPicker = true },
                    enabled = false,
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Simple Color Selection (Hex)
                OutlinedTextField(
                    value = "0x" + color.toString(16).uppercase(),
                    onValueChange = {
                        it.removePrefix("0x").toLongOrNull(16)?.let { newColor ->
                            color = newColor
                        }
                    },
                    label = { Text("Color (Hex)") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Box(Modifier.size(24.dp).background(Color(color)))
                    }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = row.toString(),
                        onValueChange = { it.toIntOrNull()?.let { row = it } },
                        label = { Text("Row") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = col.toString(),
                        onValueChange = { it.toIntOrNull()?.let { col = it } },
                        label = { Text("Column") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Widget Type
                Column {
                    Text("Widget Type", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WidgetType.values().forEach { widgetType ->
                            FilterChip(
                                selected = type == widgetType,
                                onClick = { type = widgetType },
                                label = { Text(widgetType.name) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMacroPicker) {
        AppDialog(
            onCloseRequest = { showMacroPicker = false },
            title = "Select Macro",
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            icon = icon,
            state = rememberWindowState(width = 400.dp, height = 500.dp)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                LazyColumn {
                    items(availableMacros) { macro ->
                        ListItem(
                            headlineContent = { Text(macro.name) },
                            modifier = Modifier.clickable {
                                macroId = macro.id
                                if (label.isBlank()) label = macro.name
                                showMacroPicker = false
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
