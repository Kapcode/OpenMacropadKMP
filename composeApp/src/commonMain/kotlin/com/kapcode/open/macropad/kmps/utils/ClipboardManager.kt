package com.kapcode.open.macropad.kmps.utils

import androidx.compose.runtime.staticCompositionLocalOf

expect class ClipboardManager() {
    fun copyToClipboard(text: String)
    fun getTextFromClipboard(): String?
}

val LocalClipboardManager = staticCompositionLocalOf<ClipboardManager> {
    error("No ClipboardManager provided")
}
