package switchdektoptocompose.model

import java.util.*

data class TriggerState(
    val keyName: String,
    val allowedClients: String,
    val triggerType: TriggerType = TriggerType.RELEASE,
    val holdDurationMs: Long = 500,
    val multiTapCount: Int = 2,
    val tapWindowMs: Long = 300,
    val sequenceWindowMs: Long = 1000,
    val confirmationRequired: Boolean = false
)

sealed class MacroEventState(val id: String = UUID.randomUUID().toString()) {
    data class KeyEvent(val keyName: String, val action: KeyAction) : MacroEventState()
    data class MouseEvent(val x: Int, val y: Int, val action: MouseAction, val isAnimated: Boolean = false) : MacroEventState()
    data class MouseButtonEvent(val buttonNumber: Int, val action: KeyAction) : MacroEventState()
    data class ScrollEvent(val scrollAmount: Int) : MacroEventState()
    data class DelayEvent(val durationMs: Long) : MacroEventState()
    data class SetAutoWaitEvent(val delayMs: Int) : MacroEventState()
    data class ScriptEvent(val script: String) : MacroEventState()
}

enum class KeyAction { PRESS, RELEASE }
enum class MouseAction { MOVE, CLICK }

enum class TriggerType { RELEASE, HOLD, MULTI_TAP, SEQUENCE }
