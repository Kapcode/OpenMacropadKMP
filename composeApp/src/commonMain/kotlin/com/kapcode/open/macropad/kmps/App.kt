package com.kapcode.open.macropad.kmps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.ui.components.ConnectionItem
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun App(
    modifier: Modifier = Modifier,
    scanServers: () -> Unit,
    foundServers: List<ServerInfo>,
    onConnectClick: (serverInfo: ServerInfo, deviceName: String) -> Unit
) {
    var deviceName by remember { mutableStateOf("Android Device") }
    var manualIpAddress by remember { mutableStateOf("") }
    var isManualSecure by remember { mutableStateOf(true) }

    MaterialTheme {
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
            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // --- Server Discovery ---
            Button(onClick = scanServers) {
                Text("Scan for Servers")
            }
            Spacer(modifier = Modifier.height(8.dp))
            foundServers.forEach { server ->
                ConnectionItem(
                    name = server.name,
                    ipAddressPort = server.address,
                    onClick = { onConnectClick(server, deviceName) }
                )
            }
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    val sampleServers = listOf(
        ServerInfo("Server 1", "192.168.1.100:8443", true),
        ServerInfo("Desktop-PC", "192.168.1.108:8449", true)
    )
    App(
        scanServers = {},
        foundServers = sampleServers,
        onConnectClick = { _, _ -> }
    )
}
