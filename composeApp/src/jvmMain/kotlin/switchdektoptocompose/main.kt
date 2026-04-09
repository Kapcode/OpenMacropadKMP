package switchdektoptocompose

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import java.awt.GraphicsEnvironment
import kotlinx.coroutines.*
import javax.swing.SwingUtilities
import javax.swing.UIManager

@OptIn(ExperimentalMaterial3Api::class)
fun main() = application {
    // Set the initial Look and Feel
    UIManager.setLookAndFeel(FlatDarkLaf())

    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
    val settingsViewModel = remember { SettingsViewModel() }
    val newEventViewModel = remember { NewEventViewModel() }
    val consoleViewModel = remember { ConsoleViewModel() }
    val inspectorViewModel = remember { InspectorViewModel(consoleViewModel) }
    val desktopViewModel = remember { DesktopViewModel(settingsViewModel, consoleViewModel) }
    
    val macroManagerViewModel = remember {
        MacroManagerViewModel(
            settingsViewModel = settingsViewModel,
            consoleViewModel = consoleViewModel,
            onEditMacroRequested = { /* Will be updated below */ },
            onMacrosUpdated = {
                desktopViewModel.sendMacroListToAllClients()
            }
        )
    }
    
    val recordMacroViewModel = remember { RecordMacroViewModel(macroManagerViewModel) }
    
    val macroEditorViewModel = remember {
        MacroEditorViewModel(settingsViewModel) {
            macroManagerViewModel.refresh()
        }
    }

    // Now that all VMs are created, we can set the circular dependencies
    macroManagerViewModel.onEditMacroRequested = { macroState ->
        macroEditorViewModel.openOrSwitchToTab(macroState)
    }
    desktopViewModel.macroManagerViewModel = macroManagerViewModel
    
    var isWindowVisible by remember { mutableStateOf(true) }
    var isTransitioning by remember { mutableStateOf(false) }
    var animationJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    
    var showMinimizeToTrayDialog by remember { mutableStateOf(false) }
    val minimizeToTray by settingsViewModel.minimizeToTray.collectAsState()
    val showMinimizeToTrayDialogSetting by settingsViewModel.showMinimizeToTrayDialog.collectAsState()
    val clickTrayToToggle by settingsViewModel.clickTrayToToggle.collectAsState()
    val animateToTraySetting by settingsViewModel.animateToTray.collectAsState()
    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()
    // Using the higher resolution icon to avoid white fringing artifacts
    val icon = painterResource("macropadIcon512.png")

    val animateToTray = {
        if (!isTransitioning && isWindowVisible) {
            if (!animateToTraySetting) {
                isWindowVisible = false
            } else {
                isTransitioning = true
                animationJob = scope.launch {
                    val initialPlacement = windowState.placement
                    val initialSize = windowState.size
                    val initialPosition = windowState.position
                    try {
                        if (windowState.placement == WindowPlacement.Maximized) {
                            windowState.placement = WindowPlacement.Floating
                            delay(100)
                        }

                        val startSize = windowState.size
                        val startPos = (windowState.position as? WindowPosition.Absolute) ?: WindowPosition(0.dp, 0.dp)
                        
                        val screen = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
                        val targetSize = DpSize(200.dp, 100.dp)
                        val targetX = (screen.width - 250).dp
                        val targetY = (screen.height - 150).dp

                        val steps = 20
                        for (i in 1..steps) {
                            val t = i.toFloat() / steps
                            val eased = t * t // quadratic ease-in
                            
                            windowState.size = DpSize(
                                startSize.width + (targetSize.width - startSize.width) * eased,
                                startSize.height + (targetSize.height - startSize.height) * eased
                            )
                            
                            windowState.position = WindowPosition(
                                startPos.x + (targetX - startPos.x) * eased,
                                startPos.y + (targetY - startPos.y) * eased
                            )
                            delay(16)
                        }
                        
                        isWindowVisible = false
                        delay(50)
                    } finally {
                        withContext(NonCancellable) {
                            windowState.placement = initialPlacement
                            windowState.size = initialSize
                            windowState.position = initialPosition
                            isTransitioning = false
                            animationJob = null
                        }
                    }
                }
            }
        }
    }

    val showWindow = {
        if (isTransitioning) {
            animationJob?.cancel()
        }
        
        if (!animateToTraySetting) {
            isWindowVisible = true
            windowState.isMinimized = false
            isTransitioning = false
            animationJob = null
        } else {
            isTransitioning = true
            animationJob = scope.launch {
                try {
                    val targetSize = windowState.size
                    val targetPos = (windowState.position as? WindowPosition.Absolute) ?: WindowPosition(0.dp, 0.dp)
                    val targetPlacement = windowState.placement

                    val screen = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
                    val traySize = DpSize(200.dp, 100.dp)
                    val trayX = (screen.width - 250).dp
                    val trayY = (screen.height - 150).dp

                    if (!isWindowVisible) {
                        // Starting from fully hidden, set to tray position first
                        windowState.placement = WindowPlacement.Floating
                        windowState.size = traySize
                        windowState.position = WindowPosition(trayX, trayY)
                        isWindowVisible = true
                    }
                    
                    windowState.isMinimized = false

                    val startSize = windowState.size
                    val startPos = (windowState.position as? WindowPosition.Absolute) ?: WindowPosition(0.dp, 0.dp)

                    val steps = 20
                    for (i in 1..steps) {
                        val t = i.toFloat() / steps
                        val eased = 1f - (1f - t) * (1f - t) // quadratic ease-out
                        
                        windowState.size = DpSize(
                            startSize.width + (targetSize.width - startSize.width) * eased,
                            startSize.height + (targetSize.height - startSize.height) * eased
                        )
                        
                        windowState.position = WindowPosition(
                            startPos.x + (targetPos.x - startPos.x) * eased,
                            startPos.y + (targetPos.y - startPos.y) * eased
                        )
                        delay(16)
                    }
                    
                    windowState.placement = targetPlacement
                } finally {
                    withContext(NonCancellable) {
                        isWindowVisible = true
                        windowState.isMinimized = false
                        isTransitioning = false
                        animationJob = null
                    }
                }
            }
        }
    }


    val macroTimelineViewModel = remember { MacroTimelineViewModel(macroEditorViewModel) }

    val triggerListener = remember {
        TriggerListener(desktopViewModel) { macroToPlay ->
            macroManagerViewModel.onPlayMacro(macroToPlay)
        }
    }

    val inspectorManager = remember { InspectorManager(inspectorViewModel, consoleViewModel) }

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
                val isMinimized = windowState.isMinimized
                if (isWindowVisible && !isMinimized && !isTransitioning) {
                    animateToTray()
                } else {
                    showWindow()
                }
            }
        },
        menu = {
            if (isWindowVisible && !windowState.isMinimized) {
                Item("Hide to Tray", onClick = { animateToTray() })
            } else {
                Item("Show Main Window", onClick = { showWindow() })
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
                animateToTray()
            },
            onDismiss = { showMinimizeToTrayDialog = false }
        )
    }

    Window(
        visible = isWindowVisible,
        onCloseRequest = {
            if (minimizeToTray) {
                if (showMinimizeToTrayDialogSetting) {
                    showMinimizeToTrayDialog = true
                } else {
                    animateToTray()
                }
            } else {
                exitApplication()
            }
        },
        state = windowState,
        title = "Open Macropad (Compose)",
        icon = icon
    ) {
            val macroFiles by macroManagerViewModel.macroFiles.collectAsState()
            val eStopKey by settingsViewModel.eStopKey.collectAsState()

            LaunchedEffect(macroFiles, eStopKey) {
                triggerListener.updateActiveTriggers(macroFiles, eStopKey)
            }

            val viewModels = DesktopViewModels(
                desktopViewModel = desktopViewModel,
                consoleViewModel = consoleViewModel,
                inspectorViewModel = inspectorViewModel,
                recordMacroViewModel = recordMacroViewModel,
                macroEditorViewModel = macroEditorViewModel,
                macroManagerViewModel = macroManagerViewModel,
                settingsViewModel = settingsViewModel,
                macroTimelineViewModel = macroTimelineViewModel,
                newEventViewModel = newEventViewModel
            )

            DesktopApp(
                viewModels = viewModels,
                onExit = ::exitApplication
            )
        }
    }
