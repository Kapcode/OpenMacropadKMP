package switchdektoptocompose

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState

fun main() = application {
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)

    val settingsViewModel = remember { SettingsViewModel() }
    val newEventViewModel = remember { NewEventViewModel() }

    val desktopViewModel = remember { DesktopViewModel(settingsViewModel) }
    lateinit var macroManagerViewModel: MacroManagerViewModel

    val macroEditorViewModel = remember {
        MacroEditorViewModel(settingsViewModel) {
            macroManagerViewModel.refresh()
        }
    }

    macroManagerViewModel = remember {
        MacroManagerViewModel(
            settingsViewModel = settingsViewModel,
            onEditMacroRequested = { macroState ->
                macroEditorViewModel.openOrSwitchToTab(macroState)
            },
            onMacrosUpdated = {
                desktopViewModel.sendMacroListToAllClients()
            }
        )
    }

    desktopViewModel.macroManagerViewModel = macroManagerViewModel

    val macroTimelineViewModel = remember { MacroTimelineViewModel(macroEditorViewModel) }

    val triggerListener = remember {
        TriggerListener(desktopViewModel) { macroToPlay ->
            macroManagerViewModel.onPlayMacro(macroToPlay)
        }
    }

    DisposableEffect(Unit) {
        desktopViewModel.startServer()
        triggerListener.startListening()
        onDispose {
            desktopViewModel.shutdown()
            triggerListener.shutdown()
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Open Macropad (Compose)"
    ) {
        val macroFiles by macroManagerViewModel.macroFiles.collectAsState()

        LaunchedEffect(macroFiles) {
            triggerListener.updateActiveTriggers(macroFiles)
        }

        DesktopApp(
            desktopViewModel = desktopViewModel,
            macroEditorViewModel = macroEditorViewModel,
            macroManagerViewModel = macroManagerViewModel,
            settingsViewModel = settingsViewModel,
            macroTimelineViewModel = macroTimelineViewModel,
            newEventViewModel = newEventViewModel,
            onExit = ::exitApplication
        )
    }
}

