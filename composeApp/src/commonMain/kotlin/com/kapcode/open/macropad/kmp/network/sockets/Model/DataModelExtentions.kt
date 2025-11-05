
package Model

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

// Extension function to easily send with priority
fun SecureSocket.sendWithPriority(
    messageType: MessageType,
    priority: DataModel.Priority = DataModel.Priority.NORMAL
) {
    val dataModel = DataModel(messageType = messageType, priority = priority)
    this.send(dataModel)
}

// Extension function to send text quickly
fun SecureSocket.sendText(text: String) {
    this.send(textMessage(text))
}

// Extension function to send command quickly
fun SecureSocket.sendCommand(command: String, parameters: Map<String, String> = emptyMap()) {
    this.send(commandMessage(command, parameters))
}

// Extension function to handle received messages with pattern matching
inline fun DataModel.handle(
    onText: (String) -> Unit = {},
    onCommand: (String, Map<String, String>) -> Unit = { _, _ -> },
    onData: (String, ByteArray) -> Unit = { _, _ -> },
    onResponse: (Boolean, String, Any?) -> Unit = { _, _, _ -> },
    onHeartbeat: (Long) -> Unit = {}
) {
    when (val msg = this.messageType) {
        is MessageType.Text -> onText(msg.content)
        is MessageType.Command -> onCommand(msg.command, msg.parameters)
        is MessageType.Data -> onData(msg.key, msg.value)
        is MessageType.Response -> onResponse(msg.success, msg.message, msg.data)
        is MessageType.Heartbeat -> onHeartbeat(msg.timestamp)
    }
}