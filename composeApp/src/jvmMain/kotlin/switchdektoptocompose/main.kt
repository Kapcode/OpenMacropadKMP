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
import switchdektoptocompose.ui.MinimizeToTrayDialog
import switchdektoptocompose.ui.rememberDesktopWindowState
import javax.swing.UIManager

@OptIn(ExperimentalMaterial3Api::class)
fun main() = application {
    // Set the initial Look and Feel
    UIManager.setLookAndFeel(FlatDarkLaf())

    val viewModels = ViewModelFactory.createViewModels()
    val desktopWindowState = rememberDesktopWindowState(settingsViewModel = viewModels.settingsViewModel)
    
    val settingsViewModel = viewModels.settingsViewModel
    val desktopViewModel = viewModels.desktopViewModel
    val consoleViewModel = viewModels.consoleViewModel
    val inspectorViewModel = viewModels.inspectorViewModel
    val macroManagerViewModel = viewModels.macroManagerViewModel
    val triggerListener = remember {
        TriggerListener(desktopViewModel) { macroToPlay ->
            macroManagerViewModel.onPlayMacro(macroToPlay)
        }
    }
    val inspectorManager = remember { InspectorManager(inspectorViewModel, consoleViewModel) }

    var showMinimizeToTrayDialog by remember { mutableStateOf(false) }
    val minimizeToTray by settingsViewModel.minimizeToTray.collectAsState()
    val showMinimizeToTrayDialogSetting by settingsViewModel.showMinimizeToTrayDialog.collectAsState()
    val clickTrayToToggle by settingsViewModel.clickTrayToToggle.collectAsState()
    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()
    val icon = painterResource("macropadIcon512.png")

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
            Separator()
            Item("Exit", onClick = ::exitApplication)
        }
    )

    if (showMinimizeToTrayDialog) {
        MinimizeToTrayDialog(
            selectedTheme = selectedTheme,
            onConfirm = { dontShowAgain ->
                if (dontShowAgain) {
                    settingsViewModel.setShowMinimizeToTrayDialog(false)
                }
                showMinimizeToTrayDialog = false
                desktopWindowState.animateToTray()
            },
            onDismiss = { showMinimizeToTrayDialog = false }
        )
    }

    Window(
        visible = desktopWindowState.isWindowVisible,
        onCloseRequest = {
            if (minimizeToTray) {
                if (showMinimizeToTrayDialogSetting) {
                    showMinimizeToTrayDialog = true
                } else {
                    desktopWindowState.animateToTray()
                }
            } else {
                exitApplication()
            }
        },
        state = desktopWindowState.windowState,
        title = "Open Macropad (Compose)",
        icon = icon
    ) {
        val macroFiles by macroManagerViewModel.macroFiles.collectAsState()
        val eStopKey by settingsViewModel.eStopKey.collectAsState()

        LaunchedEffect(macroFiles, eStopKey) {
            triggerListener.updateActiveTriggers(macroFiles, eStopKey)
        }

        DesktopApp(
            viewModels = viewModels,
            onExit = ::exitApplication
        )
    }
}
