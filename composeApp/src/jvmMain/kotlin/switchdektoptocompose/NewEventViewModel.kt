package switchdektoptocompose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MacroAction { PRESS, RELEASE, `ON-PRESS`, `ON-RELEASE`, PRESS_THEN_RELEASE, TYPE }

class NewEventViewModel {
    private val viewModelScope = CoroutineScope(Dispatchers.Default)

    val isTriggerEvent = MutableStateFlow(false)
    val allowedClientsText = MutableStateFlow("")

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
    val animateMouseMovement = MutableStateFlow(false)
    val useDelay = MutableStateFlow(false)
    val delayText = MutableStateFlow("100")
    val delayBetweenActions = MutableStateFlow(false)
    val useAutoDelay = MutableStateFlow(false)
    val autoDelayText = MutableStateFlow("50")

    init {
        viewModelScope.launch {
            isTriggerEvent.collect { isTrigger ->
                if (isTrigger) {
                    selectedAction.value = MacroAction.`ON-RELEASE`
                    useKeys.value = true
                    useMouseButtons.value = false
                    useMouseScroll.value = false
                    useMouseLocation.value = false
                    useDelay.value = false
                    delayBetweenActions.value = false
                    useAutoDelay.value = false
                }
            }
        }
    }

    fun reset() {
        isTriggerEvent.value = false
        allowedClientsText.value = ""
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
        animateMouseMovement.value = false
        useDelay.value = false
        delayText.value = "100"
        delayBetweenActions.value = false
        useAutoDelay.value = false
        autoDelayText.value = "50"
    }

    fun createEvents(): List<MacroEventState> {
        val events = mutableListOf<MacroEventState>()
        
        if (isTriggerEvent.value) {
            // Trigger creation is now handled by the timeline VM directly
            // This function will only produce regular events
            return emptyList()
        }
        
        val autoDelay = if (useAutoDelay.value) autoDelayText.value.toLongOrNull() else null
        var addedAction = false

        if (useKeys.value && keysText.value.isNotBlank()) {
            addedAction = true
            when (selectedAction.value) {
                MacroAction.PRESS, MacroAction.`ON-PRESS` -> {
                    keysText.value.split(',').map { it.trim() }.forEach { events.add(MacroEventState.KeyEvent(it, KeyAction.PRESS)) }
                }
                MacroAction.RELEASE, MacroAction.`ON-RELEASE` -> {
                    keysText.value.split(',').map { it.trim() }.forEach { events.add(MacroEventState.KeyEvent(it, KeyAction.RELEASE)) }
                }
                MacroAction.PRESS_THEN_RELEASE -> {
                    val keys = keysText.value.split(',').map { it.trim() }
                    keys.forEach { events.add(MacroEventState.KeyEvent(it, KeyAction.PRESS)) }
                    if (delayBetweenActions.value) autoDelay?.let { events.add(MacroEventState.DelayEvent(it)) }
                    keys.reversed().forEach { events.add(MacroEventState.KeyEvent(it, KeyAction.RELEASE)) }
                }
                MacroAction.TYPE -> {
                    keysText.value.forEachIndexed { index, char ->
                        if (index > 0 && delayBetweenActions.value) autoDelay?.let { events.add(MacroEventState.DelayEvent(it)) }
                        events.add(MacroEventState.KeyEvent(char.toString(), KeyAction.PRESS))
                        events.add(MacroEventState.KeyEvent(char.toString(), KeyAction.RELEASE))
                    }
                }
            }
        }

        if (useMouseLocation.value) {
            if (addedAction && delayBetweenActions.value) autoDelay?.let { events.add(MacroEventState.DelayEvent(it)) }
            addedAction = true
            val x = mouseX.value.toIntOrNull() ?: 0
            val y = mouseY.value.toIntOrNull() ?: 0
            events.add(MacroEventState.MouseEvent(x, y, MouseAction.MOVE))
        }

        if (useDelay.value) {
            if (addedAction && delayBetweenActions.value) autoDelay?.let { events.add(MacroEventState.DelayEvent(it)) }
            delayText.value.toLongOrNull()?.let { events.add(MacroEventState.DelayEvent(it)) }
        }
        
        return events
    }
}