@Composable
@Preview
fun DesktopApp(
    desktopViewModel: DesktopViewModel,
    macroEditorViewModel: MacroEditorViewModel,
    macroManagerViewModel: MacroManagerViewModel,
    settingsViewModel: SettingsViewModel,
    macroTimelineViewModel: MacroTimelineViewModel,
    newEventViewModel: NewEventViewModel,
    onExit: () -> Unit = {}
) {
    val connectedDevices by desktopViewModel.connectedDevices.collectAsState()
    val isServerRunning by desktopViewModel.isServerRunning.collectAsState()
    val serverIpAddress by desktopViewModel.serverIpAddress.collectAsState()
    val encryptionEnabled by desktopViewModel.encryptionEnabled.collectAsState()
    val isMacroExecutionEnabled by desktopViewModel.isMacroExecutionEnabled.collectAsState()
    val serverPort by settingsViewModel.serverPort.collectAsState()
    val secureServerPort by settingsViewModel.secureServerPort.collectAsState()
    val currentPort = if (encryptionEnabled) secureServerPort else serverPort
    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()
    val filePendingDeletion by macroManagerViewModel.filePendingDeletion.collectAsState()
    val filesPendingDeletion by macroManagerViewModel.filesPendingDeletion.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNewEventDialog by remember { mutableStateOf(false) }

    val colorScheme = when (selectedTheme) {
        "Dark Blue" -> DarkBlueColorScheme
        "Light Blue" -> LightBlueColorScheme
        else -> DarkBlueColorScheme
    }

    // --- Dialogs ---
    if (showSettingsDialog) {
        SettingsDialog(
            desktopViewModel = desktopViewModel,
            settingsViewModel = settingsViewModel,
            onDismissRequest = { showSettingsDialog = false }
        )
    }
    filePendingDeletion?.let { file ->
        ConfirmDeleteDialog(file = file, onConfirm = { macroManagerViewModel.confirmDeletion() }, onDismiss = { macroManagerViewModel.cancelDeletion() })
    }
    filesPendingDeletion?.let { files ->
        ConfirmDeleteMultipleDialog(files = files, onConfirm = { macroManagerViewModel.confirmMultipleDeletion() }, onDismiss = { macroManagerViewModel.cancelMultipleDeletion() })
    }
    if (showNewEventDialog) {
        NewEventDialog(
            viewModel = newEventViewModel,
            onDismissRequest = { showNewEventDialog = false },
            onAddEvent = {
                if (newEventViewModel.isTriggerEvent.value) {
                    macroTimelineViewModel.addOrUpdateTrigger(
                        keyName = newEventViewModel.keysText.value,
                        allowedClients = newEventViewModel.allowedClientsText.value
                    )
                } else {
                    val events = newEventViewModel.createEvents()
                    macroTimelineViewModel.addEvents(events)
                }
                showNewEventDialog = false
            }
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val rootVerticalSplitter = rememberSplitPaneState(initialPositionPercentage = 0.2f)
            val mainHorizontalSplitter = rememberSplitPaneState(initialPositionPercentage = 0.1f)

            VerticalSplitPane(splitPaneState = rootVerticalSplitter) {
                // --- Top Pane ---
                first(minSize = 100.dp) {
                    Row(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var menuExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.padding(start = 4.dp)) {
                            TextButton(onClick = { menuExpanded = true }) {
                                Text("Menu", color = MaterialTheme.colorScheme.onSurface)
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(text = { Text("Start Server") }, onClick = { desktopViewModel.startServer(); menuExpanded = false })
                                DropdownMenuItem(text = { Text("Stop Server") }, onClick = { desktopViewModel.stopServer(); menuExpanded = false })
                                Divider()
                                DropdownMenuItem(text = { Text("Settings") }, onClick = { showSettingsDialog = true; menuExpanded = false })
                                Divider()
                                DropdownMenuItem(text = { Text("Exit") }, onClick = onExit)
                            }
                        }
                        // --- Server Status ---
                        Box(
                            modifier = Modifier.weight(0.9f).fillMaxHeight().padding(8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text("Status: ${if (isServerRunning) "Running" else "Stopped"}", color = if (isServerRunning) Color.Green else Color.Red)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Address: $serverIpAddress:$currentPort", color = MaterialTheme.colorScheme.onSurface)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Macros Enabled:", color = MaterialTheme.colorScheme.onSurface)
                                    Switch(
                                        checked = isMacroExecutionEnabled,
                                        onCheckedChange = { desktopViewModel.setMacroExecutionEnabled(it) }
                                    )
                                }
                            }
                        }
                        // --- Inspector ---
                        Box(modifier = Modifier.weight(0.1f).fillMaxHeight()) {
                            InspectorScreen()
                        }
                    }
                }
                // --- Bottom Pane ---
                second(minSize = 200.dp) {
                    HorizontalSplitPane(splitPaneState = mainHorizontalSplitter) {
                        // --- Left Side ---
                        first(minSize = 250.dp) {
                           Column {
                               // --- Connected Devices ---
                               Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
                                   ConnectedDevicesScreen(devices = connectedDevices)
                               }
                               // --- Console ---
                               Box(modifier = Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
                                   Text("Console Area", color = MaterialTheme.colorScheme.onSurfaceVariant)
                               }
                           }
                        }
                        // --- Right Side (Contains Timeline) ---
                        second(minSize = 500.dp) {
                            MacroEditingArea(
                                macroManagerViewModel = macroManagerViewModel,
                                macroEditorViewModel = macroEditorViewModel,
                                macroTimelineViewModel = macroTimelineViewModel,
                                onAddEventClicked = {
                                    newEventViewModel.reset()
                                    showNewEventDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}