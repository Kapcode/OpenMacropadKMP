package switchdektoptocompose.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import switchdektoptocompose.ui.components.AppTooltipArea
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import switchdektoptocompose.viewmodel.ServerViewModel
import switchdektoptocompose.viewmodel.SettingsViewModel
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel as SharedSettingsViewModel
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NetworkSettings(
    serverPort: Int,
    secureServerPort: Int,
    encryptionEnabled: Boolean,
    isServerRunning: Boolean,
    defaultPairingModeQr: Boolean,
    allowNewConnections: Boolean,
    multiQrEnabled: Boolean,
    fleetModeEnabled: Boolean,
    allowOnceOnly: Boolean,
    enableWebsocketPings: Boolean,
    settingsViewModel: SettingsViewModel,
    sharedSettingsViewModel: SharedSettingsViewModel,
    serverViewModel: ServerViewModel,
    onSecuritySectionPositioned: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Security & Privacy",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.onGloballyPositioned { coordinates ->
                onSecuritySectionPositioned(coordinates.positionInParent().y)
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        AppTooltipArea(
            tooltipText = "Sets whether the Android client defaults to QR scanning (On) or PIN entry (Off) when starting a pairing request."
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Default to QR Scanning",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = defaultPairingModeQr,
                    onCheckedChange = { settingsViewModel.setDefaultPairingModeQr(it) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        AppTooltipArea(
            tooltipText = "When enabled, the server will broadcast its presence to let phones find it automatically on your home network."
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Device Discovery", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(
                    checked = allowNewConnections,
                    onCheckedChange = { settingsViewModel.setAllowNewConnections(it) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        AppTooltipArea(
            tooltipText = "When enabled, the server will show extra QR codes to make it easier for phones to scan and join."
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Multi-QR Pairing Mode",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = multiQrEnabled,
                    onCheckedChange = { sharedSettingsViewModel.setMultiQrEnabled(it) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        AppTooltipArea(
            tooltipText = "When enabled, the server will show a large grid of QR codes to help you connect many devices at the same time."
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Pairing & Sync (Fleet) Mode",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = fleetModeEnabled,
                    onCheckedChange = { settingsViewModel.setFleetModeEnabled(it) }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        AppTooltipArea(
            tooltipText = "When enabled, you must give manual permission for every phone connection. This is the safest way to run the server."
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Ask Every Time (One-Time Approvals) ONLY",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = allowOnceOnly,
                    onCheckedChange = { settingsViewModel.setAllowOnceOnly(it) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        AppTooltipArea(
            tooltipText = "When enabled, the server uses background signals to check if phones are still connected. Disable only if you have connectivity issues."
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "WebSocket Protocol Heartbeats",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = enableWebsocketPings,
                    onCheckedChange = { settingsViewModel.setEnableWebsocketPings(it) }
                )
            }
        }
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

        AppTooltipArea(
            tooltipText = "Adds an extra layer of security to your connection. Highly recommended for most users."
        ) {
            // --- Encryption Setting ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enable Encryption (WSS)", modifier = Modifier.weight(1f))
                Checkbox(
                    checked = encryptionEnabled,
                    onCheckedChange = { serverViewModel.setEncryption(it) },
                    enabled = !isServerRunning
                )
            }
        }
        Text(
            text = "Requires a restart of the server to apply.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Pairing Ban Duration ---
        val banDuration by settingsViewModel.pairingBanDurationMinutes.collectAsState()
        AppTooltipArea(
            tooltipText = "Determines how long a device is blocked from pairing after 6 failed PIN attempts."
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Pairing Ban Duration (Minutes)",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "${banDuration} min",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).widthIn(min = 60.dp)
                        )
                    }
                }
                Slider(
                    value = banDuration.toFloat(),
                    onValueChange = { settingsViewModel.setPairingBanDurationMinutes(it.toInt()) },
                    valueRange = 1f..60f,
                    steps = 59,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Pairing Strike Limit ---
        val strikeLimit by settingsViewModel.pairingStrikeLimit.collectAsState()
        AppTooltipArea(
            tooltipText = "Number of failed attempts before a temporary ban is applied. Set to 0 to disable brute-force protection."
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Pairing Strike Limit",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (strikeLimit == 0) "None (Disabled)" else "$strikeLimit Strikes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).widthIn(min = 100.dp)
                        )
                    }
                }
                Slider(
                    value = strikeLimit.toFloat(),
                    onValueChange = { settingsViewModel.setPairingStrikeLimit(it.toInt()) },
                    valueRange = 0f..20f,
                    steps = 20,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

