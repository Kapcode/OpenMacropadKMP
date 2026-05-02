package com.kapcode.open.macropad.kmps

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun openFolder(path: String) {
    // No-op for Android
}

object DirectoryPicker {
    private var callback: ((String?) -> Unit)? = null
    private var launcher: (() -> Unit)? = null

    fun register(launcher: () -> Unit) {
        this.launcher = launcher
    }

    fun pickDirectory(onResult: (String?) -> Unit) {
        this.callback = onResult
        launcher?.invoke()
    }

    fun onResult(uri: String?) {
        callback?.invoke(uri)
        callback = null
    }
}

actual fun pickDirectory(onResult: (String?) -> Unit) {
    DirectoryPicker.pickDirectory(onResult)
}
