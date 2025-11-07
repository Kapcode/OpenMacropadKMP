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
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState

fun main() = application {
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
    val desktopViewModel = remember { DesktopViewModel() }
    val macroEditorViewModel = remember { MacroEditorViewModel() }
    val macroManagerViewModel = remember { MacroManagerViewModel() }
    val settingsViewModel = remember { SettingsViewModel() }
    val macroTimelineViewModel = remember { MacroTimelineViewModel(macroEditorViewModel) }

    DisposableEffect(Unit) {
        desktopViewModel.startServer()
        onDispose {
            desktopViewModel.shutdown()
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Open Macropad (Compose)"
    ) {
        DesktopApp(
            desktopViewModel = desktopViewModel,
            macroEditorViewModel = macroEditorViewModel,
            macroManagerViewModel = macroManagerViewModel,
            settingsViewModel = settingsViewModel,
            macroTimelineViewModel = macroTimelineViewModel,
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
    onExit: () -> Unit = {}
) {
    val connectedDevices by desktopViewModel.connectedDevices.collectAsState()
    val isServerRunning by desktopViewModel.isServerRunning.collectAsState()
    val serverIpAddress by desktopViewModel.serverIpAddress.collectAsState()
    val serverPort by desktopViewModel.serverPort.collectAsState()
    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }

    val colorScheme = when (selectedTheme) {
        "Dark Blue" -> DarkBlueColorScheme
        "Light Blue" -> LightBlueColorScheme
        else -> DarkBlueColorScheme
    }

    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = settingsViewModel,
            onDismissRequest = { showSettingsDialog = false }
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
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(text = { Text("Start Server") }, onClick = { desktopViewModel.startServer(); menuExpanded = false })
                                DropdownMenuItem(text = { Text("Stop Server") }, onClick = { desktopViewModel.stopServer(); menuExpanded = false })
                                Divider()
                                DropdownMenuItem(text = { Text("Settings") }, onClick = { showSettingsDialog = true; menuExpanded = false })
                                Divider()
                                DropdownMenuItem(text = { Text("Exit") }, onClick = onExit)
                            }
                        }

                        Box(
                            modifier = Modifier.weight(0.9f).fillMaxHeight().padding(8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text("Status: ${if (isServerRunning) "Running" else "Stopped"}", color = if (isServerRunning) Color.Green else Color.Red)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Address: $serverIpAddress:$serverPort", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Box(modifier = Modifier.weight(0.1f).fillMaxHeight()) {
                            InspectorScreen()
                        }
                    }
                }
                // --- Bottom Pane ---
                second(minSize = 200.dp) {
                    HorizontalSplitPane(splitPaneState = mainHorizontalSplitter) {
                        first(minSize = 250.dp) {
                           Column {
                               Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
                                   ConnectedDevicesScreen(devices = connectedDevices)
                               }
                               Box(modifier = Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
                                   Text("Console Area", color = MaterialTheme.colorScheme.onSurfaceVariant)
                               }
                           }
                        }
                        second(minSize = 500.dp) {
                            MacroEditingArea(
                                macroManagerViewModel = macroManagerViewModel,
                                macroEditorViewModel = macroEditorViewModel,
                                macroTimelineViewModel = macroTimelineViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}