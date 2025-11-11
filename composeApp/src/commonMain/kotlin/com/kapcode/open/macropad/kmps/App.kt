package com.kapcode.open.macropad.kmps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
    foundServers: List<String>,
    onConnectClick: (serverAddress: String, deviceName: String) -> Unit = { _, _ -> }
) {
    var deviceName by remember { mutableStateOf("Android Device") }

    MaterialTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextField(
                value = deviceName,
                onValueChange = { deviceName = it },
                label = { Text("Device Name") }
            )

            Button(onClick = {
                scanServers()
            }) {
                Text("Scan for Available Servers")
            }

            // Display found servers
            foundServers.forEach { serverAddress ->
                ConnectionItem(
                    name = "Discovered Server", // You might want to get a real name later
                    ipAddressPort = serverAddress,
                    onClick = { onConnectClick(serverAddress, deviceName) }
                )
            }
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    App(
        scanServers = {},
        foundServers = listOf("192.168.1.10:9999", "192.168.1.15:9999"),
        onConnectClick = { _, _ -> /* Preview click handler */ }
    )
}