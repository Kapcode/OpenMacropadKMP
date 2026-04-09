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