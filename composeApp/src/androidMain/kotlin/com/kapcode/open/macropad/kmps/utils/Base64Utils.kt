package com.kapcode.open.macropad.kmps.utils

import android.util.Base64

actual object Base64Utils {
    actual fun encode(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    actual fun decode(base64: String): ByteArray {
        return Base64.decode(base64, Base64.NO_WRAP)
    }
}
