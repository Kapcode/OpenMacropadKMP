package switchdektoptocompose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.ui.components.ConnectionItem // Corrected import path

@Composable
fun ConnectedDevicesScreen(
    devices: List<ClientInfo>,
    modifier: Modifier = Modifier
) {
    if (devices.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No devices connected.")
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(devices, key = { it.id }) { device ->
                ConnectionItem(
                    name = device.name,
                    ipAddressPort = device.id, // The ID is the unique identifier
                    onClick = {
                        println("Clicked on device: ${device.name} (${device.id})")
                        // Future: Add logic for disconnecting or interacting with the device.
                    }
                )
            }
        }
    }
}