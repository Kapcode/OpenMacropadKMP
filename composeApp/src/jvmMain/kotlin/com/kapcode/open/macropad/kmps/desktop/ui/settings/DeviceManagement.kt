package com.kapcode.open.macropad.kmps.desktop.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.desktop.viewmodel.ClientCommunicationViewModel

@Composable
fun DeviceManagement(
    trustedDevices: Map<String, String>,
    bannedDevices: Map<String, String>,
    clientCommunicationViewModel: ClientCommunicationViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Trusted Devices", style = MaterialTheme.typography.titleMedium)
        if (trustedDevices.isEmpty()) {
            Text(
                "No trusted devices.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            trustedDevices.forEach { (id, name) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            id,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        TextButton(onClick = { clientCommunicationViewModel.removeTrustedDevice(id) }) {
                            Text("Unpair")
                        }
                        TextButton(onClick = { clientCommunicationViewModel.banDevice(id, name) }) {
                            Text("Ban", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Banned Devices", style = MaterialTheme.typography.titleMedium)
            if (bannedDevices.isNotEmpty()) {
                TextButton(onClick = { clientCommunicationViewModel.unbanAllDevices() }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Unban All")
                }
            }
        }
        if (bannedDevices.isEmpty()) {
            Text(
                "No banned devices.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            bannedDevices.forEach { (id, name) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            id,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { clientCommunicationViewModel.unbanDevice(id) }) {
                        Text("Unban")
                    }
                }
            }
        }
    }
}
