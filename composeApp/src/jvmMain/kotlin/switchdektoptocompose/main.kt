package switchdektoptocompose

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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
    
    // Pass listener back to serverViewModel
    remember(triggerListener, viewModels.serverViewModel) {
        viewModels.serverViewModel.triggerListener = triggerListener
        Unit
    }
    val inspectorManager = remember { 
        InspectorManager(
            inspectorViewModel, 
            consoleViewModel,
            viewModels.serverViewModel.processWatcher
        ) 
    }

    val clientCommunicationViewModel = viewModels.clientCommunicationViewModel
    val pendingPairingRequests by clientCommunicationViewModel.pendingPairingRequests.collectAsState()
    val icon = painterResource("macropadIcon512.png")

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
        viewModels.serverViewModel.controllerManager?.start()
        onDispose {
            desktopViewModel.shutdown()
            triggerListener.shutdown()
            inspectorManager.stopListening()
            viewModels.serverViewModel.controllerManager?.stop()
        }
    }
    
    val exitBehavior by settingsViewModel.exitBehavior.collectAsState()
    val clickTrayToToggle by settingsViewModel.clickTrayToToggle.collectAsState()
    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()
    
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
            Item("Bulk Settings Pusher", onClick = { desktopWindowState.togglePushSettings(true) })
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
            windowState = desktopWindowState.shortcutsWindowState
        )
    }

    if (desktopWindowState.showPushSettingsDialog) {
        switchdektoptocompose.ui.PushSettingsDialog(
            settingsViewModel = settingsViewModel,
            consoleViewModel = consoleViewModel,
            selectedTheme = selectedTheme,
            onDismissRequest = { desktopWindowState.showPushSettingsDialog = false },
            windowState = desktopWindowState.pushSettingsWindowState
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
            onShowPushSettingsRequest = { desktopWindowState.togglePushSettings(true) },
            windowState = desktopWindowState.settingsWindowState
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
