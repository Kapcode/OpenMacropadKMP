package com.kapcode.open.macropad.kmps.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import com.kapcode.open.macropad.kmps.desktop.ui.components.AppTooltipArea
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.Res
import com.kapcode.`open`.macropad.kmps.*
import com.kapcode.open.macropad.kmps.ui.components.*
import com.kapcode.open.macropad.kmps.desktop.logic.ConnectionHistoryManager
import com.kapcode.open.macropad.kmps.desktop.model.ClientInfo
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ConnectedDevicesScreen(
    devices: List<ClientInfo>,
    history: List<ConnectionHistoryManager.ConnectionEvent>,
    trustedDevices: Map<String, String> = emptyMap(),
    totalCurrencySpent: Long = 0,
    currencySpentEvents: SharedFlow<String>? = null,
    graceSkipEvents: SharedFlow<String>? = null,
    onDisconnect: (String) -> Unit = {},
    onUnpair: (String) -> Unit = {},
    onBan: (ClientInfo) -> Unit = {},
    onUnban: (String) -> Unit = {},
    onClearHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val animationManager = LocalKapAnimationManager.current
    val lastClickedPositions = remember { mutableMapOf<String, Offset>() }

    LaunchedEffect(currencySpentEvents) {
        currencySpentEvents?.collectLatest { clientId ->
            val pos = lastClickedPositions[clientId]
            if (pos != null) {
                animationManager.triggerFlight(pos, animationManager.balancePosition)
            }
        }
    }

    LaunchedEffect(graceSkipEvents) {
        graceSkipEvents?.collectLatest { clientId ->
            val pos = lastClickedPositions[clientId]
            if (pos != null) {
                animationManager.triggerGraceSkip(pos)
            }
        }
    }
    
    val connectedIds = remember(devices) { devices.map { it.id }.toSet() }
    val offlineTrustedDevices = remember(trustedDevices, connectedIds) {
        trustedDevices.filterKeys { it !in connectedIds }
    }

    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val goldColor = if (isLight) Color(0xFFB8860B) else Color(0xFFFFD700) // Darker gold for light theme
    val textShadow = Shadow(
        color = Color.Black.copy(alpha = if (isLight) 0.3f else 0.8f),
        offset = Offset(1f, 1f),
        blurRadius = 2f
    )

    Column(modifier = modifier.fillMaxSize()) {
        SectionHeader(
            icon = Icons.Default.Sensors,
            title = "CURRENT SESSIONS",
            badgeCount = devices.size
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(Res.drawable.macropadIcon64),
                    null,
                    modifier = Modifier
                        .size(16.dp)
                        .trackBalancePosition(animationManager),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Total Spent: $totalCurrencySpent",
                    style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow),
                    color = goldColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (devices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(60.dp),
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
                                items.add(ContextMenuItem("Revoke Trust (Unpair)") { onUnpair(device.id) })
                            }
                            items.add(ContextMenuItem("Ban Device") { onBan(device) })
                            items.add(ContextMenuItem("Copy ID") { scope.launch { clipboard.setClipEntry(ClipEntry(AnnotatedString(device.id))) } })
                            items
                        }
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ConnectionItem(
                                        name = device.name,
                                        ipAddressPort = device.id,
                                        onClick = {
                                            scope.launch { clipboard.setClipEntry(ClipEntry(AnnotatedString("${device.name} (${device.id})"))) }
                                        }
                                    )
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (device.isTrusted) {
                                        AppTooltipArea(tooltipText = "Trusted Device") {
                                            Icon(
                                                Icons.Default.VerifiedUser,
                                                contentDescription = "Trusted",
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(20.dp).padding(horizontal = 4.dp)
                                            )
                                        }
                                        
                                        AppTooltipArea(tooltipText = "Revoke Trust") {
                                            IconButton(onClick = { onUnpair(device.id) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Outlined.LinkOff, "Revoke Trust", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    } else {
                                        AppTooltipArea(tooltipText = "One-Time Session") {
                                            Icon(
                                                Icons.Default.Timer,
                                                contentDescription = "Temporary",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp).padding(horizontal = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(4.dp))
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    painter = painterResource(Res.drawable.macropadIcon64),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp).trackWidgetPosition {
                                                        lastClickedPositions[device.id] = it
                                                    },
                                                    tint = Color.Unspecified
                                                )
                                                Spacer(Modifier.width(2.dp))
                                                Text(
                                                    "${device.currency}",
                                                    style = MaterialTheme.typography.labelLarge.copy(shadow = textShadow),
                                                    color = goldColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            
                                            if (device.lastSpentTime > 0) {
                                                GraceTimerBar(
                                                    lastSpentTime = device.lastSpentTime,
                                                    modifier = Modifier.width(48.dp).padding(top = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    IconButton(onClick = { onDisconnect(device.id) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Close, "Disconnect", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (offlineTrustedDevices.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SectionHeader(
                icon = Icons.Default.VerifiedUser,
                title = "TRUSTED DEVICES (OFFLINE)",
                badgeCount = offlineTrustedDevices.size
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(offlineTrustedDevices.toList(), key = { it.first }) { (id, name) ->
                    ContextMenuArea(
                        items = {
                            listOf(
                                ContextMenuItem("Revoke Trust") { onUnpair(id) },
                                ContextMenuItem("Ban Device") { onBan(ClientInfo(id, name)) },
                                ContextMenuItem("Copy ID") { scope.launch { clipboard.setClipEntry(ClipEntry(AnnotatedString(id))) } }
                            )
                        }
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Devices, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(id, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onUnpair(id) }) {
                                    Icon(Icons.Outlined.GppBad, "Revoke Trust", tint = MaterialTheme.colorScheme.error)
                                }
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
                                    scope.launch { clipboard.setClipEntry(ClipEntry(AnnotatedString(event.clientId))) }
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
fun GraceTimerBar(
    lastSpentTime: Long,
    modifier: Modifier = Modifier
) {
    var remainingGraceFraction by remember(lastSpentTime) { mutableStateOf(0f) }

    LaunchedEffect(lastSpentTime) {
        if (lastSpentTime > 0) {
            while (true) {
                val now = System.currentTimeMillis()
                val elapsed = now - lastSpentTime
                val remaining = (BillingConstants.GRACE_PERIOD_MS - elapsed).coerceAtLeast(0L)
                remainingGraceFraction = remaining.toFloat() / BillingConstants.GRACE_PERIOD_MS
                if (remaining <= 0) break
                kotlinx.coroutines.delay(50)
            }
        } else {
            remainingGraceFraction = 0f
        }
    }

    if (remainingGraceFraction > 0) {
        LinearProgressIndicator(
            progress = { remainingGraceFraction },
            modifier = modifier.height(2.dp),
            color = Color(0xFF2196F3), // Grace blue
            trackColor = Color(0xFF2196F3).copy(alpha = 0.2f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
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
