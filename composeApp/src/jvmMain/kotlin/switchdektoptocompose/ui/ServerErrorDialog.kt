package switchdektoptocompose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import switchdektoptocompose.model.LogLevel
import switchdektoptocompose.viewmodel.ConsoleViewModel

@Composable
fun ServerErrorDialog(
    error: String,
    selectedTheme: String,
    consoleViewModel: ConsoleViewModel,
    onResetIdentity: () -> Unit,
    onDismiss: () -> Unit
) {
    var confirmText by remember { mutableStateOf("") }
    val isConfirmEnabled = confirmText.equals("RESET", ignoreCase = false)

    AppDialog(
        onCloseRequest = onDismiss,
        state = rememberWindowState(width = 450.dp, height = 320.dp),
        title = "Server Identity Error",
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                Spacer(Modifier.height(16.dp))
                Text(
                    "Resetting your identity will unpair all currently connected devices. This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = confirmText,
                onValueChange = { confirmText = it },
                label = { Text("Type 'RESET' to confirm") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = confirmText.isNotEmpty() && !isConfirmEnabled
            )

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
                    onClick = {
                        if (isConfirmEnabled) {
                            onResetIdentity()
                        } else {
                            consoleViewModel.addLog(LogLevel.Warn, "Identity reset blocked: confirmation text mismatch")
                        }
                    },
                    enabled = isConfirmEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Reset Identity")
                }
            }
        }
    }
}
