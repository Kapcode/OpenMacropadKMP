package com.kapcode.open.macropad.kmps.network.sockets.model

import com.kapcode.open.macropad.kmps.network.sockets.model.DataModel
import com.kapcode.open.macropad.kmps.network.sockets.model.DataModelBuilder
import com.kapcode.open.macropad.kmps.network.sockets.model.MessageType


/**
 * Extension functions and convenience methods for DataModel
 */

// Quick builders for common message types
fun textMessage(content: String, metadata: Map<String, String> = emptyMap()): DataModel {
    return DataModelBuilder()
        .text(content)
        .metadata(metadata)
        .build()
}

fun commandMessage(
    command: String,
    parameters: Map<String, String> = emptyMap(),
    metadata: Map<String, String> = emptyMap()
): DataModel {
    return DataModelBuilder()
        .command(command, parameters)
        .metadata(metadata)
        .build()
}

fun dataMessage(
    key: String,
    value: ByteArray,
    metadata: Map<String, String> = emptyMap()
): DataModel {
    return DataModelBuilder()
        .data(key, value)
        .metadata(metadata)
        .build()
}

fun responseMessage(
    success: Boolean,
    message: String,
    data: Any? = null,
    metadata: Map<String, String> = emptyMap()
): DataModel {
    return DataModelBuilder()
        .response(success, message, data)
        .metadata(metadata)
        .build()
}

fun heartbeatMessage(): DataModel {
    return DataModelBuilder()
        .heartbeat()
        .priority(DataModel.Priority.LOW)
        .build()
}

fun controlMessage(
    command: ControlCommand,
    parameters: Map<String, String> = emptyMap(),
    metadata: Map<String, String> = emptyMap()
): DataModel {
    return DataModelBuilder()
        .control(command, parameters)
        .metadata(metadata)
        .priority(DataModel.Priority.HIGH)
        .build()
}

// Macro specialized builders
fun macroListMessage(macros: List<String>): DataModel =
    textMessage("macros:${macros.joinToString(",")}")

fun playMacroMessage(macroName: String): DataModel =
    commandMessage("play:$macroName")

fun getMacrosRequest(): DataModel =
    textMessage("getMacros")

// Security & Pairing specialized builders
fun pairingRequestMessage(deviceName: String, deviceId: String): DataModel =
    controlMessage(
        ControlCommand.PAIRING_REQUEST,
        parameters = mapOf("name" to deviceName, "id" to deviceId)
    )

fun pairingApprovedMessage(): DataModel =
    controlMessage(ControlCommand.PAIRING_APPROVED)

fun pairingRejectedMessage(reason: String? = null): DataModel =
    controlMessage(
        ControlCommand.PAIRING_REJECTED,
        parameters = reason?.let { mapOf("reason" to it) } ?: emptyMap()
    )

fun disconnectMessage(reason: String? = null): DataModel =
    controlMessage(
        ControlCommand.DISCONNECT,
        parameters = reason?.let { mapOf("reason" to it) } ?: emptyMap()
    )

// Added errorMessage function
fun errorMessage(message: String, context: String = "general", throwable: Throwable? = null): DataModel {
    val metadata = mutableMapOf("context" to context)
    throwable?.let { metadata["stackTrace"] = it.stackTraceToString() }
    return DataModelBuilder()
        .response(false, message)
        .metadata(metadata)
        .priority(DataModel.Priority.CRITICAL)
        .build()
}


// Extension function to handle received messages with pattern matching
inline fun DataModel.handle(
    onText: (String) -> Unit = {},
    onCommand: (String, Map<String, String>) -> Unit = { _, _ -> },
    onData: (String, ByteArray) -> Unit = { _, _ -> },
    onResponse: (Boolean, String, Any?) -> Unit = { _, _, _ -> },
    onControl: (ControlCommand, Map<String, String>) -> Unit = { _, _ -> },
    onHeartbeat: (Long) -> Unit = {}
) {
    when (val msg = this.messageType) {
        is MessageType.Text -> onText(msg.content)
        is MessageType.Command -> onCommand(msg.command, msg.parameters)
        is MessageType.Data -> onData(msg.key, msg.value)
        is MessageType.Response -> onResponse(msg.success, msg.message, msg.data)
        is MessageType.Control -> onControl(msg.command, msg.parameters)
        is MessageType.Heartbeat -> onHeartbeat(msg.timestamp)
    }
}

/**
 * Enhanced handler that provides the full DataModel for metadata access
 */
inline fun DataModel.process(
    onText: (String, DataModel) -> Unit = { _, _ -> },
    onCommand: (String, Map<String, String>, DataModel) -> Unit = { _, _, _ -> },
    onData: (String, ByteArray, DataModel) -> Unit = { _, _, _ -> },
    onResponse: (Boolean, String, Any?, DataModel) -> Unit = { _, _, _, _ -> },
    onControl: (ControlCommand, Map<String, String>, DataModel) -> Unit = { _, _, _ -> },
    onHeartbeat: (Long, DataModel) -> Unit = { _, _ -> }
) {
    when (val msg = this.messageType) {
        is MessageType.Text -> onText(msg.content, this)
        is MessageType.Command -> onCommand(msg.command, msg.parameters, this)
        is MessageType.Data -> onData(msg.key, msg.value, this)
        is MessageType.Response -> onResponse(msg.success, msg.message, msg.data, this)
        is MessageType.Control -> onControl(msg.command, msg.parameters, this)
        is MessageType.Heartbeat -> onHeartbeat(msg.timestamp, this)
    }
}
