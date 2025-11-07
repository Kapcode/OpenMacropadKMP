package switchdektoptocompose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    desktopViewModel: DesktopViewModel, // Use the main ViewModel
    settingsViewModel: SettingsViewModel,
    onDismissRequest: () -> Unit
) {
    val macroDirectory by settingsViewModel.macroDirectory.collectAsState()
    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()
    val encryptionEnabled by desktopViewModel.encryptionEnabled.collectAsState()
    val isServerRunning by desktopViewModel.isServerRunning.collectAsState()

    val dialogState = rememberDialogState(width = 600.dp, height = 400.dp)

    DialogWindow(
        onCloseRequest = onDismissRequest,
        state = dialogState,
        title = "Settings",
        resizable = false
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ... (Other settings remain the same)

                // --- Encryption Setting ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable Encryption (WSS)", modifier = Modifier.weight(1f))
                    Checkbox(
                        checked = encryptionEnabled,
                        onCheckedChange = { desktopViewModel.setEncryption(it) },
                        enabled = !isServerRunning // Disable checkbox if server is running
                    )
                }
                 Text(
                    text = "Requires a restart of the server to apply. Disables pairing.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )


                Spacer(Modifier.weight(1f)) // Pushes the close button to the bottom
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}