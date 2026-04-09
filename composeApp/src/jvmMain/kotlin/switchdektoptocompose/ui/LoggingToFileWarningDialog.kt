package switchdektoptocompose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme

@Composable
fun LoggingToFileWarningDialog(
    selectedTheme: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    DialogWindow(
        onCloseRequest = onDismiss,
        state = rememberDialogState(width = 500.dp, height = 400.dp),
        title = "Security and Performance Warning",
        resizable = false,
        alwaysOnTop = true
    ) {
        AppTheme(useDarkTheme = selectedTheme == "Dark Blue") {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Enabling 'Log to File' will record all activities, including key presses and mouse events, to a local file.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("IMPLICATIONS:", style = MaterialTheme.typography.labelLarge)
                        Text("• SECURITY: Sensitive information like passwords or private messages will be stored in plain text on your disk.")
                        Text("• HARDWARE: Continuous writing to disk (especially SSDs) can contribute to hardware wear over long periods.")
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "This setting should only be used TEMPORARILY for debugging. Do not leave it on.",
                            color = MaterialTheme.colorScheme.error,
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
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("I Understand, Enable Temporarily")
                        }
                    }
                }
            }
        }
    }
}
