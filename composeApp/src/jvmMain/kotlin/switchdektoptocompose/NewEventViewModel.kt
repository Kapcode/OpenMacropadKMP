package switchdektoptocompose

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class MacroAction { PRESS, RELEASE, `ON-PRESS`, `ON-RELEASE` }

class NewEventViewModel {

    // --- UI State ---
    val isTriggerEvent = MutableStateFlow(false)

    val selectedAction = MutableStateFlow(MacroAction.PRESS)
    val actionOptions = MacroAction.values().toList()

    val useKeys = MutableStateFlow(true)
    val keysText = MutableStateFlow("")

    val useMouseButtons = MutableStateFlow(false)
    val mouseButtonsText = MutableStateFlow("")

    val useMouseScroll = MutableStateFlow(false)
    val mouseScrollText = MutableStateFlow("")

    val useMouseLocation = MutableStateFlow(false)
    val mouseX = MutableStateFlow("0")
    val mouseY = MutableStateFlow("0")

    val useDelay = MutableStateFlow(false)
    val delayText = MutableStateFlow("100")

    val delayBetweenActions = MutableStateFlow(false)

    /**
     * Resets all fields to their default state.
     */
    fun reset() {
        isTriggerEvent.value = false
        selectedAction.value = MacroAction.PRESS
        useKeys.value = true
        keysText.value = ""
        useMouseButtons.value = false
        mouseButtonsText.value = ""
        useMouseScroll.value = false
        mouseScrollText.value = ""
        useMouseLocation.value = false
        mouseX.value = "0"
        mouseY.value = "0"
        useDelay.value = false
        delayText.value = "100"
        delayBetweenActions.value = false
    }

    /**
     * Gathers all the UI state and generates a list of MacroEventState objects.
     */
    fun createEvents(): List<MacroEventState> {
        val events = mutableListOf<MacroEventState>()

        // For now, this is a placeholder.
        // We will add the complex parsing logic in a later step.
        println("--- Creating Events (Simulated) ---")
        if (useKeys.value && keysText.value.isNotBlank()) {
            println("Key Event: ${keysText.value}")
            events.add(MacroEventState.KeyEvent(keysText.value, KeyAction.PRESS))
        }
        if (useMouseLocation.value) {
            println("Mouse Move Event: ${mouseX.value}, ${mouseY.value}")
            events.add(MacroEventState.MouseEvent(mouseX.value.toIntOrNull() ?: 0, mouseY.value.toIntOrNull() ?: 0, MouseAction.MOVE))
        }
        if (useDelay.value) {
            println("Delay Event: ${delayText.value}")
            events.add(MacroEventState.DelayEvent(delayText.value.toLongOrNull() ?: 100))
        }
        println("------------------------------------")
        
        return events
    }
}