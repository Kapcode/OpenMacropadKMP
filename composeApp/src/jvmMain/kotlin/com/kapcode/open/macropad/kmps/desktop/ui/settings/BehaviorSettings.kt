package com.kapcode.open.macropad.kmps.desktop.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import com.kapcode.open.macropad.kmps.desktop.ui.components.AppTooltipArea
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.desktop.viewmodel.SettingsViewModel

@Composable
fun BehaviorSettings(
    exitBehavior: String,
    clickTrayToToggle: Boolean,
    enablePackSwitchNotifications: Boolean,
    enableToasts: Boolean,
    enableBackgroundToasts: Boolean,
    enableNetworkToasts: Boolean,
    includeWindowNamesInToasts: Boolean,
    toastDurationMs: Long,
    toastTarget: String,
    notificationClientIds: Set<String>,
    trustedDevices: Map<String, String>,
    settingsViewModel: SettingsViewModel,
    onShowShortcutsRequest: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("App Exit Behavior", style = MaterialTheme.typography.titleMedium)
        Column(Modifier.selectableGroup()) {
            listOf(
                "ASK" to "Ask every time",
                "TRAY" to "Exit to system tray",
                "EXIT" to "Just exit the application"
            ).forEach { (value, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .selectable(
                            selected = (exitBehavior == value),
                            onClick = { settingsViewModel.setExitBehavior(value) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (exitBehavior == value),
                        onClick = null
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        AppTooltipArea(
            tooltipText = "When enabled, clicking the icon in your system tray (near the clock) will hide or show the main window."
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Click tray icon to show/hide window", modifier = Modifier.weight(1f))
                Checkbox(
                    checked = clickTrayToToggle,
                    onCheckedChange = { settingsViewModel.setClickTrayToToggle(it) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        AppTooltipArea(
            tooltipText = "Shows a small pop-up on your screen whenever a new Macro Pack is automatically activated."
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show notifications on pack switch", modifier = Modifier.weight(1f))
                Checkbox(
                    checked = enablePackSwitchNotifications,
                    onCheckedChange = { settingsViewModel.setEnablePackSwitchNotifications(it) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Toast Notifications", style = MaterialTheme.typography.titleMedium)
        AppTooltipArea(
            tooltipText = "Enables short temporary messages (Toasts) that appear on your screen to confirm actions."
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enable Global Toasts", modifier = Modifier.weight(1f))
                Switch(
                    checked = enableToasts,
                    onCheckedChange = { settingsViewModel.setEnableToasts(it) }
                )
            }
        }
        AppTooltipArea(
            tooltipText = "Shows notifications even if the app is hidden or minimized."
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Toasts in Background / Headless", modifier = Modifier.weight(1f))
                Switch(
                    checked = enableBackgroundToasts,
                    onCheckedChange = { settingsViewModel.setEnableBackgroundToasts(it) }
                )
            }
        }
        AppTooltipArea(
            tooltipText = "Sends the notification message to your connected phones or tablets."
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send Toasts over Network", modifier = Modifier.weight(1f))
                Switch(
                    checked = enableNetworkToasts,
                    onCheckedChange = { settingsViewModel.setEnableNetworkToasts(it) }
                )
            }
        }
        AppTooltipArea(
            tooltipText = "Adds the name of the active app (e.g., Photoshop) to the notification message."
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Include Window Names in Toasts", modifier = Modifier.weight(1f))
                Switch(
                    checked = includeWindowNamesInToasts,
                    onCheckedChange = { settingsViewModel.setIncludeWindowNamesInToasts(it) }
                )
            }
        }

        OutlinedTextField(
            value = toastDurationMs.toString(),
            onValueChange = { it.toLongOrNull()?.let { duration -> settingsViewModel.setToastDurationMs(duration) } },
            label = { Text("Toast Duration (ms)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))
        Text("Notification Target", style = MaterialTheme.typography.titleMedium)
        Column(Modifier.selectableGroup()) {
            listOf(
                "SERVER" to "Show on Server Only",
                "CLIENTS" to "Show on Selected Clients Only",
                "BOTH" to "Show on Both Server & Clients"
            ).forEach { (value, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .selectable(
                            selected = (toastTarget == value),
                            onClick = { settingsViewModel.setToastTarget(value) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = (toastTarget == value), onClick = null)
                    Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
                }
            }
        }

        if (toastTarget != "SERVER" && trustedDevices.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Target Clients", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { settingsViewModel.setAllNotificationClients(trustedDevices.keys) }) {
                    Text("Select All")
                }
                TextButton(onClick = { settingsViewModel.setAllNotificationClients(emptySet()) }) {
                    Text("Deselect All")
                }
            }
            
            Column {
                trustedDevices.forEach { (id, name) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Checkbox(
                            checked = notificationClientIds.contains(id),
                            onCheckedChange = { settingsViewModel.toggleNotificationClient(id) }
                        )
                        Text(name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // --- Shortcuts Section ---
        Text("Shortcuts & Keymap", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onShowShortcutsRequest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Configure Global & App Shortcuts")
        }
    }
}
