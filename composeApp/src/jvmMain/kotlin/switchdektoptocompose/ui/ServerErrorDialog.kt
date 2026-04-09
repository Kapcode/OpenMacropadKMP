package switchdektoptocompose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme

@Composable
fun ServerErrorDialog(
    error: String,
    selectedTheme: String,
    onResetIdentity: () -> Unit,
    onDismiss: () -> Unit
) {
    val windowState = rememberWindowState(width = 450.dp, height = 250.dp)

    Window(
        onCloseRequest = onDismiss,
        state = windowState,
        title = "Server Identity Error",
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
                            "Server Identity Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onResetIdentity,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Reset Identity")
                        }
                    }
                }
            }
        }
    }
}
