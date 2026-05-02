package switchdektoptocompose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.formdev.flatlaf.FlatDarkLaf
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme
import switchdektoptocompose.di.ViewModelFactory
import switchdektoptocompose.logic.InspectorManager
import switchdektoptocompose.logic.TriggerListener
import switchdektoptocompose.ui.DesktopApp
import switchdektoptocompose.ui.DesktopWindowState
import switchdektoptocompose.ui.MarketplaceScreen
import switchdektoptocompose.ui.rememberDesktopWindowState
import javax.swing.UIManager

object AppConfig {
    var isVerboseOutputEnabled: Boolean = false
}

@OptIn(ExperimentalMaterial3Api::class)
fun main(args: Array<String>) = application {
    AppConfig.isVerboseOutputEnabled = args.contains("-o") || args.contains("-output")

    // Set the initial Look and Feel
    UIManager.setLookAndFeel(FlatDarkLaf())

    val viewModels = ViewModelFactory.createViewModels()
    val desktopViewModel = viewModels.desktopViewModel
    val desktopWindowState = rememberDesktopWindowState(
        settingsViewModel = viewModels.settingsViewModel,
        layoutViewModel = viewModels.layoutViewModel,
        onTrayMinimize = { desktopViewModel.rejectAllPendingDevices() }
    )
    
    val settingsViewModel = viewModels.settingsViewModel
    val serverViewModel = viewModels.serverViewModel
    val macroTimelineViewModel = viewModels.macroTimelineViewModel
    val consoleViewModel = viewModels.consoleViewModel
    val inspectorViewModel = viewModels.inspectorViewModel
    val macroManagerViewModel = viewModels.macroManagerViewModel
    val triggerListener = remember {
        TriggerListener(desktopViewModel) { macroToPlay ->
            macroManagerViewModel.onPlayMacro(macroToPlay)
        }
    }
    
    // Pass listener back to serverViewModel
    remember(triggerListener, serverViewModel) {
        serverViewModel.triggerListener = triggerListener
        Unit
    }
    val inspectorManager = remember { 
        InspectorManager(
            inspectorViewModel, 
            consoleViewModel,
            serverViewModel.processWatcher
        ) 
    }

    val clientCommunicationViewModel = viewModels.clientCommunicationViewModel
    val pendingPairingRequests by clientCommunicationViewModel.pendingPairingRequests.collectAsState()
    val icon = painterResource("macropadIcon64.png")

    // Update triggers in the application scope so they stay active even when window is hidden
    val macroFiles by macroManagerViewModel.macroFiles.collectAsState()
    val macroPacks by macroManagerViewModel.macroPacks.collectAsState()
    val eStopKey by settingsViewModel.eStopKey.collectAsState()
    val copyConsoleShortcut by settingsViewModel.copyConsoleOutputShortcut.collectAsState()
    val stopKeyShortcut by settingsViewModel.stopKeyShortcut.collectAsState()
    val inspectKeyShortcut by settingsViewModel.inspectKeyShortcut.collectAsState()

    LaunchedEffect(macroFiles, macroPacks, eStopKey, copyConsoleShortcut, stopKeyShortcut, inspectKeyShortcut) {
        val routines = macroPacks.filter { it.pack.isActive }.flatMap { it.pack.routines }
        triggerListener.updateActiveTriggers(
            macroFiles,
            eStopKey,
            copyConsoleShortcut,
            stopKeyShortcut,
            inspectKeyShortcut,
            routines
        )
    }

    DisposableEffect(Unit) {
        desktopViewModel.startServer()
        triggerListener.startListening()
        inspectorManager.startListening()
        serverViewModel.controllerManager?.start()
        onDispose {
            desktopViewModel.shutdown()
            triggerListener.shutdown()
            inspectorManager.stopListening()
            serverViewModel.controllerManager?.stop()
        }
    }
    
    val exitBehavior by settingsViewModel.exitBehavior.collectAsState()
    val clickTrayToToggle by settingsViewModel.clickTrayToToggle.collectAsState()
    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()

    val activeToast by macroManagerViewModel.activeToast.collectAsState()
    val showLoggingWarning by consoleViewModel.showLoggingWarning.collectAsState()
    val pendingUpdate by clientCommunicationViewModel.pendingUpdate.collectAsState()
    val filePendingDeletion by macroManagerViewModel.filePendingDeletion.collectAsState()
    val filesPendingDeletion by macroManagerViewModel.filesPendingDeletion.collectAsState()
    val triggerPendingConfirmation by macroManagerViewModel.triggerPendingConfirmation.collectAsState()
    val serverError by serverViewModel.serverError.collectAsState()
    val allowOnceOnly by settingsViewModel.allowOnceOnly.collectAsState()

    LaunchedEffect(selectedTheme) {
        val laf = if (selectedTheme == "Dark Blue") com.formdev.flatlaf.FlatDarkLaf::class.java.name else com.formdev.flatlaf.FlatLightLaf::class.java.name
        UIManager.setLookAndFeel(laf)
        for (window in java.awt.Window.getWindows()) {
            javax.swing.SwingUtilities.updateComponentTreeUI(window)
        }
    }

    activeToast?.let { toastMsg ->
        Window(
            onCloseRequest = {},
            state = rememberWindowState(
                position = WindowPosition(Alignment.BottomCenter),
                width = 400.dp,
                height = 64.dp
            ),
            title = "Toast",
            transparent = true,
            undecorated = true,
            alwaysOnTop = true,
            focusable = false,
            resizable = false,
            icon = icon
        ) {
            AppTheme(useDarkTheme = selectedTheme == "Dark Blue") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = toastMsg,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    if (desktopWindowState.showMarketplace) {
        Window(
            onCloseRequest = { desktopWindowState.toggleMarketplace(false) },
            state = desktopWindowState.marketplaceWindowState,
            title = "Marketplace",
            icon = icon
        ) {
            AppTheme(useDarkTheme = selectedTheme == "Dark Blue") {
                MarketplaceScreen(
                    viewModel = viewModels.marketplaceViewModel,
                    onBack = { desktopWindowState.toggleMarketplace(false) }
                )
            }
        }
    }
    
    Tray(
        icon = icon,
        tooltip = "Open Macropad Server (Right-click for menu)",
        onAction = { 
            if (clickTrayToToggle) {
                desktopWindowState.toggleWindow()
            }
        },
        menu = {
            if (desktopWindowState.isWindowVisible && !desktopWindowState.windowState.isMinimized) {
                Item("Hide to Tray", onClick = { desktopWindowState.animateToTray() })
            } else {
                Item("Show Main Window", onClick = { desktopWindowState.showWindow() })
            }
            if (pendingPairingRequests.isNotEmpty()) {
                Separator()
                Item("Cancel All Sync Requests (${pendingPairingRequests.size})", onClick = { desktopViewModel.rejectAllPendingDevices() })
            }
            Separator()
            Item("Shortcuts & Keymap", onClick = { desktopWindowState.toggleShortcuts(true) })
            Item("Settings", onClick = { desktopWindowState.toggleSettings(true) })
            Separator()
            Item("Exit", onClick = {
                if (exitBehavior == "ASK") {
                    desktopWindowState.showExitDialog = true
                    desktopWindowState.showWindow()
                } else {
                    exitApplication()
                }
            })
        }
    )

    if (desktopWindowState.showShortcutsDialog) {
        switchdektoptocompose.ui.ShortcutsDialog(
            settingsViewModel = settingsViewModel,
            consoleViewModel = consoleViewModel,
            selectedTheme = selectedTheme,
            onDismissRequest = { desktopWindowState.showShortcutsDialog = false },
            windowState = desktopWindowState.shortcutsWindowState,
            icon = icon
        )
    }

    if (desktopWindowState.showSettingsDialog) {
        switchdektoptocompose.ui.SettingsDialog(
            desktopViewModel = desktopViewModel,
            settingsViewModel = settingsViewModel,
            sharedSettingsViewModel = viewModels.sharedSettingsViewModel,
            consoleViewModel = consoleViewModel,
            onDismissRequest = { desktopWindowState.showSettingsDialog = false },
            onShowShortcutsRequest = { desktopWindowState.toggleShortcuts(true) },
            windowState = desktopWindowState.settingsWindowState,
            icon = icon
        )
    }

    if (desktopWindowState.showExitDialog) {
        switchdektoptocompose.ui.ExitConfirmDialog(
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            onExitNow = { exitApplication() },
            onExitToTray = {
                desktopWindowState.toggleExitDialog(false)
                desktopWindowState.animateToTray()
            },
            onDismiss = {
                desktopWindowState.toggleExitDialog(false)
            }
        )
    }

    if (showLoggingWarning) {
        switchdektoptocompose.ui.LoggingToFileWarningDialog(
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            onConfirm = { consoleViewModel.confirmLoggingToFile() },
            onDismiss = { consoleViewModel.dismissLoggingWarning() }
        )
    }

    if (desktopWindowState.showUpdateConfirmDialog) {
        pendingUpdate?.let { update ->
            switchdektoptocompose.ui.UpdateConfirmDialog(
                selectedTheme = selectedTheme,
                consoleViewModel = consoleViewModel,
                clientName = update.clientName,
                isSimulation = update.isSimulation,
                onAccept = { clientCommunicationViewModel.approveUpdate() },
                onReject = { clientCommunicationViewModel.rejectUpdate() }
            )
        }
    }

    filePendingDeletion?.let { file ->
        switchdektoptocompose.ui.ConfirmDeleteDialog(
            file = file,
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            onConfirm = { macroManagerViewModel.confirmDeletion() },
            onDismiss = { macroManagerViewModel.cancelDeletion() }
        )
    }

    filesPendingDeletion?.let { files ->
        switchdektoptocompose.ui.ConfirmDeleteMultipleDialog(
            files = files,
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            onConfirm = { macroManagerViewModel.confirmMultipleDeletion() },
            onDismiss = { macroManagerViewModel.cancelMultipleDeletion() }
        )
    }

    if (desktopWindowState.showNewEventDialog) {
        switchdektoptocompose.ui.NewEventDialog(
            viewModel = viewModels.newEventViewModel,
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            onDismissRequest = { desktopWindowState.toggleNewEventDialog(false) },
            onAddEvent = {
                val newEventViewModel = viewModels.newEventViewModel
                val isTrigger = newEventViewModel.isTriggerEvent.value
                val isEdit = newEventViewModel.isEditMode.value
                val editIndex = newEventViewModel.editingIndex.value

                if (isTrigger) {
                    val allowedClients = if (newEventViewModel.isAllTrustedSelected.value) {
                        "ALL_TRUSTED"
                    } else {
                        (newEventViewModel.selectedClients.value +
                                newEventViewModel.allowedClientsText.value.split(',').filter { it.isNotBlank() })
                            .joinToString(",")
                    }

                    macroTimelineViewModel.addOrUpdateTrigger(
                        keyName = newEventViewModel.triggerKeysText.value,
                        allowedClients = allowedClients,
                        triggerType = newEventViewModel.triggerType.value,
                        holdDurationMs = newEventViewModel.holdDurationMs.value.toLongOrNull() ?: 500,
                        multiTapCount = newEventViewModel.multiTapCount.value.toIntOrNull() ?: 2,
                        tapWindowMs = newEventViewModel.tapWindowMs.value.toLongOrNull() ?: 300,
                        sequenceWindowMs = newEventViewModel.sequenceWindowMs.value.toLongOrNull() ?: 1000,
                        confirmationRequired = newEventViewModel.confirmationRequired.value
                    )
                } else {
                    val events = newEventViewModel.createEvents()
                    if (isEdit && editIndex != -1) {
                        events.firstOrNull()?.let {
                            macroTimelineViewModel.updateEvent(editIndex, it)
                        }
                    } else {
                        macroTimelineViewModel.addEvents(events)
                    }
                }
                desktopWindowState.toggleNewEventDialog(false)
            }
        )
    }

    if (desktopWindowState.showRecordDialog) {
        switchdektoptocompose.ui.RecordMacroDialog(
            viewModel = viewModels.recordMacroViewModel,
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            onDismissRequest = { desktopWindowState.toggleRecordDialog(false) },
            onStartRecording = {
                macroManagerViewModel.startRecording(viewModels.recordMacroViewModel)
                desktopWindowState.toggleRecordDialog(false)
            }
        )
    }

    if (pendingPairingRequests.isNotEmpty()) {
        switchdektoptocompose.ui.PairingRequestDialog(
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
        switchdektoptocompose.ui.ServerErrorDialog(
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

    triggerPendingConfirmation?.let { trigger ->
        AlertDialog(
            onDismissRequest = { macroManagerViewModel.cancelTrigger() },
            title = { Text("Confirm Trigger") },
            text = {
                Column {
                    Text("The following macro was triggered:")
                    Text(
                        trigger.macro?.name ?: trigger.routine?.name ?: "Unknown",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Trigger Keys: ${trigger.keyCodes}")
                    Text("Do you want to execute it?")
                }
            },
            confirmButton = {
                Button(onClick = { macroManagerViewModel.confirmTrigger() }) {
                    Text("Execute")
                }
            },
            dismissButton = {
                TextButton(onClick = { macroManagerViewModel.cancelTrigger() }) {
                    Text("Cancel")
                }
            }
        )
    }


    Window(
        visible = desktopWindowState.isWindowVisible,
        onCloseRequest = {
            when (exitBehavior) {
                "TRAY" -> desktopWindowState.animateToTray()
                "EXIT" -> exitApplication()
                else -> {
                    desktopWindowState.showExitDialog = true
                }
            }
        },
        state = desktopWindowState.windowState,
        title = "Open Macropad (Compose)",
        icon = icon
    ) {
        DesktopApp(
            viewModels = viewModels,
            desktopWindowState = desktopWindowState,
            onExit = ::exitApplication
        )
    }
}
