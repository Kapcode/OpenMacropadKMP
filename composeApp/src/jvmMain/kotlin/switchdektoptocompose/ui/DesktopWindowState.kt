package switchdektoptocompose.ui

import switchdektoptocompose.viewmodel.SettingsViewModel
import switchdektoptocompose.viewmodel.LayoutViewModel
import androidx.compose.runtime.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.*
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo

class DesktopWindowState(
    val windowState: WindowState,
    private val scope: CoroutineScope,
    private val settingsViewModel: SettingsViewModel,
    private val layoutViewModel: LayoutViewModel,
    private val onTrayMinimize: () -> Unit = {}
) {
    var isWindowVisible by mutableStateOf(true)
        private set
    var isTransitioning by mutableStateOf(false)
        private set
    private var animationJob: Job? = null

    // Track the user's intended placement (Maximized or Floating) 
    // to ensure we restore correctly after an animation or hide/show.
    private var preferredPlacement by mutableStateOf(WindowPlacement.Maximized)

    // Centralized dialog visibility states
    var showExitDialog by mutableStateOf(false)
    var showShortcutsDialog by mutableStateOf(false)
    var showSettingsDialog by mutableStateOf(false)
    var scrollToVariables by mutableStateOf(false)
    var scrollToSecurity by mutableStateOf(false)
    var showNewEventDialog by mutableStateOf(false)
    var showRecordDialog by mutableStateOf(false)
    var showUpdateConfirmDialog by mutableStateOf(false)
    var showMarketplace by mutableStateOf(false)
    var showLoggingWarning by mutableStateOf(false)
    var showAppInfo by mutableStateOf(false)

    // Secondary window states
    val shortcutsWindowState = WindowState(size = DpSize(500.dp, 550.dp))
    val settingsWindowState = WindowState(size = DpSize(600.dp, 700.dp))
    val marketplaceWindowState = WindowState(size = DpSize(900.dp, 700.dp))

    init {
        applyInitialPlacement()

        // Sync preferredPlacement with manual user interactions
        scope.launch {
            snapshotFlow { windowState.placement }
                .collect { placement ->
                    // Only update preferred placement if we're not currently in an animation transition
                    if (!isTransitioning && (placement == WindowPlacement.Maximized || placement == WindowPlacement.Floating)) {
                        preferredPlacement = placement
                    }
                }
        }

        scope.launch {
            layoutViewModel.showUpdateConfirmDialog.collect { show ->
                showUpdateConfirmDialog = show
            }
        }
        scope.launch {
            layoutViewModel.showExitDialog.collect { show ->
                showExitDialog = show
            }
        }
        scope.launch {
            layoutViewModel.showShortcutsDialog.collect { show ->
                if (show) {
                    shortcutsWindowState.position = calculateCenteredWindowPosition(shortcutsWindowState.size)
                    shortcutsWindowState.isMinimized = false
                }
                showShortcutsDialog = show
            }
        }
        scope.launch {
            layoutViewModel.showSettingsDialog.collect { show ->
                if (show) {
                    settingsWindowState.position = calculateCenteredWindowPosition(settingsWindowState.size)
                    settingsWindowState.isMinimized = false
                }
                showSettingsDialog = show
            }
        }
        scope.launch {
            layoutViewModel.scrollToVariables.collect { scroll ->
                scrollToVariables = scroll
            }
        }
        scope.launch {
            layoutViewModel.scrollToSecurity.collect { scroll ->
                scrollToSecurity = scroll
            }
        }
        scope.launch {
            layoutViewModel.showMarketplace.collect { show ->
                if (show) {
                    marketplaceWindowState.position = calculateCenteredWindowPosition(marketplaceWindowState.size)
                    marketplaceWindowState.isMinimized = false
                }
                showMarketplace = show
            }
        }
        scope.launch {
            layoutViewModel.showNewEventDialog.collect { show ->
                showNewEventDialog = show
            }
        }
        scope.launch {
            layoutViewModel.showRecordDialog.collect { show ->
                showRecordDialog = show
            }
        }
        scope.launch {
            layoutViewModel.showLoggingWarning.collect { show ->
                showLoggingWarning = show
            }
        }
        scope.launch {
            layoutViewModel.showAppInfo.collect { show ->
                showAppInfo = show
            }
        }
    }

    private fun applyInitialPlacement() {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screens = ge.screenDevices
        val mode = settingsViewModel.windowPlacementMode.value
        val index = settingsViewModel.windowMonitorIndex.value

        println("Applying initial placement. Mode: $mode, Index: $index, Screens detected: ${screens.size}")

        val targetBounds = when (mode) {
            "INDEX" -> {
                if (index >= 0 && index < screens.size) {
                    screens[index].defaultConfiguration.bounds
                } else ge.maximumWindowBounds
            }
            "CURSOR" -> {
                val mouseLoc = MouseInfo.getPointerInfo().location
                println("Cursor location: $mouseLoc")
                screens.find { it.defaultConfiguration.bounds.contains(mouseLoc) }?.defaultConfiguration?.bounds
                    ?: ge.maximumWindowBounds
            }
            else -> ge.maximumWindowBounds // PRIMARY
        }

        println("Target bounds for placement: $targetBounds")

        windowState.placement = WindowPlacement.Floating
        windowState.position = WindowPosition(targetBounds.x.dp, targetBounds.y.dp)
        windowState.size = DpSize(1200.dp, 800.dp)
    }

    fun calculateCenteredWindowPosition(dialogSize: DpSize): WindowPosition {
        val mainPos = (windowState.position as? WindowPosition.Absolute) ?: WindowPosition(0.dp, 0.dp)
        
        // Handle unspecified or zero sizes gracefully to avoid smearing/jitter during init
        val mainWidth = if (windowState.size.width.isSpecified && windowState.size.width > 0.dp) windowState.size.width else 1200.dp
        val mainHeight = if (windowState.size.height.isSpecified && windowState.size.height > 0.dp) windowState.size.height else 800.dp

        val centerX = mainPos.x + (mainWidth - dialogSize.width) / 2
        val centerY = mainPos.y + (mainHeight - dialogSize.height) / 2

        return WindowPosition(centerX, centerY)
    }

    fun toggleShortcuts(show: Boolean = !showShortcutsDialog) {
        layoutViewModel.setShowShortcutsDialog(show)
    }

    fun toggleSettings(show: Boolean = !showSettingsDialog, scrollToVariables: Boolean = false, scrollToSecurity: Boolean = false) {
        if (!show) {
            // Clear scroll flags when closing to ensure next open is fresh
            layoutViewModel.setShowSettingsDialog(false, scrollToVariables = false, scrollToSecurity = false)
        } else {
            layoutViewModel.setShowSettingsDialog(show, scrollToVariables, scrollToSecurity)
        }
    }

    fun toggleMarketplace(show: Boolean = !showMarketplace) {
        layoutViewModel.setShowMarketplace(show)
    }

    fun toggleNewEventDialog(show: Boolean = !showNewEventDialog) {
        layoutViewModel.setShowNewEventDialog(show)
    }

    fun toggleRecordDialog(show: Boolean = !showRecordDialog) {
        layoutViewModel.setShowRecordDialog(show)
    }

    fun toggleUpdateConfirmDialog(show: Boolean = !showUpdateConfirmDialog) {
        layoutViewModel.setShowUpdateConfirmDialog(show)
    }

    fun toggleExitDialog(show: Boolean = !showExitDialog) {
        layoutViewModel.setShowExitDialog(show)
    }

    fun toggleLoggingWarning(show: Boolean = !showLoggingWarning) {
        layoutViewModel.setShowLoggingWarning(show)
    }

    fun toggleAppInfo(show: Boolean = !showAppInfo) {
        layoutViewModel.setShowAppInfo(show)
    }

    fun toggleWindow() {
        // If the window is hidden, or minimized, or we're not currently the active application
        // then show and bring to front.
        if (!isWindowVisible || windowState.isMinimized) {
            showWindow()
        } else {
            // Check if our window is actually the active one in the OS
            val isActive = java.awt.Window.getWindows().any { 
                (it as? java.awt.Frame)?.title == "MacroKap (Compose)" && (it.isFocused || it.isActive)
            }
            if (!isActive) {
                showWindow() // Bring to front if not active
            } else {
                animateToTray() // Hide if already active and visible
            }
        }
    }

    fun animateToTray() {
        if (!isWindowVisible) return
        
        onTrayMinimize()
        if (!settingsViewModel.animateToTray.value) {
            isWindowVisible = false
            return
        }

        if (isTransitioning) {
            // If already transitioning, don't start a new animation unless it's a reversal (handled in showWindow)
            return
        }
            
        animationJob = scope.launch {
            isTransitioning = true
            val initialPlacement = preferredPlacement
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
                    val eased = t * t 
                    
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
            } catch (e: CancellationException) {
                // Animation interrupted, will be handled by the new animation job
                throw e
            } finally {
                withContext(NonCancellable) {
                    // Restore state so it's correct when we next "Show"
                    windowState.placement = initialPlacement
                    windowState.size = initialSize
                    windowState.position = initialPosition
                    isTransitioning = false
                    animationJob = null
                }
            }
        }
    }

    fun showWindow() {
        val prevJob = animationJob
        animationJob = scope.launch {
            // Cancel and wait for previous animation to clean up its state
            prevJob?.cancelAndJoin()
            
            if (!settingsViewModel.animateToTray.value) {
                isWindowVisible = true
                windowState.isMinimized = false
                windowState.placement = preferredPlacement
                isTransitioning = false
                return@launch
            }

            try {
                isTransitioning = true
                val targetPlacement = preferredPlacement
                val targetSize = windowState.size
                val targetPos = (windowState.position as? WindowPosition.Absolute) ?: WindowPosition(0.dp, 0.dp)

                val screen = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
                val traySize = DpSize(200.dp, 100.dp)
                val trayX = (screen.width - 250).dp
                val trayY = (screen.height - 150).dp

                if (!isWindowVisible) {
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
                    val eased = 1f - (1f - t) * (1f - t)
                    
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
            } catch (e: CancellationException) {
                throw e
            } finally {
                withContext(NonCancellable) {
                    isWindowVisible = true
                    windowState.isMinimized = false
                    isTransitioning = false
                    // If we were cancelled, we don't null animationJob because the new job just set it
                }
            }
        }
    }
}

@Composable
fun rememberDesktopWindowState(
    windowState: WindowState = rememberWindowState(placement = WindowPlacement.Maximized),
    scope: CoroutineScope = rememberCoroutineScope(),
    settingsViewModel: SettingsViewModel,
    layoutViewModel: LayoutViewModel,
    onTrayMinimize: () -> Unit = {}
): DesktopWindowState {
    return remember(windowState, scope, settingsViewModel, layoutViewModel, onTrayMinimize) {
        DesktopWindowState(windowState, scope, settingsViewModel, layoutViewModel, onTrayMinimize)
    }
}
