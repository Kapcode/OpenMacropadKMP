package switchdektoptocompose.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import switchdektoptocompose.viewmodel.*
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel as SharedSettingsViewModel
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsDialog(
    desktopViewModel: DesktopViewModel,
    settingsViewModel: SettingsViewModel,
    sharedSettingsViewModel: SharedSettingsViewModel,
    consoleViewModel: ConsoleViewModel,
    onDismissRequest: () -> Unit,
    initialScrollToSecurity: Boolean = false
) {
    val serverPort by settingsViewModel.serverPort.collectAsState()
    val secureServerPort by settingsViewModel.secureServerPort.collectAsState()
    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()
    val uiState by desktopViewModel.uiState.collectAsState()
    val encryptionEnabled = uiState.encryptionEnabled
    val isServerRunning = uiState.isServerRunning
    val bannedDevices = uiState.bannedDevices
    val trustedDevices = uiState.trustedDevices
    val minimizeToTray by settingsViewModel.minimizeToTray.collectAsState()
    val showMinimizeToTrayDialog by settingsViewModel.showMinimizeToTrayDialog.collectAsState()
    val animateToTraySetting by settingsViewModel.animateToTray.collectAsState()
    val clickTrayToToggle by settingsViewModel.clickTrayToToggle.collectAsState()
    val hardEstop by settingsViewModel.hardEstop.collectAsState()
    val allowNewConnections by settingsViewModel.allowNewConnections.collectAsState()
    val allowOnceOnly by settingsViewModel.allowOnceOnly.collectAsState()
    val fleetModeEnabled by settingsViewModel.fleetModeEnabled.collectAsState()
    val enableWebsocketPings by settingsViewModel.enableWebsocketPings.collectAsState()
    val multiQrEnabled by sharedSettingsViewModel.multiQrEnabled.collectAsState()
    val defaultPairingModeQr by settingsViewModel.defaultPairingModeQr.collectAsState()

    // Scroll state management
    val scrollState = rememberScrollState()
    var securitySectionOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(initialScrollToSecurity, securitySectionOffset) {
        if (initialScrollToSecurity && securitySectionOffset > 0) {
            scrollState.animateScrollTo(securitySectionOffset.toInt())
        }
    }

    AppDialog(
        onCloseRequest = onDismissRequest,
        state = rememberWindowState(width = 600.dp, height = 700.dp),
        title = "Settings",
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 12.dp)
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                        // --- Theme Selection ---
                        Text("Theme", style = MaterialTheme.typography.titleMedium)
                        Column(Modifier.selectableGroup()) {
                            settingsViewModel.availableThemes.forEach { theme ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .selectable(
                                            selected = (theme == selectedTheme),
                                            onClick = { settingsViewModel.selectTheme(theme) },
                                            role = Role.RadioButton
                                        )
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (theme == selectedTheme),
                                        onClick = null // null recommended for accessibility with screenreaders
                                    )
                                    Text(
                                        text = theme,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // --- Behavior Settings ---
                         Text("Behavior", style = MaterialTheme.typography.titleMedium)
                         Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Exit to tray", modifier = Modifier.weight(1f))
                            Checkbox(
                                checked = minimizeToTray,
                                onCheckedChange = { settingsViewModel.setMinimizeToTray(it) }
                            )
                        }
                        if (minimizeToTray) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp)
                            ) {
                                Text("Show notification when minimizing", modifier = Modifier.weight(1f))
                                Checkbox(
                                    checked = showMinimizeToTrayDialog,
                                    onCheckedChange = { settingsViewModel.setShowMinimizeToTrayDialog(it) }
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp)
                            ) {
                                Text("Animate to tray", modifier = Modifier.weight(1f))
                                Checkbox(
                                    checked = animateToTraySetting,
                                    onCheckedChange = { settingsViewModel.setAnimateToTray(it) }
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Click tray icon to show/hide window", modifier = Modifier.weight(1f))
                            Checkbox(
                                checked = clickTrayToToggle,
                                onCheckedChange = { settingsViewModel.setClickTrayToToggle(it) }
                            )
                        }
                         Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Hard E-Stop", modifier = Modifier.weight(1f))
                            Checkbox(
                                checked = hardEstop,
                                onCheckedChange = { settingsViewModel.setHardEstop(it) }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // --- Network/Security Settings ---
                        Text(
                            "Security & Privacy",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                securitySectionOffset = coordinates.positionInParent().y
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TooltipArea(
                            tooltip = {
                                Surface(
                                    modifier = Modifier.padding(4.dp),
                                    shape = MaterialTheme.shapes.small,
                                    shadowElevation = 4.dp
                                ) {
                                    Text(
                                        "Sets whether the Android client defaults to QR scanning (On) or PIN entry (Off) when starting a pairing request.",
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Default to QR Scanning", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = defaultPairingModeQr,
                                    onCheckedChange = { settingsViewModel.setDefaultPairingModeQr(it) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TooltipArea(
                            tooltip = {
                                Surface(
                                    modifier = Modifier.padding(4.dp),
                                    shape = MaterialTheme.shapes.small,
                                    shadowElevation = 4.dp
                                ) {
                                    Text(
                                        "When enabled, new devices can find this server and request to pair.",
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Device Discovery", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = allowNewConnections,
                                    onCheckedChange = { settingsViewModel.setAllowNewConnections(it) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TooltipArea(
                            tooltip = {
                                Surface(
                                    modifier = Modifier.padding(4.dp),
                                    shape = MaterialTheme.shapes.small,
                                    shadowElevation = 4.dp
                                ) {
                                    Text(
                                        "When enabled, multiple QR codes can be used for pairing.",
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Multi-QR Pairing Mode", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = multiQrEnabled,
                                    onCheckedChange = { sharedSettingsViewModel.setMultiQrEnabled(it) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TooltipArea(
                            tooltip = {
                                Surface(
                                    modifier = Modifier.padding(4.dp),
                                    shape = MaterialTheme.shapes.small,
                                    shadowElevation = 4.dp
                                ) {
                                    Text(
                                        "When enabled, the pairing dialog uses a multi-QR grid for high-reliability syncing.",
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Sync (Fleet) Mode", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = fleetModeEnabled,
                                    onCheckedChange = { settingsViewModel.setFleetModeEnabled(it) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TooltipArea(
                            tooltip = {
                                Surface(
                                    modifier = Modifier.padding(4.dp),
                                    shape = MaterialTheme.shapes.small,
                                    shadowElevation = 4.dp
                                ) {
                                    Text(
                                        "If enabled, all new connections must be approved manually every time. No new devices will be added to the trusted list.",
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ask Every Time (One-Time Approvals) ONLY", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = allowOnceOnly,
                                    onCheckedChange = { settingsViewModel.setAllowOnceOnly(it) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TooltipArea(
                            tooltip = {
                                Surface(
                                    modifier = Modifier.padding(4.dp),
                                    shape = MaterialTheme.shapes.small,
                                    shadowElevation = 4.dp
                                ) {
                                    Text(
                                        "When enabled, the server uses Ktor native WebSocket pings (15s/30s). Disable this if clients are disconnecting frequently due to timeout errors.",
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("WebSocket Protocol Heartbeats", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = enableWebsocketPings,
                                    onCheckedChange = { settingsViewModel.setEnableWebsocketPings(it) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = serverPort.toString(),
                                onValueChange = { settingsViewModel.onServerPortChange(it) },
                                label = { Text("Server Port (WS)") },
                                modifier = Modifier.weight(1f),
                                enabled = !isServerRunning
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            OutlinedTextField(
                                value = secureServerPort.toString(),
                                onValueChange = { settingsViewModel.onSecureServerPortChange(it) },
                                label = { Text("Secure Server Port (WSS)") },
                                modifier = Modifier.weight(1f),
                                enabled = !isServerRunning
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // --- Encryption Setting ---
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enable Encryption (WSS)", modifier = Modifier.weight(1f))
                            Checkbox(
                                checked = encryptionEnabled,
                                onCheckedChange = { desktopViewModel.setEncryption(it) },
                                enabled = !isServerRunning
                            )
                        }
                        Text(
                            text = "Requires a restart of the server to apply.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                        // --- Device Management ---
                        Text("Trusted Devices", style = MaterialTheme.typography.titleMedium)
                        if (trustedDevices.isEmpty()) {
                            Text("No trusted devices.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                        } else {
                            trustedDevices.forEach { (id, name) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, style = MaterialTheme.typography.bodyLarge)
                                        Text(id, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row {
                                        TextButton(onClick = { desktopViewModel.unpairDevice(id) }) {
                                            Text("Unpair")
                                        }
                                        TextButton(onClick = { desktopViewModel.banDevice(id, name) }) {
                                            Text("Ban", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Banned Devices", style = MaterialTheme.typography.titleMedium)
                        if (bannedDevices.isEmpty()) {
                            Text("No banned devices.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                        } else {
                            bannedDevices.forEach { (id, name) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, style = MaterialTheme.typography.bodyLarge)
                                        Text(id, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    TextButton(onClick = { desktopViewModel.unbanDevice(id) }) {
                                        Text("Unban")
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // --- Close Button ---
                        Box(modifier = Modifier.fillMaxWidth()) {
                            IconButton(
                                onClick = onDismissRequest,
                                modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    }

                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(scrollState),
                        style = ScrollbarStyle(
                            minimalHeight = 16.dp,
                            thickness = 8.dp,
                            shape = MaterialTheme.shapes.small,
                            hoverDurationMillis = 300,
                            unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f)
                        )
                    )
                }
    }
}
