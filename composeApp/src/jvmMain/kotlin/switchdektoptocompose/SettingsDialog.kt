package switchdektoptocompose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsDialog(
    desktopViewModel: DesktopViewModel,
    settingsViewModel: SettingsViewModel,
    onDismissRequest: () -> Unit
) {
    val serverPort by settingsViewModel.serverPort.collectAsState()
    val secureServerPort by settingsViewModel.secureServerPort.collectAsState()
    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()
    val encryptionEnabled by desktopViewModel.encryptionEnabled.collectAsState()
    val isServerRunning by desktopViewModel.isServerRunning.collectAsState()

    val dialogState = rememberDialogState(width = 600.dp, height = 450.dp)

    DialogWindow(
        onCloseRequest = onDismissRequest,
        state = dialogState,
        title = "Settings",
        resizable = false
    ) {
        AppTheme(useDarkTheme = selectedTheme == "Dark Blue") {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()) // Make content scrollable
                ) {
                    // --- Theme Selection ---
                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                    Column(Modifier.selectableGroup()) {
                        settingsViewModel.availableThemes.forEach { theme ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .selectable(
                                        selected = (theme == selectedTheme),
                                        onClick = { settingsViewModel.selectTheme(theme) },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (theme == selectedTheme),
                                    onClick = null // null recommended for accessibility with screenreaders
                                )
                                Text(
                                    text = theme,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // --- Server Ports ---
                    Text("Network", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = serverPort.toString(),
                            onValueChange = { settingsViewModel.onServerPortChange(it) },
                            label = { Text("Server Port (WS)") },
                            modifier = Modifier.weight(1f),
                            enabled = !isServerRunning
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedTextField(
                            value = secureServerPort.toString(),
                            onValueChange = { settingsViewModel.onSecureServerPortChange(it) },
                            label = { Text("Secure Server Port (WSS)") },
                            modifier = Modifier.weight(1f),
                            enabled = !isServerRunning
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Encryption Setting ---
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enable Encryption (WSS)", modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = encryptionEnabled,
                            onCheckedChange = { desktopViewModel.setEncryption(it) },
                            enabled = !isServerRunning
                        )
                    }
                    Text(
                        text = "Requires a restart of the server to apply.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )

                    Spacer(Modifier.weight(1f)) // Pushes the close button to the bottom

                    // --- Close Button ---
                    Box(modifier = Modifier.fillMaxWidth()) {
                        TooltipArea(
                            tooltip = { Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp){ Text("Close", modifier = Modifier.padding(4.dp)) } },
                            modifier = Modifier.align(Alignment.BottomEnd),
                            delayMillis = 0
                        ) {
                            IconButton(onClick = onDismissRequest) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    }
                }
            }
        }
    }
}