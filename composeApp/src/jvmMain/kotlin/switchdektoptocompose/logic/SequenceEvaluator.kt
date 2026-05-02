package switchdektoptocompose.logic

import com.kapcode.open.macropad.kmps.models.*
import kotlinx.coroutines.*
import switchdektoptocompose.model.*
import switchdektoptocompose.viewmodel.DesktopViewModel
import java.util.concurrent.ConcurrentHashMap

data class UnifiedTrigger(
    val id: String,
    val keyCodes: List<Int>,
    val triggerType: TriggerType,
    val durationMs: Long = 0,
    val tapCount: Int = 0,
    val windowMs: Long = 0,
    val confirmationRequired: Boolean = false,
    val targetProcess: String? = null,
    val routine: AutomationRoutine? = null,
    val macro: MacroFileState? = null
)

class SequenceEvaluator(
    private val viewModel: DesktopViewModel,
    private val onTriggerRoutine: (AutomationRoutine) -> Unit,
    private val onTriggerMacro: (MacroFileState) -> Unit
) {
    private val pressedKeys = ConcurrentHashMap.newKeySet<Int>()
    private val tapHistory = ConcurrentHashMap<Int, MutableList<Long>>()
    private val sequenceHistory = mutableListOf<Int>()
    private val activeUnifiedTriggers = mutableListOf<UnifiedTrigger>()
    private val pendingHoldJobs = ConcurrentHashMap<String, Job>()
    private val evaluatorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun updateTriggers(triggers: List<UnifiedTrigger>) {
        activeUnifiedTriggers.clear()
        activeUnifiedTriggers.addAll(triggers)
        
        // Cancel all pending holds when triggers change
        pendingHoldJobs.values.forEach { it.cancel() }
        pendingHoldJobs.clear()
    }

    fun onKeyPressed(keyCode: Int) {
        val now = System.currentTimeMillis()
        
        // Filter auto-repeat for sequence history
        if (sequenceHistory.lastOrNull() != keyCode) {
            sequenceHistory.add(keyCode)
            if (sequenceHistory.size > 20) sequenceHistory.removeAt(0)
        }
        
        pressedKeys.add(keyCode)
        
        checkSequences(now)
        checkChordStarts(now)
    }

    fun onKeyReleased(keyCode: Int) {
        val now = System.currentTimeMillis()
        pressedKeys.remove(keyCode)
        
        // Cancel any hold jobs that required this key
        activeUnifiedTriggers.filter { it.triggerType == TriggerType.HOLD && it.keyCodes.contains(keyCode) }
            .forEach { trigger ->
                pendingHoldJobs.remove(trigger.id)?.cancel()
            }

        // Update tap history for RELEASE/MULTI_TAP triggers
        val taps = tapHistory.computeIfAbsent(keyCode) { mutableListOf() }
        taps.add(now)
        taps.retainAll { now - it < 2000 }

        checkReleaseAndTapTriggers(keyCode, now)
    }

    private fun checkSequences(now: Long) {
        activeUnifiedTriggers.filter { it.triggerType == TriggerType.SEQUENCE }.forEach { trigger ->
            if (sequenceHistory.takeLast(trigger.keyCodes.size) == trigger.keyCodes) {
                trigger(trigger)
            }
        }
    }

    private fun checkChordStarts(now: Long) {
        activeUnifiedTriggers.filter { it.triggerType == TriggerType.HOLD }.forEach { trigger ->
            // If all required keys are pressed and no job is already running for this trigger
            if (pressedKeys.containsAll(trigger.keyCodes) && !pendingHoldJobs.containsKey(trigger.id)) {
                val job = evaluatorScope.launch {
                    delay(trigger.durationMs)
                    // Double check keys are still held after delay
                    if (pressedKeys.containsAll(trigger.keyCodes)) {
                        trigger(trigger)
                    }
                }
                pendingHoldJobs[trigger.id] = job
            }
        }
    }

    private fun checkReleaseAndTapTriggers(keyCode: Int, now: Long) {
        activeUnifiedTriggers.forEach { trigger ->
            when (trigger.triggerType) {
                TriggerType.RELEASE -> {
                    if (trigger.keyCodes.size == 1 && trigger.keyCodes[0] == keyCode) {
                        trigger(trigger)
                    }
                }
                TriggerType.MULTI_TAP -> {
                    if (trigger.keyCodes.size == 1 && trigger.keyCodes[0] == keyCode) {
                        val taps = tapHistory[keyCode] ?: emptyList<Long>()
                        val recentTaps = taps.filter { now - it <= trigger.windowMs }
                        if (recentTaps.size >= trigger.tapCount) {
                            trigger(trigger)
                            tapHistory[keyCode]?.clear()
                        }
                    }
                }
                else -> {}
            }
        }
    }

    private fun trigger(unified: UnifiedTrigger) {
        println(">>> TRIGGER MATCHED: ${unified.id} (Keys: ${unified.keyCodes})")
        if (unified.confirmationRequired) {
            evaluatorScope.launch(Dispatchers.Main) {
                viewModel.macroManagerViewModel.showTriggerConfirmation(unified)
            }
        } else {
            execute(unified)
        }
    }

    fun execute(unified: UnifiedTrigger) {
        unified.routine?.let { onTriggerRoutine(it) }
        unified.macro?.let { onTriggerMacro(it) }
    }
}
