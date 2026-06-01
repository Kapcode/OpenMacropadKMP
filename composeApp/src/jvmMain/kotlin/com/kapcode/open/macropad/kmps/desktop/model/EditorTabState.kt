package com.kapcode.open.macropad.kmps.desktop.model

import java.io.File

data class EditorTabState(
    val title: String,
    val content: String,
    val file: File? = null,
    val syntaxStyle: String = "text/json",
    val isModified: Boolean = false
)
