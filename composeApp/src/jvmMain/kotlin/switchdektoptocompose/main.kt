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
    // Instantiate both ViewModels
    val desktopViewModel = remember { DesktopViewModel() }
    val macroEditorViewModel = remember { MacroEditorViewModel() }

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
        DesktopApp(desktopViewModel, macroEditorViewModel)
    }
}

@Composable
@Preview
fun DesktopApp(desktopViewModel: DesktopViewModel, macroEditorViewModel: MacroEditorViewModel) {
    val connectedDevices by desktopViewModel.connectedDevices.collectAsState()
    val isServerRunning by desktopViewModel.isServerRunning.collectAsState()
    val serverIpAddress by desktopViewModel.serverIpAddress.collectAsState()
    val serverPort by desktopViewModel.serverPort.collectAsState()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val verticalSplitter = rememberSplitPaneState(initialPositionPercentage = 0.3f)
            val horizontalSplitter = rememberSplitPaneState(initialPositionPercentage = 0.5f)

            VerticalSplitPane(splitPaneState = verticalSplitter) {
                // --- Top Pane ---
                first(minSize = 100.dp) {
                    Row(modifier = Modifier.fillMaxSize().background(Color(0xFF3C3F41))) {
                        // Server Status (90% width)
                        Box(
                            modifier = Modifier.weight(0.9f).fillMaxHeight().padding(8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text(
                                    "Status: ${if (isServerRunning) "Running" else "Stopped"}",
                                    color = if (isServerRunning) Color.Green else Color.Red
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Address: $serverIpAddress:$serverPort",
                                    color = Color.White
                                )
                            }
                        }
                        // Inspector (10% width)
                        Box(modifier = Modifier.weight(0.1f).fillMaxHeight()) {
                            InspectorScreen()
                        }
                    }
                }
                // --- Bottom Pane ---
                second(minSize = 200.dp) {
                    HorizontalSplitPane(splitPaneState = horizontalSplitter) {
                        // Left side of bottom pane
                        first(minSize = 250.dp) {
                            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                ConnectedDevicesScreen(devices = connectedDevices)
                            }
                        }
                        // Right side of bottom pane
                        second(minSize = 300.dp) {
                             // --- INTEGRATION ---
                             // Replace the placeholder Box with the MacroEditorScreen
                             MacroEditorScreen(viewModel = macroEditorViewModel)
                             // --- END INTEGRATION ---
                        }
                    }
                }
            }
        }
    }
}