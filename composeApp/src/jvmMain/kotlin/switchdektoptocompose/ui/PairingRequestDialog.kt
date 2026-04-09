package switchdektoptocompose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import switchdektoptocompose.model.ClientInfo
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme

@Composable
fun PairingRequestDialog(
    request: ClientInfo,
    selectedTheme: String,
    isAlwaysAllowAvailable: Boolean = true,
    onApprove: (Boolean) -> Unit,
    onDeny: () -> Unit,
    onBan: () -> Unit
) {
    val windowState = rememberWindowState(width = 550.dp, height = 300.dp)

    Window(
        onCloseRequest = { /* Must act on the dialog */ },
        state = windowState,
        title = "Pairing Request",
        resizable = false,
        alwaysOnTop = true,
        focusable = true
    ) {
        AppTheme(useDarkTheme = selectedTheme == "Dark Blue") {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Allow '${request.name}' to control this PC?",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Device ID: ${request.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onBan,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Ban")
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onDeny) {
                            Text("Deny")
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { onApprove(false) }) {
                            Text("Allow Once")
                        }
                        if (isAlwaysAllowAvailable) {
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { onApprove(true) }) {
                                Text("Always Allow")
                            }
                        } else {
                            // Optionally add a note why Always Allow is missing
                        }
                    }
                }
            }
        }
    }
}
