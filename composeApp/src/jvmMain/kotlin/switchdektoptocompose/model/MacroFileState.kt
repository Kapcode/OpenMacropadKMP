package switchdektoptocompose.model

import java.io.File

data class MacroFileState(
    val id: String,
    val file: File?,
    val name: String,
    val content: String,
    val isActive: Boolean = false,
    val isSelectedForDeletion: Boolean = false,
    val allowedClients: String = ""
)
