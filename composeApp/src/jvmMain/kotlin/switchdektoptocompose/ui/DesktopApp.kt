package switchdektoptocompose.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
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
import java.io.File
import androidx.compose.ui.tooling.preview.Preview
import switchdektoptocompose.logic.ProcessWatcher
import switchdektoptocompose.di.DesktopViewModels
import switchdektoptocompose.viewmodel.*
import switchdektoptocompose.ui.components.AppTooltipArea
import switchdektoptocompose.ui.components.LocalTooltipSettings
import switchdektoptocompose.ui.components.TooltipSettings
import switchdektoptocompose.viewmodel.SettingsViewModel as DesktopSettingsViewModel
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel as SharedSettingsViewModel
import switchdektoptocompose.di.ViewModelFactory

@Preview
@Composable
fun DesktopAppPreview() {
    val settingsViewModel = remember { DesktopSettingsViewModel() }
    val consoleViewModel = remember { ConsoleViewModel() }
    val scope = rememberCoroutineScope()
    val processWatcher = remember { ProcessWatcher(scope) }
    val inspectorViewModel = remember { InspectorViewModel(consoleViewModel, processWatcher) }
    val clientCommunicationViewModel = remember { ClientCommunicationViewModel(settingsViewModel, consoleViewModel) }
    val serverViewModel = remember {
        ServerViewModel(
            settingsViewModel = settingsViewModel,
            consoleViewModel = consoleViewModel,
            processWatcher = processWatcher,
            onMessageReceived = { clientId, dataModel -> clientCommunicationViewModel.onDataReceived(clientId, dataModel) },
            onClientConnected = { clientId, name -> clientCommunicationViewModel.onClientConnected(clientId, name) },
            onClientDisconnected = { clientId -> clientCommunicationViewModel.onClientDisconnected(clientId) },
            onPairingRequest = { clientId, name -> clientCommunicationViewModel.onPairingRequest(clientId, name) }
        )
    }
    val desktopViewModel = remember {
        DesktopViewModel(
            settingsViewModel = settingsViewModel,
            consoleViewModel = consoleViewModel,
            inspectorViewModel = inspectorViewModel,
            serverViewModel = serverViewModel,
            clientCommunicationViewModel = clientCommunicationViewModel
        )
    }
    val macroManagerViewModel = remember {
        MacroManagerViewModel(
            settingsViewModel = settingsViewModel,
            consoleViewModel = consoleViewModel,
            onEditMacroRequested = { },
            onMacrosUpdated = { }
        )
    }
    // Wire up circular references for preview
    remember(macroManagerViewModel, serverViewModel, clientCommunicationViewModel) {
        clientCommunicationViewModel.macroManagerViewModel = macroManagerViewModel
        clientCommunicationViewModel.serverViewModel = serverViewModel
        desktopViewModel.macroManagerViewModel = macroManagerViewModel
        Unit
    }
    
    val recordMacroViewModel = remember { RecordMacroViewModel(macroManagerViewModel) }
    val macroEditorViewModel = remember { MacroEditorViewModel(settingsViewModel) { } }
    val macroTimelineViewModel = remember { MacroTimelineViewModel(macroEditorViewModel) }
    val sharedSettingsViewModel = remember { SharedSettingsViewModel() }
    val newEventViewModel = remember { NewEventViewModel() }
    val marketplaceViewModel = remember { MarketplaceViewModel(settingsViewModel, macroManagerViewModel) }
    val pairingViewModel = remember { PairingViewModel(settingsViewModel) }
    val layoutViewModel = remember { LayoutViewModel(settingsViewModel) }

    val viewModels = DesktopViewModels(
        desktopViewModel = desktopViewModel,
        serverViewModel = serverViewModel,
        clientCommunicationViewModel = clientCommunicationViewModel,
        consoleViewModel = consoleViewModel,
        inspectorViewModel = inspectorViewModel,
        recordMacroViewModel = recordMacroViewModel,
        macroEditorViewModel = macroEditorViewModel,
        macroManagerViewModel = macroManagerViewModel,
        settingsViewModel = settingsViewModel,
        sharedSettingsViewModel = sharedSettingsViewModel,
        macroTimelineViewModel = macroTimelineViewModel,
        newEventViewModel = newEventViewModel,
        marketplaceViewModel = marketplaceViewModel,
        pairingViewModel = pairingViewModel,
        layoutViewModel = layoutViewModel
    )

    val desktopWindowState = rememberDesktopWindowState(settingsViewModel = settingsViewModel)

    DesktopApp(
        viewModels = viewModels,
        desktopWindowState = desktopWindowState
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSplitPaneApi::class)
@Composable
fun DesktopApp(
    viewModels: DesktopViewModels,
    desktopWindowState: DesktopWindowState,
    onExit: () -> Unit = {},
    showExitDialog: Boolean = false,
    onShowExitDialogChange: (Boolean) -> Unit = {},
    showShortcutsDialog: Boolean = false,
    onShowShortcutsDialogChange: (Boolean) -> Unit = {},
    showPushSettingsDialog: Boolean = false,
    onShowPushSettingsDialogChange: (Boolean) -> Unit = {},
    showSettingsDialog: Boolean = false,
    onShowSettingsDialogChange: (Boolean) -> Unit = {}
) {
    val desktopViewModel = viewModels.desktopViewModel
    val serverViewModel = viewModels.serverViewModel
    val clientCommunicationViewModel = viewModels.clientCommunicationViewModel
    val consoleViewModel = viewModels.consoleViewModel
    val inspectorViewModel = viewModels.inspectorViewModel
    val recordMacroViewModel = viewModels.recordMacroViewModel
    val macroEditorViewModel = viewModels.macroEditorViewModel
    val macroManagerViewModel = viewModels.macroManagerViewModel
    val settingsViewModel = viewModels.settingsViewModel
    val sharedSettingsViewModel = viewModels.sharedSettingsViewModel
    val macroTimelineViewModel = viewModels.macroTimelineViewModel
    val newEventViewModel = viewModels.newEventViewModel
    val marketplaceViewModel = viewModels.marketplaceViewModel
    val layoutViewModel = viewModels.layoutViewModel

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


    val connectedDevices by clientCommunicationViewModel.connectedDevices.collectAsState()
    val pendingPairingRequests by clientCommunicationViewModel.pendingPairingRequests.collectAsState()
    val isServerRunning by serverViewModel.isServerRunning.collectAsState()
    val serverError by serverViewModel.serverError.collectAsState()
    val serverIpAddress by serverViewModel.serverIpAddress.collectAsState()
    val encryptionEnabled by serverViewModel.encryptionEnabled.collectAsState()
    val isMacroExecutionEnabled by clientCommunicationViewModel.isMacroExecutionEnabled.collectAsState()
    val connectionHistory by clientCommunicationViewModel.connectionHistory.collectAsState()
    val trustedDevices by clientCommunicationViewModel.trustedDevices.collectAsState()
    val totalCurrencySpent by clientCommunicationViewModel.totalCurrencySpent.collectAsState()

    val serverPort by settingsViewModel.serverPort.collectAsState()
    val secureServerPort by settingsViewModel.secureServerPort.collectAsState()
    val currentPort = if (encryptionEnabled) secureServerPort else serverPort
    val filePendingDeletion by macroManagerViewModel.filePendingDeletion.collectAsState()
    val filesPendingDeletion by macroManagerViewModel.filesPendingDeletion.collectAsState()
    val eStopKey by settingsViewModel.eStopKey.collectAsState()
    val showLoggingWarning by consoleViewModel.showLoggingWarning.collectAsState()
    
    val tooltipXOffset by settingsViewModel.tooltipXOffset.collectAsState()
    val tooltipYOffset by settingsViewModel.tooltipYOffset.collectAsState()

    val showNewEventDialog by layoutViewModel.showNewEventDialog.collectAsState()
    val showRecordDialog by layoutViewModel.showRecordDialog.collectAsState()
    val showExitDialogInternal by layoutViewModel.showExitDialogInternal.collectAsState()
    val showMarketplace by layoutViewModel.showMarketplace.collectAsState()
    val showExitDialogResolved = showExitDialog || showExitDialogInternal

    val exitBehavior by settingsViewModel.exitBehavior.collectAsState()
    // Using the desktopWindowState passed from main.kt

    if (showMarketplace) {
        AppTheme(useDarkTheme = selectedTheme == "Dark Blue") {
            MarketplaceScreen(
                viewModel = marketplaceViewModel,
                onBack = { layoutViewModel.setShowMarketplace(false) }
            )
        }
        return
    }

    if (showExitDialogResolved) {
        ExitConfirmDialog(
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            onExitNow = onExit,
            onExitToTray = {
                onShowExitDialogChange(false)
                layoutViewModel.setShowExitDialogInternal(false)
                desktopWindowState.animateToTray()
            },
            onDismiss = {
                onShowExitDialogChange(false)
                layoutViewModel.setShowExitDialogInternal(false)
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            desktopViewModel = desktopViewModel,
            settingsViewModel = settingsViewModel,
            sharedSettingsViewModel = sharedSettingsViewModel,
            consoleViewModel = consoleViewModel,
            onDismissRequest = { 
                onShowSettingsDialogChange(false)
            },
            onShowShortcutsRequest = {
                onShowSettingsDialogChange(false)
                onShowShortcutsDialogChange(true)
            },
            onShowPushSettingsRequest = {
                onShowSettingsDialogChange(false)
                onShowPushSettingsDialogChange(true)
            }
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
            onDismissRequest = { layoutViewModel.setShowNewEventDialog(false) },
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
                layoutViewModel.setShowNewEventDialog(false)
            }
        )
    }
    
    if (showRecordDialog) {
        RecordMacroDialog(
            viewModel = recordMacroViewModel,
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            onDismissRequest = { layoutViewModel.setShowRecordDialog(false) },
            onStartRecording = {
                macroManagerViewModel.startRecording(recordMacroViewModel)
                layoutViewModel.setShowRecordDialog(false)
            }
        )
    }

    if (pendingPairingRequests.isNotEmpty()) {
        PairingRequestDialog(
            requests = pendingPairingRequests,
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            pairingViewModel = viewModels.pairingViewModel,
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
        CompositionLocalProvider(
            LocalTooltipSettings provides TooltipSettings(xOffset = tooltipXOffset, yOffset = tooltipYOffset)
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                modifier = Modifier.fillMaxSize()
            ) { paddingValues ->
            Surface(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                val rootVerticalSplitter = rememberSplitPaneState(
                    initialPositionPercentage = layoutViewModel.getSplitterPosition("Root Layout", 0.0254f)
                )
                val mainHorizontalSplitter = rememberSplitPaneState(
                    initialPositionPercentage = layoutViewModel.getSplitterPosition("Main Horizontal", 0.2965f)
                )
                val secondaryPanelSplitter = rememberSplitPaneState(
                    initialPositionPercentage = layoutViewModel.getSplitterPosition("Secondary Panel", 0.1564f)
                )
                val sidebarSplitter = rememberSplitPaneState(
                    initialPositionPercentage = layoutViewModel.getSplitterPosition("Sidebar Split", 0.7073f)
                )

                MoveableVerticalSplitPane(
                    name = "Root Layout",
                    firstName = "Header",
                    secondName = "Main Content",
                    splitPaneState = rootVerticalSplitter,
                    consoleViewModel = consoleViewModel,
                    settingsViewModel = settingsViewModel,
                    firstMinSize = 48.dp,
                    secondMinSize = 200.dp,
                    first = {
                        DesktopTopBar(
                            desktopViewModel = desktopViewModel,
                            settingsViewModel = settingsViewModel,
                            isServerRunning = isServerRunning,
                            connectedDevicesCount = connectedDevices.size,
                            isMacroExecutionEnabled = isMacroExecutionEnabled,
                            serverIpAddress = serverIpAddress,
                            currentPort = currentPort,
                            encryptionEnabled = encryptionEnabled,
                            allowOnceOnly = allowOnceOnly,
                            allowNewConnections = allowNewConnections,
                            selectedTheme = selectedTheme,
                            exitBehavior = exitBehavior,
                            eStopKey = eStopKey,
                            onShowSettings = { onShowSettingsDialogChange(true) },
                            onShowShortcuts = { onShowShortcutsDialogChange(true) },
                            onShowPushSettings = { onShowPushSettingsDialogChange(true) },
                            onExit = onExit,
                            onShowExitDialog = { layoutViewModel.setShowExitDialogInternal(true) }
                        )
                    },
                    second = {
                        MoveableHorizontalSplitPane(
                            name = "Main Horizontal",
                            firstName = "Left Sidebar",
                            secondName = "Right Panel",
                            splitPaneState = mainHorizontalSplitter,
                            consoleViewModel = consoleViewModel,
                            settingsViewModel = settingsViewModel,
                            firstMinSize = 150.dp,
                            secondMinSize = 500.dp,
                            first = {
                                MoveableVerticalSplitPane(
                                    name = "Sidebar Split",
                                    firstName = "Console",
                                    secondName = "Inspector",
                                    splitPaneState = sidebarSplitter,
                                    consoleViewModel = consoleViewModel,
                                    settingsViewModel = settingsViewModel,
                                    firstMinSize = 100.dp,
                                    secondMinSize = 100.dp,
                                    first = {
                                        Box(
                                            modifier = Modifier.fillMaxSize()
                                                .background(panelBackground(1))
                                                .padding(8.dp)
                                        ) {
                                            Console(viewModel = consoleViewModel)
                                        }
                                    },
                                    second = {
                                        Box(
                                            modifier = Modifier.fillMaxSize()
                                                .background(panelBackground(2))
                                                .padding(8.dp)
                                        ) {
                                            InspectorScreen(viewModel = inspectorViewModel)
                                        }
                                    }
                                )
                            },
                            second = {
                                MoveableHorizontalSplitPane(
                                    name = "Secondary Panel",
                                    firstName = "Connections",
                                    secondName = "Macros",
                                    splitPaneState = secondaryPanelSplitter,
                                    consoleViewModel = consoleViewModel,
                                    settingsViewModel = settingsViewModel,
                                    firstMinSize = 250.dp,
                                    secondMinSize = 500.dp,
                                    first = {
                                        Box(modifier = Modifier.fillMaxSize().background(panelBackground(3)).padding(8.dp)) {
                                            ConnectedDevicesScreen(
                                                devices = connectedDevices,
                                                history = connectionHistory,
                                                trustedDevices = trustedDevices,
                                                totalCurrencySpent = totalCurrencySpent,
                                                onDisconnect = { desktopViewModel.disconnectClient(it) },
                                                onUnpair = { desktopViewModel.unpairDevice(it) },
                                                onBan = { desktopViewModel.banDevice(it.id, it.name) },
                                                onUnban = { desktopViewModel.unbanDevice(it) },
                                                onClearHistory = { desktopViewModel.clearConnectionHistory() }
                                            )
                                        }
                                    },
                                    second = {
                                        MacroEditingArea(
                                            macroManagerViewModel = macroManagerViewModel,
                                            macroEditorViewModel = macroEditorViewModel,
                                            macroTimelineViewModel = macroTimelineViewModel,
                                            settingsViewModel = settingsViewModel,
                                            consoleViewModel = consoleViewModel,
                                            selectedTheme = selectedTheme,
                                            onAddEventClicked = {
                                                newEventViewModel.reset()
                                                layoutViewModel.setShowNewEventDialog(true)
                                            },
                                            onRecordMacroClicked = {
                                                recordMacroViewModel.reset()
                                                layoutViewModel.setShowRecordDialog(true)
                                            },
                                            onMarketplaceClicked = {
                                                layoutViewModel.setShowMarketplace(true)
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }
                )
            }
        }
    }
}
}
