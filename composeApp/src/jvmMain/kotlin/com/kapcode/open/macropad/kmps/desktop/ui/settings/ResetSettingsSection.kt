package com.kapcode.open.macropad.kmps.desktop.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import com.kapcode.open.macropad.kmps.desktop.ui.AppDialog
import com.kapcode.open.macropad.kmps.desktop.viewmodel.ClientCommunicationViewModel
import com.kapcode.open.macropad.kmps.desktop.viewmodel.ConsoleViewModel
import com.kapcode.open.macropad.kmps.desktop.viewmodel.MacroManagerViewModel
import com.kapcode.open.macropad.kmps.desktop.viewmodel.SettingsViewModel

@Composable
fun ResetSettingsSection(
    settingsViewModel: SettingsViewModel,
    macroManagerViewModel: MacroManagerViewModel,
    clientCommunicationViewModel: ClientCommunicationViewModel,
    selectedTheme: String,
    consoleViewModel: ConsoleViewModel,
    icon: Painter? = null
) {
    var resetSettings by remember { mutableStateOf(false) }
    var resetMacros by remember { mutableStateOf(false) }
    var resetPacks by remember { mutableStateOf(false) }
    var resetPaired by remember { mutableStateOf(false) }
    var resetBanned by remember { mutableStateOf(false) }
    var resetPro by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmText by remember { mutableStateOf("") }

    if (showConfirmDialog) {
        AppDialog(
            onCloseRequest = { 
                showConfirmDialog = false 
                confirmText = ""
            },
            title = "Confirm Factory Reset",
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            icon = icon,
            alwaysOnTop = true,
            state = rememberWindowState(width = 500.dp, height = 550.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Are you sure?", style = MaterialTheme.typography.titleLarge)
                }

                Text("You are about to reset the following items:")

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (resetSettings) Text("• Application Settings", style = MaterialTheme.typography.bodyMedium)
                    if (resetMacros) Text("• All Custom Macros (Files)", style = MaterialTheme.typography.bodyMedium)
                    if (resetPacks) Text("• All Macro Packs", style = MaterialTheme.typography.bodyMedium)
                    if (resetPaired) Text("• All Paired Devices (Unpair)", style = MaterialTheme.typography.bodyMedium)
                    if (resetBanned) Text("• All Banned Devices (Unban)", style = MaterialTheme.typography.bodyMedium)
                    if (resetPro) Text("• Global Pro Status Revocation", style = MaterialTheme.typography.bodyMedium)
                }

                Text(
                    "This action is permanent and cannot be undone. Please type 'Delete' below to confirm:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = confirmText,
                    onValueChange = { confirmText = it },
                    placeholder = { Text("Delete") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = confirmText.isNotEmpty() && confirmText != "Delete"
                )

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            showConfirmDialog = false 
                            confirmText = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL")
                    }
                    Button(
                        onClick = {
                            if (resetSettings) settingsViewModel.resetAllSettings()
                            if (resetMacros) macroManagerViewModel.resetAllMacros()
                            if (resetPacks) macroManagerViewModel.resetAllPacks()
                            if (resetPaired) {
                                settingsViewModel.resetPairedDevices()
                                clientCommunicationViewModel.unbanAllDevices()
                            }
                            if (resetBanned) {
                                settingsViewModel.resetBannedDevices()
                                clientCommunicationViewModel.unbanAllDevices()
                            }
                            if (resetPro) {
                                settingsViewModel.revokeProStatus()
                            }
                            
                            resetSettings = false
                            resetMacros = false
                            resetPacks = false
                            resetPaired = false
                            resetBanned = false
                            showConfirmDialog = false
                            confirmText = ""
                        },
                        enabled = confirmText == "Delete",
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("RESET SELECTED")
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("System Maintenance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = resetSettings, onCheckedChange = { resetSettings = it })
                    Text("Reset App Settings (Ports, UI, Exit Behavior)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = resetMacros, onCheckedChange = { resetMacros = it })
                    Text("Delete All Macros (JSON & JS Files)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = resetPacks, onCheckedChange = { resetPacks = it })
                    Text("Delete All Macro Packs")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = resetPaired, onCheckedChange = { resetPaired = it })
                    Text("Unpair All Trusted Devices")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = resetBanned, onCheckedChange = { resetBanned = it })
                    Text("Clear All Banned Devices (Reset Strikes)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = resetPro, onCheckedChange = { resetPro = it })
                    Text("Revoke Global Pro Status (Shared 12h Timer)")
                }
                
                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = resetSettings || resetMacros || resetPacks || resetPaired || resetBanned || resetPro,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Warning, null)
                    Spacer(Modifier.width(8.dp))
                    Text("EXECUTE FACTORY RESET")
                }
            }
        }
    }
}
