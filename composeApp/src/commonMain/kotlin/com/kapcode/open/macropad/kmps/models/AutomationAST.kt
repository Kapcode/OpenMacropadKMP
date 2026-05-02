@file:OptIn(ExperimentalSerializationApi::class)

package com.kapcode.open.macropad.kmps.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * A serializer that can read both JSON strings and JSON numbers as a Kotlin String.
 * This ensures backward compatibility when a field changes from Int/Long to String.
 */
object FlexibleStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                element.content
            } else {
                element.toString()
            }
        } else {
            decoder.decodeString()
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

@Serializable
sealed class AutomationTrigger {
    @Serializable
    data class KeyHold(val keyName: String, @Serializable(with = FlexibleStringSerializer::class) val durationMs: String) : AutomationTrigger()
    
    @Serializable
    data class MultiTap(val keyName: String, @Serializable(with = FlexibleStringSerializer::class) val tapCount: String, @Serializable(with = FlexibleStringSerializer::class) val windowMs: String) : AutomationTrigger()
    
    @Serializable
    data class Sequence(val keys: List<String>, @Serializable(with = FlexibleStringSerializer::class) val windowMs: String) : AutomationTrigger()
    
    @Serializable
    data class OnConditionMet(val condition: AutomationCondition) : AutomationTrigger()

    @Serializable
    data class ControllerButton(val button: String, val controllerIndex: String = "0") : AutomationTrigger()
}

@Serializable
sealed class AutomationCondition {
    @Serializable
    data class ActiveWindowIs(val processName: String) : AutomationCondition()
    
    @Serializable
    data class ActiveWindowTitleIs(val windowTitle: String) : AutomationCondition()
    
    @Serializable
    data class Equals(val variable: String, val value: String) : AutomationCondition()
    
    @Serializable
    data class GreaterThan(val variable: String, val value: String) : AutomationCondition()
    
    @Serializable
    data class LessThan(val variable: String, val value: String) : AutomationCondition()
    
    @Serializable
    data class Contains(val variable: String, val substring: String) : AutomationCondition()
}

@Serializable
@JsonClassDiscriminator("type")
sealed class AutomationAction {
    @Serializable
    @SerialName("MacroAction")
    data class MacroAction(val macroId: String) : AutomationAction()
    
    @Serializable
    @SerialName("ScriptAction")
    data class ScriptAction(val script: String) : AutomationAction()
    
    @Serializable
    @SerialName("LayerShift")
    data class LayerShift(val layerId: String, val isMomentary: Boolean) : AutomationAction()
    
    @Serializable
    @SerialName("KeyEvent")
    data class KeyEvent(val keyName: String, val actionType: String) : AutomationAction() // type: "PRESS", "RELEASE", "TYPE"
    
    @Serializable
    @SerialName("MouseEvent")
    data class MouseEvent(@Serializable(with = FlexibleStringSerializer::class) val x: String, @Serializable(with = FlexibleStringSerializer::class) val y: String, val actionType: String, val isAnimated: Boolean = false) : AutomationAction() // type: "MOVE", "CLICK"
    
    @Serializable
    @SerialName("MouseButtonEvent")
    data class MouseButtonEvent(@Serializable(with = FlexibleStringSerializer::class) val buttonNumber: String, val actionType: String) : AutomationAction() // type: "PRESS", "RELEASE", "CLICK"
    
    @Serializable
    @SerialName("ScrollEvent")
    data class ScrollEvent(@Serializable(with = FlexibleStringSerializer::class) val amount: String) : AutomationAction()
    
    @Serializable
    @SerialName("DelayEvent")
    data class DelayEvent(@Serializable(with = FlexibleStringSerializer::class) val durationMs: String) : AutomationAction()

    @Serializable
    @SerialName("SetAutoDelay")
    data class SetAutoDelay(@Serializable(with = FlexibleStringSerializer::class) val delayMs: String) : AutomationAction()
    
    @Serializable
    @SerialName("SetVariable")
    data class SetVariable(val name: String, val value: String) : AutomationAction()

    @Serializable
    @SerialName("MouseKeyboard")
    data class MouseKeyboard(
        val actionType: String,
        val parameters: Map<String, String>
    ) : AutomationAction()

    @Serializable
    @SerialName("ControllerButton")
    data class ControllerButton(val button: String, val controllerIndex: String = "0", val actionType: String = "PRESS") : AutomationAction()
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
    val triggers: List<AutomationTrigger> = emptyList(), // Support multiple triggers
    val logicBlocks: List<LogicBlock> = emptyList(),
    val targetProcess: String? = null
)
