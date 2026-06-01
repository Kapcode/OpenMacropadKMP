package com.kapcode.open.macropad.kmps.desktop.viewmodel

import com.kapcode.open.macropad.kmps.desktop.model.MacroFileState
import com.kapcode.open.macropad.kmps.desktop.model.TriggerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class RecordMacroViewModel(
    private val macroManagerViewModel: MacroManagerViewModel,
    private val clientCommunicationViewModel: ClientCommunicationViewModel? = null
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

    val triggerKeysText = MutableStateFlow("ESCAPE")
    val triggerType = MutableStateFlow(TriggerType.RELEASE)
    val holdDurationMs = MutableStateFlow("500")
    val multiTapCount = MutableStateFlow("2")
    val tapWindowMs = MutableStateFlow("300")
    val sequenceWindowMs = MutableStateFlow("1000")
    val confirmationRequired = MutableStateFlow(false)

    val selectedClients = MutableStateFlow<Set<String>>(emptySet())
    val isAllTrustedSelected = MutableStateFlow(false)
    val trustedDevices = clientCommunicationViewModel?.trustedDevices ?: MutableStateFlow<Map<String, String>>(emptyMap())
    
    val validationState: StateFlow<Pair<Boolean, String>> = combine(
        macroName,
        macroManagerViewModel.macroFiles,
        triggerKeysText,
        triggerType,
        holdDurationMs,
        multiTapCount,
        tapWindowMs,
        sequenceWindowMs,
        isAllTrustedSelected
    ) { params: Array<Any?> ->
        val name = params[0] as String
        @Suppress("UNCHECKED_CAST")
        val existingMacros = params[1] as List<MacroFileState>
        val triggerKeys = params[2] as String
        val trigger = params[3] as TriggerType
        val hold = params[4] as String
        val tapCount = params[5] as String
        val window = params[6] as String
        val seqWindow = params[7] as String
        
        val nameValidation = validate(name, existingMacros)
        if (!nameValidation.first) return@combine nameValidation

        if (triggerKeys.isBlank()) return@combine false to "Trigger key(s) cannot be empty."

        when (trigger) {
            TriggerType.HOLD -> {
                if (hold.toLongOrNull() == null) return@combine false to "Hold duration must be a number."
            }
            TriggerType.MULTI_TAP -> {
                if (tapCount.toIntOrNull() == null) return@combine false to "Tap count must be a number."
                if (window.toLongOrNull() == null) return@combine false to "Tap window must be a number."
            }
            TriggerType.SEQUENCE -> {
                if (seqWindow.toLongOrNull() == null) return@combine false to "Sequence window must be a number."
            }
            else -> {}
        }
        true to ""
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
        triggerKeysText.value = "ESCAPE"
        triggerType.value = TriggerType.RELEASE
        holdDurationMs.value = "500"
        multiTapCount.value = "2"
        tapWindowMs.value = "300"
        sequenceWindowMs.value = "1000"
        confirmationRequired.value = false
        selectedClients.value = emptySet()
        isAllTrustedSelected.value = false
        
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