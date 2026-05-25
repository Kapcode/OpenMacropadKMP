package switchdektoptocompose.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import switchdektoptocompose.ui.components.AppTooltipArea
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import switchdektoptocompose.viewmodel.DesktopViewModel
import switchdektoptocompose.viewmodel.SettingsViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DesktopTopBar(
    desktopViewModel: DesktopViewModel,
    settingsViewModel: SettingsViewModel,
    isServerRunning: Boolean,
    connectedDevicesCount: Int,
    isMacroExecutionEnabled: Boolean,
    serverIpAddress: String,
    currentPort: Int,
    encryptionEnabled: Boolean,
    allowOnceOnly: Boolean,
    allowNewConnections: Boolean,
    selectedTheme: String,
    exitBehavior: String,
    eStopKey: String,
    onShowSettings: () -> Unit,
    onShowSettingsScrollToSecurity: () -> Unit = {},
    onShowShortcuts: () -> Unit,
    onExit: () -> Unit,
    onShowExitDialog: () -> Unit
) {
    val statusColor = if (isServerRunning) {
        if (selectedTheme == "Dark Blue") Color.Green else Color(0xFF008000)
    } else {
        Color.Red
    }

    Row(
        modifier = Modifier.fillMaxSize().background(panelBackground(0)).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var menuExpanded by remember { mutableStateOf(false) }
        AppTooltipArea(tooltipText = "Menu", delayMillis = 0) {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Start Server") },
                onClick = { desktopViewModel.startServer(); menuExpanded = false },
                enabled = !isServerRunning,
                leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
            )
            DropdownMenuItem(
                text = { Text("Stop Server") },
                onClick = { desktopViewModel.stopServer(); menuExpanded = false },
                enabled = isServerRunning,
                leadingIcon = { Icon(Icons.Default.Stop, null) }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = { onShowSettings(); menuExpanded = false },
                leadingIcon = { Icon(Icons.Default.Settings, null) }
            )
            DropdownMenuItem(
                text = { Text("Shortcuts & Keymap") },
                onClick = { onShowShortcuts(); menuExpanded = false },
                leadingIcon = { Icon(Icons.Default.Keyboard, null) }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Exit") },
                onClick = {
                    menuExpanded = false
                    if (exitBehavior == "ASK") {
                        onShowExitDialog()
                    } else {
                        onExit()
                    }
                },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) }
            )
        }
        
        Spacer(Modifier.width(16.dp))

        Box(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 4.dp, horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status: ${if (isServerRunning) "Running" else "Stopped"}", color = statusColor, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(12.dp))
                    Text("Connected: $connectedDevicesCount", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Macros: ${if (isMacroExecutionEnabled) "Enabled" else "Disabled"}", 
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isMacroExecutionEnabled) Color.Unspecified else Color.Red
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Address: $serverIpAddress:$currentPort", style = MaterialTheme.typography.bodySmall)
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                AppTooltipArea(
                    tooltipText = "Security Status (Click to edit)",
                    delayMillis = 500
                ) {
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), MaterialTheme.shapes.small)
                            .clickable {
                                onShowSettingsScrollToSecurity()
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (encryptionEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (encryptionEnabled) statusColor else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Security: ${if (encryptionEnabled) "Encrypted" else "Unencrypted"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (encryptionEnabled) statusColor else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "One-Time: ${if (allowOnceOnly) "Yes" else "No"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (allowOnceOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Discovery: ${if (allowNewConnections) "On" else "Off"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (allowNewConnections) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        
        AppTooltipArea(tooltipText = "Emergency Stop Key", delayMillis = 0) {
            var eStopMenuExpanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { eStopMenuExpanded = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("E-Stop: $eStopKey", style = MaterialTheme.typography.labelMedium)
                }
                DropdownMenu(
                    expanded = eStopMenuExpanded,
                    onDismissRequest = { eStopMenuExpanded = false },
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    Box(modifier = Modifier.sizeIn(maxHeight = 300.dp)) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            val fKeys = (1..12).map { "F$it" }
                            fKeys.forEach { key ->
                                DropdownMenuItem(
                                    text = { Text(key) },
                                    onClick = {
                                        settingsViewModel.setEStopKey(key)
                                        eStopMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        AppTooltipArea(tooltipText = "Toggle Macro Execution", delayMillis = 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Power, contentDescription = "Macros Enabled", modifier = Modifier.size(20.dp))
                Switch(
                    checked = isMacroExecutionEnabled,
                    onCheckedChange = { desktopViewModel.setMacroExecutionEnabled(it) },
                    modifier = Modifier.scale(0.8f)
                )
            }
        }
    }
}
