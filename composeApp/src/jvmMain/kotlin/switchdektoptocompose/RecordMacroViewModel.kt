package switchdektoptocompose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class RecordMacroViewModel(
    private val macroManagerViewModel: MacroManagerViewModel
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Default)

    val recordKeys = MutableStateFlow(true)
    val recordMouseButtons = MutableStateFlow(true)
    val recordMouseMoves = MutableStateFlow(false)
    val recordMouseScroll = MutableStateFlow(true)
    val recordDelays = MutableStateFlow(true)
    val useAutoDelay = MutableStateFlow(false)
    val autoDelayMs = MutableStateFlow("50")

    val macroName = MutableStateFlow("New Recorded Macro")
    
    val useRecordingDuration = MutableStateFlow(true)
    val recordingDurationMs = MutableStateFlow("5000")
    
    val selectedStopKey = MutableStateFlow("F12")
    val availableStopKeys = (1..12).map { "F$it" } + "Escape"
    
    val validationState: StateFlow<Pair<Boolean, String>> = combine(
        macroName,
        macroManagerViewModel.macroFiles
    ) { name, existingMacros ->
        validate(name, existingMacros)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Pair(true, ""))

    init {
        reset()
    }

    private fun sanitizeName(name: String) = name.replace(Regex("[^a-zA-Z0-9_]"), "")

    private fun validate(name: String, existingMacros: List<MacroFileState>): Pair<Boolean, String> {
        if (name.isBlank()) {
            return false to "Macro name cannot be empty."
        }
        val sanitizedName = sanitizeName(name)
        val existingNames = existingMacros.map { sanitizeName(it.name) }
        if (existingNames.any { it.equals(sanitizedName, ignoreCase = true) }) {
            return false to "A macro with this name already exists."
        }
        return true to ""
    }

    fun reset() {
        recordKeys.value = true
        recordMouseButtons.value = true
        recordMouseMoves.value = false
        recordMouseScroll.value = true
        recordDelays.value = true
        useAutoDelay.value = false
        autoDelayMs.value = "50"
        useRecordingDuration.value = true
        recordingDurationMs.value = "5000"
        selectedStopKey.value = "F12"
        
        // Find a unique default name
        val baseName = "New Recorded Macro"
        val existingNames = macroManagerViewModel.macroFiles.value.map { sanitizeName(it.name) }
        var newName = baseName
        var counter = 1
        while (existingNames.any { it.equals(sanitizeName(newName), ignoreCase = true) }) {
            newName = "$baseName $counter"
            counter++
        }
        macroName.value = newName
    }
}