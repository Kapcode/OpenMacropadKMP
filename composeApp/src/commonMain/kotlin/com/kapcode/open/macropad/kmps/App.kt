package com.kapcode.open.macropad.kmps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.ui.components.ConnectionItem
import com.kapcode.open.macropad.kmps.ui.components.ThreeDotsLoading
import com.kapcode.open.macropad.kmps.hardware.HardwareTriggerManager
import com.kapcode.open.macropad.kmps.models.TrustedServer
import androidx.compose.ui.tooling.preview.Preview

import com.kapcode.`open`.macropad.kmps.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun App(
    modifier: Modifier = Modifier,
    hardwareTriggerManager: HardwareTriggerManager? = null,
    scanServers: () -> Unit,
    stopScanning: () -> Unit,
    foundServers: List<ServerInfo>,
    isScanning: Boolean = false,
    onConnectClick: (serverInfo: ServerInfo, deviceName: String) -> Unit
) {
    DisposableEffect(hardwareTriggerManager) {
        hardwareTriggerManager?.start()
        onDispose {
            hardwareTriggerManager?.stop()
        }
    }

    var deviceName by remember { mutableStateOf("${DeviceInfo.name}-${DeviceInfo.uniqueId}") }
    var manualIpAddress by remember { mutableStateOf("") }
    var isManualSecure by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Make the column scrollable
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // --- Device Name ---
        TextField(
            value = deviceName,
            onValueChange = { deviceName = it },
            label = { Text(stringResource(Res.string.device_name)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // --- Server Discovery ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = scanServers, enabled = !isScanning) {
                Text(if (isScanning) stringResource(Res.string.scanning) else stringResource(Res.string.scan_for_servers))
            }
            if (isScanning) {
                Spacer(modifier = Modifier.width(16.dp))
                ThreeDotsLoading()
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = stopScanning,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop Scanning",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        foundServers.forEach { server ->
            ConnectionItem(
                name = server.name,
                ipAddressPort = server.address,
                isDefault = server.isDefault,
                onSetDefault = { 
                    // This will be handled by the caller to update storage and state
                    onConnectClick(server.copy(name = TrustedServer.ACTION_SET_DEFAULT), deviceName)
                },
                onClick = {
                    onConnectClick(server, deviceName)
                }
            )
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // --- Manual Connection ---
        Text(stringResource(Res.string.manual_connection), style = MaterialTheme.typography.headlineSmall)
        TextField(
            value = manualIpAddress,
            onValueChange = { manualIpAddress = it },
            label = { Text(stringResource(Res.string.server_ip_port)) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isManualSecure, onCheckedChange = { isManualSecure = it })
            Text(stringResource(Res.string.use_secure_connection))
        }
        Button(
            onClick = {
                if (manualIpAddress.isNotBlank()) {
                    val manualServer = ServerInfo("Manual", manualIpAddress, isManualSecure)
                    onConnectClick(manualServer, deviceName)
                }
            },
            enabled = manualIpAddress.isNotBlank()
        ) {
            Text(stringResource(Res.string.connect_manually))
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    val sampleServers = listOf(
        ServerInfo("Server 1", "192.18.1.100:8443", true),
        ServerInfo("Desktop-PC", "192.168.1.108:8449", true)
    )
    App(
        scanServers = {},
        stopScanning = {},
        foundServers = sampleServers,
        onConnectClick = { _, _ -> }
    )
}
