package com.kapcode.open.macropad.kmps.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import com.kapcode.open.macropad.kmps.desktop.ui.components.RedrawFix
import com.kapcode.open.macropad.kmps.desktop.ui.AppDialog
import com.kapcode.open.macropad.kmps.models.*
import com.kapcode.open.macropad.kmps.ui.components.VisualRoutineBuilder
import com.kapcode.open.macropad.kmps.desktop.logic.KeyParser
import com.kapcode.open.macropad.kmps.desktop.viewmodel.ConsoleViewModel
import com.kapcode.open.macropad.kmps.desktop.viewmodel.MacroManagerViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorDialog(
    initialRoutine: AutomationRoutine?,
    macroManagerViewModel: MacroManagerViewModel,
    onDismissRequest: () -> Unit,
    onSave: (AutomationRoutine) -> Unit,
    selectedTheme: String,
    consoleViewModel: ConsoleViewModel,
    icon: androidx.compose.ui.graphics.painter.Painter? = null
) {
    var routine by remember { 
        mutableStateOf(initialRoutine ?: AutomationRoutine(
            id = UUID.randomUUID().toString(),
            name = "New Routine",
            triggers = listOf(AutomationTrigger.KeyHold("F13", "500"))
        )) 
    }

    AppDialog(
        onCloseRequest = onDismissRequest,
        state = rememberWindowState(width = 600.dp, height = 700.dp),
        title = if (initialRoutine == null) "New Routine" else "Edit Routine: ${routine.name}",
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel,
        icon = icon,
        resizable = true
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (initialRoutine == null) "New Routine" else "Edit Routine") },
                    actions = {
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
                            onClick = { onSave(routine) },
                            enabled = routine.name.isNotBlank()
                        ) {
                            Text("Save Routine")
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                VisualRoutineBuilder(
                    routine = routine,
                    onRoutineChange = { routine = it },
                    getLiveValue = { macroManagerViewModel.getSystemVariable(it) },
                    isValidKey = { KeyParser.isValidKey(it) },
                    getKeySuggestions = { input ->
                        val query = input.split('+', ',').lastOrNull()?.trim()?.uppercase() ?: ""
                        if (query.isEmpty()) emptyList()
                        else KeyParser.getAllValidKeyNames().filter { it.startsWith(query) }.take(5)
                    }
                )
            }
        }
    }
}
