package switchdektoptocompose.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import switchdektoptocompose.viewmodel.ClientCommunicationViewModel
import switchdektoptocompose.viewmodel.MacroManagerViewModel
import switchdektoptocompose.viewmodel.SettingsViewModel

@Composable
fun ResetSettingsSection(
    settingsViewModel: SettingsViewModel,
    macroManagerViewModel: MacroManagerViewModel,
    clientCommunicationViewModel: ClientCommunicationViewModel
) {
    var resetSettings by remember { mutableStateOf(false) }
    var resetMacros by remember { mutableStateOf(false) }
    var resetPacks by remember { mutableStateOf(false) }
    var resetPaired by remember { mutableStateOf(false) }
    var resetBanned by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmText by remember { mutableStateOf("") }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { 
                showConfirmDialog = false 
                confirmText = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Confirm Factory Reset")
                }
            },
            text = {
                Column {
                    Text("Are you sure you want to reset the following items?")
                    Spacer(Modifier.height(8.dp))
                    if (resetSettings) Text("• Application Settings", style = MaterialTheme.typography.bodyMedium)
                    if (resetMacros) Text("• All Custom Macros (Files)", style = MaterialTheme.typography.bodyMedium)
                    if (resetPacks) Text("• All Macro Packs", style = MaterialTheme.typography.bodyMedium)
                    if (resetPaired) Text("• All Paired Devices (Unpair)", style = MaterialTheme.typography.bodyMedium)
                    if (resetBanned) Text("• All Banned Devices (Unban)", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "This action is permanent and cannot be undone. Please type 'Delete' below to confirm:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        placeholder = { Text("Delete") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = confirmText.isNotEmpty() && confirmText != "Delete"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetSettings) settingsViewModel.resetAllSettings()
                        if (resetMacros) macroManagerViewModel.resetAllMacros()
                        if (resetPacks) macroManagerViewModel.resetAllPacks()
                        if (resetPaired) {
                            settingsViewModel.resetPairedDevices()
                            // Refresh trust state in UI
                            clientCommunicationViewModel.unbanAllDevices() // Helper to refresh lists
                        }
                        if (resetBanned) {
                            settingsViewModel.resetBannedDevices()
                            clientCommunicationViewModel.unbanAllDevices()
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("RESET SELECTED")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showConfirmDialog = false 
                    confirmText = ""
                }) {
                    Text("CANCEL")
                }
            }
        )
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
                
                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = resetSettings || resetMacros || resetPacks || resetPaired || resetBanned,
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
