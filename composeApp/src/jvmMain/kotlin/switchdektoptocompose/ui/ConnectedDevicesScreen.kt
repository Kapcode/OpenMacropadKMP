package switchdektoptocompose.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.ui.components.ConnectionItem
import switchdektoptocompose.logic.ConnectionHistoryManager
import switchdektoptocompose.model.ClientInfo

@Composable
fun ConnectedDevicesScreen(
    devices: List<ClientInfo>,
    history: List<ConnectionHistoryManager.ConnectionEvent>,
    totalCurrencySpent: Long = 0,
    onDisconnect: (String) -> Unit = {},
    onUnpair: (String) -> Unit = {},
    onBan: (ClientInfo) -> Unit = {},
    onClearHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    
    Column(modifier = modifier.fillMaxSize()) {
        SectionHeader(
            icon = Icons.Default.Sensors,
            title = "CURRENT SESSIONS",
            badgeCount = devices.size
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CurrencyExchange, 
                    null, 
                    modifier = Modifier.size(16.dp), 
                    tint = Color(0xFFFFD700)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Total Spent: $totalCurrencySpent",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (devices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No active sessions.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(devices, key = { it.id }) { device ->
                    ContextMenuArea(
                        items = {
                            val items = mutableListOf(
                                ContextMenuItem("Disconnect") { onDisconnect(device.id) }
                            )
                            if (device.isTrusted) {
                                items.add(ContextMenuItem("Unpair") { onUnpair(device.id) })
                            }
                            items.add(ContextMenuItem("Ban Device") { onBan(device) })
                            items.add(ContextMenuItem("Copy ID") { clipboardManager.setText(AnnotatedString(device.id)) })
                            items
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ConnectionItem(
                                    name = device.name,
                                    ipAddressPort = device.id,
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString("${device.name} (${device.id})"))
                                    }
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Icon(
                                    Icons.Default.CurrencyExchange,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFFFFD700)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "${device.currency}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

        // History Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "RECENT ACTIVITY",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (history.isNotEmpty()) {
                IconButton(onClick = onClearHistory, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No recent activity.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { event ->
                    ContextMenuArea(
                        items = {
                            listOf(
                                ContextMenuItem("Unpair / Remove Trust") { onUnpair(event.clientId) },
                                ContextMenuItem("Ban Device") { 
                                    onBan(ClientInfo(id = event.clientId, name = event.clientName)) 
                                },
                                ContextMenuItem("Copy ID") { 
                                    clipboardManager.setText(AnnotatedString(event.clientId)) 
                                }
                            )
                        }
                    ) {
                        HistoryItem(event)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    title: String, 
    badgeCount: Int,
    content: @Composable (RowScope.() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (badgeCount > 0) {
            Spacer(Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    badgeCount.toString(),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        if (content != null) {
            Spacer(Modifier.weight(1f))
            content()
        }
    }
}

@Composable
fun HistoryItem(event: ConnectionHistoryManager.ConnectionEvent) {
    val color = when (event.action) {
        "Connected", "Permanently Approved", "Temporarily Approved" -> Color(0xFF4CAF50)
        "Disconnected" -> MaterialTheme.colorScheme.onSurfaceVariant
        "Banned", "Rejected", "Force Disconnect" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), MaterialTheme.shapes.small)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                event.action.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                event.timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "${event.clientName} (${event.clientId})",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        event.metadata?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        event.reason?.let {
            Text(
                "Reason: $it",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            )
        }
    }
}
