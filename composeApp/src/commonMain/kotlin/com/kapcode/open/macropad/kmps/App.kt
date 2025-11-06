package com.kapcode.open.macropad.kmps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.kapcode.open.macropad.kmps.ui.components.ConnectionItem

@Composable
fun App(
    scanServers: () -> Unit,
    foundServers: List<String>,
    onConnectClick: (String) -> Unit = {} // Added onConnectClick parameter
) {
    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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
                    onClick = onConnectClick // Pass the onConnectClick lambda
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
        onConnectClick = { /* Preview click handler */ }
    ) 
}