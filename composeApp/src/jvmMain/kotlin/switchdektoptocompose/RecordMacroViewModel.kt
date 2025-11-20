package switchdektoptocompose

import kotlinx.coroutines.flow.MutableStateFlow

class RecordMacroViewModel {
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
    
    fun reset() {
        recordKeys.value = true
        recordMouseButtons.value = true
        recordMouseMoves.value = false
        recordMouseScroll.value = true
        recordDelays.value = true
        useAutoDelay.value = false
        autoDelayMs.value = "50"
        macroName.value = "New Recorded Macro"
        useRecordingDuration.value = true
        recordingDurationMs.value = "5000"
        selectedStopKey.value = "F12"
    }
}