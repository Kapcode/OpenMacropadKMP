package switchdektoptocompose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import switchdektoptocompose.model.ClientInfo
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme

@Composable
fun PairingRequestDialog(
    request: ClientInfo,
    selectedTheme: String,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    val dialogState = rememberDialogState(width = 400.dp, height = 250.dp)

    DialogWindow(
        onCloseRequest = { /* Must act on the dialog */ },
        state = dialogState,
        title = "Pairing Request",
        resizable = false,
        alwaysOnTop = true
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
                        TextButton(onClick = onDeny) {
                            Text("Deny")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onApprove) {
                            Text("Always Allow")
                        }
                    }
                }
            }
        }
    }
}
