package switchdektoptocompose

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
        DesktopApp(desktopViewModel, macroEditorViewModel, macroManagerViewModel)
    }
}

@Composable
@Preview
fun DesktopApp(
    desktopViewModel: DesktopViewModel,
    macroEditorViewModel: MacroEditorViewModel,
    macroManagerViewModel: MacroManagerViewModel
) {
    val connectedDevices by desktopViewModel.connectedDevices.collectAsState()
    val isServerRunning by desktopViewModel.isServerRunning.collectAsState()
    val serverIpAddress by desktopViewModel.serverIpAddress.collectAsState()
    val serverPort by desktopViewModel.serverPort.collectAsState()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val rootVerticalSplitter = rememberSplitPaneState(initialPositionPercentage = 0.2f)
            // Set the main horizontal splitter to a 10/90 split
            val mainHorizontalSplitter = rememberSplitPaneState(initialPositionPercentage = 0.1f)

            VerticalSplitPane(splitPaneState = rootVerticalSplitter) {
                // --- Top Pane (Server Status & Inspector) ---
                first(minSize = 100.dp) {
                    Row(modifier = Modifier.fillMaxSize().background(Color(0xFF3C3F41))) {
                        Box(
                            modifier = Modifier.weight(0.9f).fillMaxHeight().padding(8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text("Status: ${if (isServerRunning) "Running" else "Stopped"}", color = if (isServerRunning) Color.Green else Color.Red)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Address: $serverIpAddress:$serverPort", color = Color.White)
                            }
                        }
                        Box(modifier = Modifier.weight(0.1f).fillMaxHeight()) {
                            InspectorScreen()
                        }
                    }
                }
                // --- Bottom Pane (Main Content) ---
                second(minSize = 200.dp) {
                    HorizontalSplitPane(splitPaneState = mainHorizontalSplitter) {
                        // Left side of bottom pane (10% width)
                        first(minSize = 250.dp) {
                           Column {
                               Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
                                   ConnectedDevicesScreen(devices = connectedDevices)
                               }
                               Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.DarkGray).padding(8.dp)) {
                                   Text("Console Area", color = Color.White)
                               }
                           }
                        }
                        // Right side of bottom pane (90% width)
                        second(minSize = 500.dp) {
                            MacroEditingArea(
                                macroManagerViewModel = macroManagerViewModel,
                                macroEditorViewModel = macroEditorViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}