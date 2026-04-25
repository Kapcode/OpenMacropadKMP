package com.kapcode.open.macropad.kmps.models

import kotlinx.serialization.Serializable

@Serializable
sealed class AutomationTrigger {
    @Serializable
    data class KeyHold(val keyName: String, val durationMs: Long) : AutomationTrigger()
    
    @Serializable
    data class MultiTap(val keyName: String, val tapCount: Int, val windowMs: Long) : AutomationTrigger()
    
    @Serializable
    data class Sequence(val keys: List<String>, val windowMs: Long) : AutomationTrigger()
}

@Serializable
sealed class AutomationCondition {
    @Serializable
    data class ActiveWindowIs(val processName: String) : AutomationCondition()
    
    @Serializable
    data class Equals(val variable: String, val value: String) : AutomationCondition()
    
    @Serializable
    data class GreaterThan(val variable: String, val value: Double) : AutomationCondition()
    
    @Serializable
    data class Contains(val variable: String, val substring: String) : AutomationCondition()
}

@Serializable
sealed class AutomationAction {
    @Serializable
    data class MacroAction(val macroId: String) : AutomationAction()
    
    @Serializable
    data class ScriptAction(val script: String) : AutomationAction()
    
    @Serializable
    data class LayerShift(val layerId: String, val isMomentary: Boolean) : AutomationAction()
    
    @Serializable
    data class MouseKeyboard(
        val type: String, // "KEY_PRESS", "KEY_RELEASE", "MOUSE_MOVE", "MOUSE_CLICK"
        val parameters: Map<String, String>
    ) : AutomationAction()
}

@Serializable
data class LogicBlock(
    val condition: AutomationCondition? = null,
    val actions: List<AutomationAction> = emptyList()
)

@Serializable
data class AutomationRoutine(
    val id: String,
    val name: String,
    val trigger: AutomationTrigger,
    val logicBlocks: List<LogicBlock> = emptyList()
)
