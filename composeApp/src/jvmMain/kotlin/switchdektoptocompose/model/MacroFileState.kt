package switchdektoptocompose.model

import com.kapcode.open.macropad.kmps.models.MacroPack
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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

@Serializable
data class MacroPackState(
    val pack: MacroPack,
    @Transient val file: File? = null
)
