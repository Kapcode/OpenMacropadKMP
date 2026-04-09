package switchdektoptocompose.model

import java.io.File

data class EditorTabState(
    val title: String,
    val content: String,
    val file: File? = null
)
