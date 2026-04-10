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
import com.kapcode.open.macropad.kmps.ui.components.LoadingIndicator
import com.kapcode.open.macropad.kmps.ui.components.ThreeDotsLoading
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun App(
    modifier: Modifier = Modifier,
    scanServers: () -> Unit,
    stopScanning: () -> Unit,
    foundServers: List<ServerInfo>,
    isScanning: Boolean = false,
    onConnectClick: (serverInfo: ServerInfo, deviceName: String) -> Unit
) {
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
            label = { Text("Device Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // --- Server Discovery ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = scanServers, enabled = !isScanning) {
                Text(if (isScanning) "Scanning..." else "Scan for Servers")
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
                    onConnectClick(server.copy(name = "SET_DEFAULT"), deviceName)
                },
                onClick = { onConnectClick(server, deviceName) }
            )
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // --- Manual Connection ---
        Text("Manual Connection", style = MaterialTheme.typography.headlineSmall)
        TextField(
            value = manualIpAddress,
            onValueChange = { manualIpAddress = it },
            label = { Text("Server IP:Port") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isManualSecure, onCheckedChange = { isManualSecure = it })
            Text("Use Secure Connection (WSS)")
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
            Text("Connect Manually")
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
