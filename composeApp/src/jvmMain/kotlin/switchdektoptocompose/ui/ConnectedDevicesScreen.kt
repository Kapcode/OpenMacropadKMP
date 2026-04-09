package switchdektoptocompose.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.ui.components.ConnectionItem
import switchdektoptocompose.model.ClientInfo

@Composable
fun ConnectedDevicesScreen(
    devices: List<ClientInfo>,
    onDisconnect: (String) -> Unit = {},
    onUnpair: (String) -> Unit = {},
    onBan: (ClientInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
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
                ContextMenuArea(
                    items = {
                        listOf(
                            ContextMenuItem("Disconnect") { onDisconnect(device.id) },
                            ContextMenuItem("Unpair") { onUnpair(device.id) },
                            ContextMenuItem("Ban Device") { onBan(device) },
                            ContextMenuItem("Copy ID") { clipboardManager.setText(AnnotatedString(device.id)) }
                        )
                    }
                ) {
                    ConnectionItem(
                        name = device.name,
                        ipAddressPort = device.id,
                        onClick = {
                            clipboardManager.setText(AnnotatedString("${device.name} (${device.id})"))
                        }
                    )
                }
            }
        }
    }
}
