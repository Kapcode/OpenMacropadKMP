package switchdektoptocompose.logic

import com.studiohartman.jamepad.ControllerManager
import com.studiohartman.jamepad.ControllerState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import switchdektoptocompose.viewmodel.MacroManagerViewModel
import switchdektoptocompose.viewmodel.SettingsViewModel
import java.awt.Robot
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

class ControllerManager(
    private val settingsViewModel: SettingsViewModel,
    private val macroManagerViewModel: MacroManagerViewModel,
    private val layoutViewModel: switchdektoptocompose.viewmodel.LayoutViewModel
) {
    private val jamepad = ControllerManager()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val robot = Robot()
    private var lastState: ControllerState? = null

    // Track a virtual cursor for in-app navigation
    private var virtualX = 0f
    private var virtualY = 0f

    fun start() {
        if (pollingJob != null) return
        
        jamepad.initSDLGamepad()
        
        pollingJob = scope.launch {
            while (isActive) {
                jamepad.update()
                val state = jamepad.getState(0)
                _isConnected.value = state.isConnected
                
                if (state.isConnected) {
                    processInput(state)
                    layoutViewModel.setVirtualCursorVisible(settingsViewModel.controllerNavigationMode.value == "CURSOR")
                } else {
                    layoutViewModel.setVirtualCursorVisible(false)
                }
                
                lastState = state
                delay(16) // ~60fps polling
            }
        }
    }

    private fun processInput(state: ControllerState) {
        val mode = settingsViewModel.controllerNavigationMode.value
        
        if (mode == "CURSOR") {
            handleCursorMode(state)
        } else {
            handleTraversalMode(state)
        }
        
        // Handle Automation Triggers (Global)
        checkAutomationTriggers(state)
    }

    private fun handleCursorMode(state: ControllerState) {
        // Sensitivity multipliers
        val sensitivity = 15f
        val dx = state.leftStickX * sensitivity
        val dy = state.leftStickY * -sensitivity // SDL Y is inverted
        
        if (Math.abs(dx) > 1f || Math.abs(dy) > 1f) {
            virtualX += dx
            virtualY += dy
            
            // Limit to screen bounds (approximate or window bounds)
            // For now just update the shared state
            layoutViewModel.updateVirtualCursor(virtualX, virtualY)
            
            // Optionally also move system mouse
            val info = java.awt.MouseInfo.getPointerInfo()
            if (info != null) {
                val loc = info.location
                robot.mouseMove(loc.x + dx.toInt(), loc.y + dy.toInt())
            }
        }
        
        // A button to Click
        val lastA = lastState?.a ?: false
        if (state.a && !lastA) {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
        } else if (!state.a && lastA) {
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
        }
        
        // B button to Right Click
        val lastB = lastState?.b ?: false
        if (state.b && !lastB) {
            robot.mousePress(InputEvent.BUTTON3_DOWN_MASK)
            robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK)
        }
    }

    private fun handleTraversalMode(state: ControllerState) {
        val l = lastState
        
        // D-Pad or Left Stick to Tab/Shift-Tab
        val tabPressed = (state.dpadRight && !(l?.dpadRight ?: false)) || (state.leftStickX > 0.5f && (l?.leftStickX ?: 0f) <= 0.5f)
        val shiftTabPressed = (state.dpadLeft && !(l?.dpadLeft ?: false)) || (state.leftStickX < -0.5f && (l?.leftStickX ?: 0f) >= -0.5f)
        
        if (tabPressed) {
            robot.keyPress(KeyEvent.VK_TAB)
            robot.keyRelease(KeyEvent.VK_TAB)
        }
        if (shiftTabPressed) {
            robot.keyPress(KeyEvent.VK_SHIFT)
            robot.keyPress(KeyEvent.VK_TAB)
            robot.keyRelease(KeyEvent.VK_TAB)
            robot.keyRelease(KeyEvent.VK_SHIFT)
        }
        
        // A to Enter
        if (state.a && !(l?.a ?: false)) {
            robot.keyPress(KeyEvent.VK_ENTER)
            robot.keyRelease(KeyEvent.VK_ENTER)
        }
    }

    private fun checkAutomationTriggers(state: ControllerState) {
        val l = lastState ?: return

        // Simple mapping of Jamepad buttons to our AST string names
        val buttonChecks = listOf(
            Triple("A", state.a, l.a),
            Triple("B", state.b, l.b),
            Triple("X", state.x, l.x),
            Triple("Y", state.y, l.y),
            Triple("LB", state.lb, l.lb),
            Triple("RB", state.rb, l.rb),
            Triple("START", state.start, l.start),
            Triple("BACK", state.back, l.back),
            Triple("L3", state.leftStickClick, l.leftStickClick),
            Triple("R3", state.rightStickClick, l.rightStickClick)
        )
        
        buttonChecks.forEach { (name, pressed, wasPressed) ->
            if (pressed && !wasPressed) {
                macroManagerViewModel.onControllerButtonTriggered(name, 0)
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        jamepad.quitSDLGamepad()
    }
}
