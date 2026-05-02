package com.kapcode.open.macropad.kmps.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.kapcode.open.macropad.kmps.MacroApplication

actual class ClipboardManager {

    private val context = MacroApplication.instance
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    actual fun copyToClipboard(text: String) {
        val clip = ClipData.newPlainText("label", text)
        clipboard.setPrimaryClip(clip)
    }

    actual fun getTextFromClipboard(): String? {
        if (!clipboard.hasPrimaryClip()) return null
        val item = clipboard.primaryClip?.getItemAt(0)
        return item?.text?.toString()
    }
}
