package switchdektoptocompose.model

data class ActiveProcessInfo(
    val name: String,
    val id: String, // Typically Window ID or PID as string
    val pid: String,
    val windowId: String,
    val command: String,
    val windowTitle: String
)
