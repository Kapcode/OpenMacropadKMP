package com.kapcode.open.macropad.kmps.utils

import java.util.Base64

actual object Base64Utils {
    actual fun encode(bytes: ByteArray): String {
        return Base64.getEncoder().encodeToString(bytes)
    }

    actual fun decode(base64: String): ByteArray {
        return Base64.getDecoder().decode(base64)
    }
}
