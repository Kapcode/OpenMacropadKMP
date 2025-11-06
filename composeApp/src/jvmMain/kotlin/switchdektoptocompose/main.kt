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
import org.jetbrains.compose.splitpane.rememberSplitPaneState

fun main() = application {
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
    val viewModel = remember { DesktopViewModel() }

    DisposableEffect(Unit) {
        viewModel.startServer()
        onDispose {
            viewModel.shutdown()
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Open Macropad (Compose)"
    ) {
        DesktopApp(viewModel)
    }
}

@Composable
@Preview
fun DesktopApp(viewModel: DesktopViewModel) {
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val isServerRunning by viewModel.isServerRunning.collectAsState()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            // Corrected the parameter name from 'initial' to 'initialPositionPercentage'
            val splitterState = rememberSplitPaneState(initialPositionPercentage = 0.5f)

            HorizontalSplitPane(splitPaneState = splitterState) {
                // First pane (left side)
                first(minSize = 300.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF3C3F41)) // Lighter grey
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(Color(0xFF45494A))
                                .padding(8.dp)
                        ) {
                            Text("Server Status: ${if (isServerRunning) "Running" else "Stopped"}", color = Color.White)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(8.dp)
                        ) {
                            ConnectedDevicesScreen(devices = connectedDevices)
                        }
                    }
                }

                // Second pane (right side)
                second(minSize = 300.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2B2B2B)) // Dark grey
                            .padding(8.dp)
                    ) {
                        Text("Macro Manager Area", color = Color.White)
                    }
                }
            }
        }
    }
}