package switchdektoptocompose.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import switchdektoptocompose.ui.components.AppTooltipArea
import androidx.compose.runtime.Composable
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
    onShowPushSettingsRequest: () -> Unit,
    onSecuritySectionPositioned: (Float) -> Unit
) {
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
            Text("Default to QR Scanning", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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
            Text("Multi-QR Pairing Mode", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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
            Text("Pairing & Sync (Fleet) Mode", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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
            Text("Ask Every Time (One-Time Approvals) ONLY", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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
            Text("WebSocket Protocol Heartbeats", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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

    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onShowPushSettingsRequest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Open Bulk Settings Pusher")
    }
}

