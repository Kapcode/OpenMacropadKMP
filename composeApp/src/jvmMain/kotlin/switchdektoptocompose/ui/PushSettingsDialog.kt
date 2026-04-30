package switchdektoptocompose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import switchdektoptocompose.viewmodel.SettingsViewModel
import switchdektoptocompose.viewmodel.ConsoleViewModel

@Composable
fun PushSettingsDialog(
    settingsViewModel: SettingsViewModel,
    consoleViewModel: ConsoleViewModel,
    selectedTheme: String,
    onDismissRequest: () -> Unit,
    windowState: WindowState? = null,
    icon: Painter? = null
) {
    val clientTheme by settingsViewModel.clientTheme.collectAsState()
    val clientAnalyticsEnabled by settingsViewModel.clientAnalyticsEnabled.collectAsState()
    val clientSlamFireEnabled by settingsViewModel.clientSlamFireEnabled.collectAsState()
    val clientSlamFireAction by settingsViewModel.clientSlamFireAction.collectAsState()

    AppDialog(
        onCloseRequest = onDismissRequest,
        state = windowState ?: rememberWindowState(width = 500.dp, height = 600.dp),
        title = "Settings Pusher (Bulk Provisioning)",
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel,
        icon = icon
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Push Settings to All Clients", style = MaterialTheme.typography.titleLarge)
            Text(
                "These settings will be broadcast to all currently connected clients and will be sent to new clients upon authentication.",
                style = MaterialTheme.typography.bodyMedium
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Client Appearance", style = MaterialTheme.typography.titleMedium)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Client Theme", modifier = Modifier.weight(1f))
                        val themes = listOf("System", "Light", "Dark")
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expanded = true }) {
                                Text(clientTheme)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                themes.forEach { theme ->
                                    DropdownMenuItem(
                                        text = { Text(theme) },
                                        onClick = {
                                            settingsViewModel.setClientTheme(theme)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Client Behavior", style = MaterialTheme.typography.titleMedium)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enable Analytics", modifier = Modifier.weight(1f))
                        Switch(
                            checked = clientAnalyticsEnabled,
                            onCheckedChange = { settingsViewModel.setClientAnalyticsEnabled(it) }
                        )
                    }

                    HorizontalDivider()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enable Slam Fire", modifier = Modifier.weight(1f))
                        Switch(
                            checked = clientSlamFireEnabled,
                            onCheckedChange = { settingsViewModel.setClientSlamFireEnabled(it) }
                        )
                    }

                    if (clientSlamFireEnabled) {
                        OutlinedTextField(
                            value = clientSlamFireAction,
                            onValueChange = { settingsViewModel.setClientSlamFireAction(it) },
                            label = { Text("Slam Fire Macro Name") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. rapid_fire") }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text("Close")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        // settingsViewModel already pushes on change, but we can trigger a manual broadcast if needed
                        // for safety, we'll re-push the current state
                        settingsViewModel.setClientTheme(clientTheme)
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Broadcast Now")
                }
            }
        }
    }
}
