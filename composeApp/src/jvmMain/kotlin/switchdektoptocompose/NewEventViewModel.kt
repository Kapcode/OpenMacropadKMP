package switchdektoptocompose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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
    // Removed: delayBetweenActions
    val useAutoDelay = MutableStateFlow(false)
    val autoDelayText = MutableStateFlow("50")

    val validationState: StateFlow<Pair<Boolean, String>> = combine(
        isTriggerEvent, allowedClientsText,
        useKeys, keysText,
        useMouseButtons, mouseButtonsText,
        useMouseScroll, mouseScrollText,
        useMouseLocation, mouseX, mouseY,
        useDelay, delayText,
        useAutoDelay, autoDelayText
    ) { args ->
        validate()
    }.stateIn(viewModelScope, SharingStarted.Lazily, Pair(false, "Initializing..."))

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
                    // Removed: delayBetweenActions.value = false
                    useAutoDelay.value = false
                }
            }
        }
    }

    private fun validate(): Pair<Boolean, String> {
        if (isTriggerEvent.value) {
             // Trigger event is handled by TimelineViewModel directly, but if we were creating it here...
             // Actually, the dialog calls addOrUpdateTrigger if isTrigger is true.
             // We should validate trigger fields if isTrigger is true.
             if (keysText.value.isBlank()) return false to "Trigger key cannot be empty."
             return true to ""
        }

        var hasAction = false

        if (useKeys.value) {
            if (keysText.value.isBlank()) return false to "Key field is checked but empty."
            hasAction = true
        }

        if (useMouseButtons.value) {
            if (mouseButtonsText.value.isBlank()) return false to "Mouse Button field is checked but empty."
            // Validate comma separated numbers
            val parts = mouseButtonsText.value.split(',')
            for (part in parts) {
                if (part.trim().toIntOrNull() == null) {
                    return false to "Mouse Button field must contain numbers separated by commas (e.g., 1,3)."
                }
            }
            hasAction = true
        }

        if (useMouseScroll.value) {
            if (mouseScrollText.value.isBlank()) return false to "Mouse Scroll field is checked but empty."
            if (mouseScrollText.value.toIntOrNull() == null) return false to "Mouse Scroll must be a number."
            hasAction = true
        }

        if (useMouseLocation.value) {
            if (mouseX.value.toIntOrNull() == null || mouseY.value.toIntOrNull() == null) return false to "Mouse X and Y must be valid numbers."
            hasAction = true
        }

        if (useDelay.value) {
            if (delayText.value.toLongOrNull() == null) return false to "Delay must be a valid number."
            hasAction = true
        }

        if (useAutoDelay.value) {
            if (autoDelayText.value.toLongOrNull() == null) return false to "Auto Delay must be a valid number."
            hasAction = true
        }

        if (!hasAction) return false to "Please select at least one action."

        return true to ""
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
        // Removed: delayBetweenActions.value = false
        useAutoDelay.value = false
        autoDelayText.value = "50"
    }

    fun createEvents(): List<MacroEventState> {
        val events = mutableListOf<MacroEventState>()
        
        if (isTriggerEvent.value) {
            return emptyList()
        }
        
        val autoDelay = if (useAutoDelay.value) autoDelayText.value.toLongOrNull() else null

        // If "Auto Delay Declaration" is checked, emit a SetAutoWaitEvent first
        if (useAutoDelay.value && autoDelay != null) {
            events.add(MacroEventState.SetAutoWaitEvent(autoDelay.toInt()))
        }
        
        // Removed addedAction tracking logic for delayBetweenActions

        if (useKeys.value && keysText.value.isNotBlank()) {
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
                    // Removed implicit delay injection
                    keys.reversed().forEach { events.add(MacroEventState.KeyEvent(it, KeyAction.RELEASE)) }
                }
                MacroAction.TYPE -> {
                    keysText.value.forEachIndexed { index, char ->
                        // Removed implicit delay injection
                        events.add(MacroEventState.KeyEvent(char.toString(), KeyAction.PRESS))
                        events.add(MacroEventState.KeyEvent(char.toString(), KeyAction.RELEASE))
                    }
                }
            }
        }

        if (useMouseButtons.value && mouseButtonsText.value.isNotBlank()) {
             val buttons = mouseButtonsText.value.split(',').mapNotNull { it.trim().toIntOrNull() }
             if (buttons.isNotEmpty()) {
                when (selectedAction.value) {
                    MacroAction.PRESS, MacroAction.`ON-PRESS` -> {
                        buttons.forEach { btn -> events.add(MacroEventState.MouseButtonEvent(btn, KeyAction.PRESS)) }
                    }
                    MacroAction.RELEASE, MacroAction.`ON-RELEASE` -> {
                        buttons.forEach { btn -> events.add(MacroEventState.MouseButtonEvent(btn, KeyAction.RELEASE)) }
                    }
                    MacroAction.PRESS_THEN_RELEASE -> {
                        buttons.forEach { btn -> events.add(MacroEventState.MouseButtonEvent(btn, KeyAction.PRESS)) }
                        buttons.reversed().forEach { btn -> events.add(MacroEventState.MouseButtonEvent(btn, KeyAction.RELEASE)) }
                    }
                    MacroAction.TYPE -> {
                        // For mouse, TYPE behaves same as PRESS_THEN_RELEASE for each button individually? 
                        // Or sequentially? For keys, TYPE is press/release char by char.
                        // Let's do button by button.
                        buttons.forEach { btn ->
                            events.add(MacroEventState.MouseButtonEvent(btn, KeyAction.PRESS))
                            events.add(MacroEventState.MouseButtonEvent(btn, KeyAction.RELEASE))
                        }
                    }
                }
             }
        }

        if (useMouseScroll.value && mouseScrollText.value.isNotBlank()) {
            val scrollAmount = mouseScrollText.value.toIntOrNull()
            if (scrollAmount != null) {
                events.add(MacroEventState.ScrollEvent(scrollAmount))
            }
        }

        if (useMouseLocation.value) {
            val x = mouseX.value.toIntOrNull() ?: 0
            val y = mouseY.value.toIntOrNull() ?: 0
            events.add(MacroEventState.MouseEvent(x, y, MouseAction.MOVE, animateMouseMovement.value))
        }

        if (useDelay.value) {
            delayText.value.toLongOrNull()?.let { events.add(MacroEventState.DelayEvent(it)) }
        }
        
        return events
    }
}