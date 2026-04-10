package switchdektoptocompose.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import javax.swing.SwingUtilities
import javax.swing.UIManager
import androidx.compose.ui.tooling.preview.Preview
import switchdektoptocompose.di.DesktopViewModels
import switchdektoptocompose.viewmodel.*
import switchdektoptocompose.viewmodel.SettingsViewModel as DesktopSettingsViewModel
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel as SharedSettingsViewModel
import switchdektoptocompose.di.ViewModelFactory

@Preview
@Composable
fun DesktopAppPreview() {
    val settingsViewModel = remember { DesktopSettingsViewModel() }
    val consoleViewModel = remember { ConsoleViewModel() }
    val inspectorViewModel = remember { InspectorViewModel(consoleViewModel) }
    val desktopViewModel = remember { DesktopViewModel(settingsViewModel, consoleViewModel) }
    val macroManagerViewModel = remember {
        MacroManagerViewModel(
            settingsViewModel = settingsViewModel,
            consoleViewModel = consoleViewModel,
            onEditMacroRequested = { },
            onMacrosUpdated = { }
        )
    }
    val recordMacroViewModel = remember { RecordMacroViewModel(macroManagerViewModel) }
    val macroEditorViewModel = remember { MacroEditorViewModel(settingsViewModel) { } }
    val macroTimelineViewModel = remember { MacroTimelineViewModel(macroEditorViewModel) }
    val sharedSettingsViewModel = remember { SharedSettingsViewModel() }
    val newEventViewModel = remember { NewEventViewModel() }

    val viewModels = DesktopViewModels(
        desktopViewModel = desktopViewModel,
        consoleViewModel = consoleViewModel,
        inspectorViewModel = inspectorViewModel,
        recordMacroViewModel = recordMacroViewModel,
        macroEditorViewModel = macroEditorViewModel,
        macroManagerViewModel = macroManagerViewModel,
        settingsViewModel = settingsViewModel,
        sharedSettingsViewModel = sharedSettingsViewModel,
        macroTimelineViewModel = macroTimelineViewModel,
        newEventViewModel = newEventViewModel
    )

    DesktopApp(
        viewModels = viewModels
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSplitPaneApi::class)
@Composable
fun DesktopApp(
    viewModels: DesktopViewModels,
    onExit: () -> Unit = {}
) {
    val desktopViewModel = viewModels.desktopViewModel
    val consoleViewModel = viewModels.consoleViewModel
    val inspectorViewModel = viewModels.inspectorViewModel
    val recordMacroViewModel = viewModels.recordMacroViewModel
    val macroEditorViewModel = viewModels.macroEditorViewModel
    val macroManagerViewModel = viewModels.macroManagerViewModel
    val settingsViewModel = viewModels.settingsViewModel
    val sharedSettingsViewModel = viewModels.sharedSettingsViewModel
    val macroTimelineViewModel = viewModels.macroTimelineViewModel
    val newEventViewModel = viewModels.newEventViewModel

    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()
    val allowOnceOnly by settingsViewModel.allowOnceOnly.collectAsState()
    val allowNewConnections by settingsViewModel.allowNewConnections.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(selectedTheme) {
        val laf = if (selectedTheme == "Dark Blue") FlatDarkLaf::class.java.name else FlatLightLaf::class.java.name
        UIManager.setLookAndFeel(laf)
        for (window in java.awt.Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window)
        }
    }
    
    val logs by consoleViewModel.logMessages.collectAsState()
    LaunchedEffect(logs) {
        if (logs.isNotEmpty()) {
            val lastLog = logs.last()
            if (lastLog.formatted.contains("MACRO FINISHED") || lastLog.formatted.contains("MACRO CANCELLED") || lastLog.formatted.contains("E-STOP") || lastLog.formatted.contains("DIALOG CLOSED")) {
                 snackbarHostState.showSnackbar(lastLog.formatted)
            }
        }
    }


    val connectedDevices by desktopViewModel.connectedDevices.collectAsState()
    val pendingPairingRequests by desktopViewModel.pendingPairingRequests.collectAsState()
    val isServerRunning by desktopViewModel.isServerRunning.collectAsState()
    val serverError by desktopViewModel.serverError.collectAsState()
    val serverIpAddress by desktopViewModel.serverIpAddress.collectAsState()
    val encryptionEnabled by desktopViewModel.encryptionEnabled.collectAsState()
    val isMacroExecutionEnabled by desktopViewModel.isMacroExecutionEnabled.collectAsState()
    val serverPort by settingsViewModel.serverPort.collectAsState()
    val secureServerPort by settingsViewModel.secureServerPort.collectAsState()
    val currentPort = if (encryptionEnabled) secureServerPort else serverPort
    val filePendingDeletion by macroManagerViewModel.filePendingDeletion.collectAsState()
    val filesPendingDeletion by macroManagerViewModel.filesPendingDeletion.collectAsState()
    val eStopKey by settingsViewModel.eStopKey.collectAsState()
    val showLoggingWarning by consoleViewModel.showLoggingWarning.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSettingsToSecurity by remember { mutableStateOf(false) }
    var showNewEventDialog by remember { mutableStateOf(false) }
    var showRecordDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        val sharedSettingsViewModel = viewModels.sharedSettingsViewModel
        SettingsDialog(
            desktopViewModel = desktopViewModel,
            settingsViewModel = settingsViewModel,
            sharedSettingsViewModel = sharedSettingsViewModel,
            consoleViewModel = consoleViewModel,
            onDismissRequest = { 
                showSettingsDialog = false
                showSettingsToSecurity = false 
            },
            initialScrollToSecurity = showSettingsToSecurity
        )
    }

    if (showLoggingWarning) {
        LoggingToFileWarningDialog(
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            onConfirm = { consoleViewModel.confirmLoggingToFile() },
            onDismiss = { consoleViewModel.dismissLoggingWarning() }
        )
    }

    filePendingDeletion?.let { file ->
        ConfirmDeleteDialog(
            file = file,
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            onConfirm = { macroManagerViewModel.confirmDeletion() },
            onDismiss = { macroManagerViewModel.cancelDeletion() }
        )
    }
    filesPendingDeletion?.let { files ->
        ConfirmDeleteMultipleDialog(
            files = files,
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            onConfirm = { macroManagerViewModel.confirmMultipleDeletion() },
            onDismiss = { macroManagerViewModel.cancelMultipleDeletion() }
        )
    }
    if (showNewEventDialog) {
        NewEventDialog(
            viewModel = newEventViewModel,
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            onDismissRequest = { showNewEventDialog = false },
            onAddEvent = {
                if (newEventViewModel.isTriggerEvent.value) {
                    macroTimelineViewModel.addOrUpdateTrigger(
                        keyName = newEventViewModel.keysText.value,
                        allowedClients = newEventViewModel.allowedClientsText.value
                    )
                } else {
                    val events = newEventViewModel.createEvents()
                    macroTimelineViewModel.addEvents(events)
                }
                showNewEventDialog = false
            }
        )
    }
    
    if (showRecordDialog) {
        RecordMacroDialog(
            viewModel = recordMacroViewModel,
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            onDismissRequest = { showRecordDialog = false },
            onStartRecording = {
                macroManagerViewModel.startRecording(recordMacroViewModel)
                showRecordDialog = false
            }
        )
    }

    if (pendingPairingRequests.isNotEmpty()) {
        PairingRequestDialog(
            requests = pendingPairingRequests,
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            desktopSettingsViewModel = settingsViewModel,
            sharedSettingsViewModel = sharedSettingsViewModel,
            isAlwaysAllowAvailable = !allowOnceOnly,
            onApprove = { id, name, persistent -> desktopViewModel.approveDevice(id, name, persistent) },
            onDeny = { id -> desktopViewModel.rejectDevice(id) },
            onBan = { id, name -> desktopViewModel.banDevice(id, name) },
            onCancelAll = { desktopViewModel.rejectAllPendingDevices() }
        )
    }

    serverError?.let { error ->
        ServerErrorDialog(
            error = error,
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            onResetIdentity = {
                desktopViewModel.clearServerError()
                desktopViewModel.startServer(forceRecreateKeystore = true)
            },
            onDismiss = { desktopViewModel.clearServerError() }
        )
    }



    AppTheme(useDarkTheme = selectedTheme == "Dark Blue") {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Surface(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                val rootVerticalSplitter = rememberSplitPaneState(initialPositionPercentage = 0.2f)
                val mainHorizontalSplitter = rememberSplitPaneState(initialPositionPercentage = 0.1f)

                VerticalSplitPane(splitPaneState = rootVerticalSplitter) {
                    first(minSize = 100.dp) {
                        Row(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var menuExpanded by remember { mutableStateOf(false) }
                            TooltipArea(tooltip = { Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp){ Text("Menu", modifier = Modifier.padding(4.dp)) } }, delayMillis = 0) {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Start Server") },
                                    onClick = { desktopViewModel.startServer(); menuExpanded = false },
                                    enabled = !isServerRunning,
                                    leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Stop Server") },
                                    onClick = { desktopViewModel.stopServer(); menuExpanded = false },
                                    enabled = isServerRunning,
                                    leadingIcon = { Icon(Icons.Default.Stop, null) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(text = { Text("Settings") }, onClick = { showSettingsDialog = true; menuExpanded = false }, leadingIcon = { Icon(Icons.Default.Settings, null) })
                                HorizontalDivider()
                                DropdownMenuItem(text = { Text("Exit") }, onClick = onExit, leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) })
                            }
                            
                            Spacer(Modifier.width(16.dp))

                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight().padding(8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Column {
                                    val statusColor = if (isServerRunning) {
                                        if (selectedTheme == "Dark Blue") Color.Green else Color(0xFF008000)
                                    } else {
                                        Color.Red
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Status: ${if (isServerRunning) "Running" else "Stopped"}", color = statusColor)
                                        Spacer(Modifier.width(12.dp))
                                        Text("Connected: ${connectedDevices.size}", style = MaterialTheme.typography.bodySmall)
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            "Macros: ${if (isMacroExecutionEnabled) "Enabled" else "Disabled"}", 
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isMacroExecutionEnabled) Color.Unspecified else Color.Red
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    TooltipArea(
                                        tooltip = {
                                            Surface(
                                                modifier = Modifier.padding(4.dp),
                                                shape = MaterialTheme.shapes.small,
                                                shadowElevation = 4.dp
                                            ) {
                                                Text("Security Status (Click to edit)", modifier = Modifier.padding(4.dp))
                                            }
                                        },
                                        delayMillis = 500
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), MaterialTheme.shapes.small)
                                                .clickable {
                                                    showSettingsToSecurity = true
                                                    showSettingsDialog = true
                                                }
                                                .padding(4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    if (encryptionEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = if (encryptionEnabled) statusColor else MaterialTheme.colorScheme.error
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "Security: ${if (encryptionEnabled) "Encrypted (WSS)" else "Unencrypted (WS)"}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (encryptionEnabled) statusColor else MaterialTheme.colorScheme.error
                                                )
                                            }
                                            Row {
                                                Text(
                                                    "One-Time Approvals ONLY: ${if (allowOnceOnly) "Yes" else "No"}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (allowOnceOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "Discovery: ${if (allowNewConnections) "On" else "Off"}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (allowNewConnections) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Address: $serverIpAddress:$currentPort", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            
                            TooltipArea(tooltip = { Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp){ Text("Emergency Stop Key", modifier = Modifier.padding(4.dp)) } }, delayMillis = 0) {
                                var eStopMenuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(onClick = { eStopMenuExpanded = true }) {
                                        Text("E-Stop: $eStopKey")
                                    }
                                    DropdownMenu(
                                        expanded = eStopMenuExpanded,
                                        onDismissRequest = { eStopMenuExpanded = false },
                                        modifier = Modifier.heightIn(max = 300.dp)
                                    ) {
                                        Box(modifier = Modifier.sizeIn(maxHeight = 300.dp)) {
                                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                                val fKeys = (1..12).map { "F$it" }
                                                fKeys.forEach { key ->
                                                    DropdownMenuItem(
                                                        text = { Text(key) },
                                                        onClick = {
                                                            settingsViewModel.setEStopKey(key)
                                                            eStopMenuExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            TooltipArea(tooltip = { Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp){ Text("Toggle Macro Execution", modifier = Modifier.padding(4.dp)) } }, delayMillis = 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Power, contentDescription = "Macros Enabled")
                                    Switch(
                                        checked = isMacroExecutionEnabled,
                                        onCheckedChange = { desktopViewModel.setMacroExecutionEnabled(it) }
                                    )
                                }
                            }
                            
                            Spacer(Modifier.width(16.dp))

                            Box(modifier = Modifier.weight(0.1f).fillMaxHeight()) {
                                InspectorScreen(viewModel = inspectorViewModel)
                            }
                        }
                    }
                    second(minSize = 200.dp) {
                        HorizontalSplitPane(splitPaneState = mainHorizontalSplitter) {
                            first(minSize = 250.dp) {
                               Column {
                                   Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
                                       ConnectedDevicesScreen(
                                           devices = connectedDevices,
                                           onDisconnect = { desktopViewModel.disconnectClient(it) },
                                           onUnpair = { desktopViewModel.unpairDevice(it) },
                                           onBan = { desktopViewModel.banDevice(it.id, it.name) }
                                       )
                                   }
                                   Box(modifier = Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
                                       Console(viewModel = consoleViewModel)
                                   }
                               }
                            }
                            second(minSize = 500.dp) {
        MacroEditingArea(
            macroManagerViewModel = macroManagerViewModel,
            macroEditorViewModel = macroEditorViewModel,
            macroTimelineViewModel = macroTimelineViewModel,
            settingsViewModel = settingsViewModel,
            consoleViewModel = consoleViewModel,
            selectedTheme = selectedTheme,
            onAddEventClicked = {
                                        newEventViewModel.reset()
                                        showNewEventDialog = true
                                    },
                                    onRecordMacroClicked = {
                                        recordMacroViewModel.reset()
                                        showRecordDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
