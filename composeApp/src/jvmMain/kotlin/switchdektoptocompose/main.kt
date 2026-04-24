package switchdektoptocompose

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.*
import com.formdev.flatlaf.FlatDarkLaf
import switchdektoptocompose.di.ViewModelFactory
import switchdektoptocompose.logic.InspectorManager
import switchdektoptocompose.logic.TriggerListener
import switchdektoptocompose.ui.DesktopApp
import switchdektoptocompose.ui.DesktopWindowState
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
        onTrayMinimize = { desktopViewModel.rejectAllPendingDevices() }
    )
    
    val settingsViewModel = viewModels.settingsViewModel
    val consoleViewModel = viewModels.consoleViewModel
    val inspectorViewModel = viewModels.inspectorViewModel
    val macroManagerViewModel = viewModels.macroManagerViewModel
    val triggerListener = remember {
        TriggerListener(desktopViewModel) { macroToPlay ->
            macroManagerViewModel.onPlayMacro(macroToPlay)
        }
    }
    val inspectorManager = remember { 
        InspectorManager(
            inspectorViewModel, 
            consoleViewModel,
            viewModels.serverViewModel.processWatcher
        ) 
    }

    var showExitDialog by remember { mutableStateOf(false) }
    var showShortcutsDialog by remember { mutableStateOf(false) }
    var showPushSettingsDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val exitBehavior by settingsViewModel.exitBehavior.collectAsState()
    val clickTrayToToggle by settingsViewModel.clickTrayToToggle.collectAsState()
    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()
    val clientCommunicationViewModel = viewModels.clientCommunicationViewModel
    val pendingPairingRequests by clientCommunicationViewModel.pendingPairingRequests.collectAsState()
    val icon = painterResource("macropadIcon512.png")

    // Update triggers in the application scope so they stay active even when window is hidden
    val macroFiles by macroManagerViewModel.macroFiles.collectAsState()
    val eStopKey by settingsViewModel.eStopKey.collectAsState()
    val copyConsoleShortcut by settingsViewModel.copyConsoleOutputShortcut.collectAsState()
    val stopKeyShortcut by settingsViewModel.stopKeyShortcut.collectAsState()
    val inspectKeyShortcut by settingsViewModel.inspectKeyShortcut.collectAsState()

    LaunchedEffect(macroFiles, eStopKey, copyConsoleShortcut, stopKeyShortcut, inspectKeyShortcut) {
        triggerListener.updateActiveTriggers(
            macroFiles,
            eStopKey,
            copyConsoleShortcut,
            stopKeyShortcut,
            inspectKeyShortcut
        )
    }

    DisposableEffect(Unit) {
        desktopViewModel.startServer()
        triggerListener.startListening()
        inspectorManager.startListening()
        onDispose {
            desktopViewModel.shutdown()
            triggerListener.shutdown()
            inspectorManager.stopListening()
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
            Item("Shortcuts & Keymap", onClick = { showShortcutsDialog = true })
            Item("Bulk Settings Pusher", onClick = { showPushSettingsDialog = true })
            Separator()
            Item("Exit", onClick = {
                if (exitBehavior == "ASK") {
                    showExitDialog = true
                    desktopWindowState.showWindow()
                } else {
                    exitApplication()
                }
            })
        }
    )

    if (showShortcutsDialog) {
        switchdektoptocompose.ui.ShortcutsDialog(
            settingsViewModel = settingsViewModel,
            consoleViewModel = consoleViewModel,
            selectedTheme = selectedTheme,
            onDismissRequest = { showShortcutsDialog = false }
        )
    }

    if (showPushSettingsDialog) {
        switchdektoptocompose.ui.PushSettingsDialog(
            settingsViewModel = settingsViewModel,
            consoleViewModel = consoleViewModel,
            selectedTheme = selectedTheme,
            onDismissRequest = { showPushSettingsDialog = false }
        )
    }


    Window(
        visible = desktopWindowState.isWindowVisible,
        onCloseRequest = {
            when (exitBehavior) {
                "TRAY" -> desktopWindowState.animateToTray()
                "EXIT" -> exitApplication()
                else -> {
                    showExitDialog = true
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
            onExit = ::exitApplication,
            showExitDialog = showExitDialog,
            onShowExitDialogChange = { showExitDialog = it },
            showShortcutsDialog = showShortcutsDialog,
            onShowShortcutsDialogChange = { showShortcutsDialog = it },
            showPushSettingsDialog = showPushSettingsDialog,
            onShowPushSettingsDialogChange = { showPushSettingsDialog = it },
            showSettingsDialog = showSettingsDialog,
            onShowSettingsDialogChange = { showSettingsDialog = it }
        )
    }
}
