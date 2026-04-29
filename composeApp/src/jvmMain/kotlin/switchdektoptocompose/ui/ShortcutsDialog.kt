package switchdektoptocompose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import switchdektoptocompose.viewmodel.SettingsViewModel
import switchdektoptocompose.viewmodel.ConsoleViewModel

enum class ShortcutType { KEY, SHORTCUT }

@Composable
fun ShortcutsDialog(
    settingsViewModel: SettingsViewModel,
    consoleViewModel: ConsoleViewModel,
    selectedTheme: String,
    onDismissRequest: () -> Unit,
    windowState: WindowState? = null
) {
    val eStopKey by settingsViewModel.eStopKey.collectAsState()
    val hardEstop by settingsViewModel.hardEstop.collectAsState()
    val copyConsoleOutputShortcut by settingsViewModel.copyConsoleOutputShortcut.collectAsState()
    val stopKeyShortcut by settingsViewModel.stopKeyShortcut.collectAsState()
    val inspectKeyShortcut by settingsViewModel.inspectKeyShortcut.collectAsState()
    val splitterSwapModifier by settingsViewModel.splitterSwapModifier.collectAsState()
    val splitterInfoModifier by settingsViewModel.splitterInfoModifier.collectAsState()

    var recordingShortcutFor by remember { mutableStateOf<String?>(null) }
    var recordingShortcutType by remember { mutableStateOf<ShortcutType>(ShortcutType.KEY) }

    if (recordingShortcutFor != null) {
        RecordShortcutDialog(
            title = "Record Shortcut for $recordingShortcutFor",
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            isFullShortcut = recordingShortcutType == ShortcutType.SHORTCUT,
            onShortcutRecorded = { shortcut ->
                when (recordingShortcutFor) {
                    "Hard E-Stop" -> settingsViewModel.setEStopKey(shortcut)
                    "Copy Console Output" -> settingsViewModel.setCopyConsoleOutputShortcut(shortcut)
                    "Stop Macro" -> settingsViewModel.setStopKeyShortcut(shortcut)
                    "Inspect Key" -> settingsViewModel.setInspectKeyShortcut(shortcut)
                }
                recordingShortcutFor = null
            },
            onDismissRequest = { recordingShortcutFor = null }
        )
    }

    AppDialog(
        onCloseRequest = onDismissRequest,
        state = windowState ?: rememberWindowState(width = 500.dp, height = 550.dp),
        title = "Shortcuts & Keymap",
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Global Shortcuts", style = MaterialTheme.typography.titleLarge)
            
            // Hard E-Stop
            Card {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Hard E-Stop", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = eStopKey,
                            onValueChange = { settingsViewModel.setEStopKey(it) },
                            modifier = Modifier.width(120.dp),
                            readOnly = true,
                            singleLine = true
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { 
                            recordingShortcutFor = "Hard E-Stop"
                            recordingShortcutType = ShortcutType.KEY
                        }) {
                            Text("Record")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hardEstop, onCheckedChange = { settingsViewModel.setHardEstop(it) })
                        Text("Hard E-Stop (Disable all macro execution)")
                    }
                }
            }

            Text("Application Shortcuts", style = MaterialTheme.typography.titleLarge)

            // Copy Console
            Card {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Copy Console Output", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = copyConsoleOutputShortcut,
                        onValueChange = { settingsViewModel.setCopyConsoleOutputShortcut(it) },
                        modifier = Modifier.width(150.dp),
                        readOnly = true,
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { 
                        recordingShortcutFor = "Copy Console Output"
                        recordingShortcutType = ShortcutType.SHORTCUT
                    }) {
                        Text("Record")
                    }
                }
            }

            // Stop Macro
            Card {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Stop Currently Executing Macro", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = stopKeyShortcut,
                        onValueChange = { settingsViewModel.setStopKeyShortcut(it) },
                        modifier = Modifier.width(150.dp),
                        readOnly = true,
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { 
                        recordingShortcutFor = "Stop Macro"
                        recordingShortcutType = ShortcutType.SHORTCUT
                    }) {
                        Text("Record")
                    }
                }
            }

            // Inspect Key
            Card {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Global Key Inspector Toggle", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = inspectKeyShortcut,
                        onValueChange = { settingsViewModel.setInspectKeyShortcut(it) },
                        modifier = Modifier.width(150.dp),
                        readOnly = true,
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { 
                        recordingShortcutFor = "Inspect Key"
                        recordingShortcutType = ShortcutType.SHORTCUT
                    }) {
                        Text("Record")
                    }
                }
            }

            Text("Splitter Modifiers", style = MaterialTheme.typography.titleLarge)

            // Splitter Swap Modifier
            Card {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Splitter Swap Modifier", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(splitterSwapModifier)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("Shift", "Ctrl", "Alt").forEach { modifier ->
                                DropdownMenuItem(
                                    text = { Text(modifier) },
                                    onClick = {
                                        settingsViewModel.setSplitterSwapModifier(modifier)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Splitter Info Modifier
            Card {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Splitter Info Modifier (Print to Console)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(splitterInfoModifier)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("Shift", "Ctrl", "Alt").forEach { modifier ->
                                DropdownMenuItem(
                                    text = { Text(modifier) },
                                    onClick = {
                                        settingsViewModel.setSplitterInfoModifier(modifier)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onDismissRequest,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Close")
            }
        }
    }
}
