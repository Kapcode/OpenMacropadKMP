package com.kapcode.open.macropad.kmps.utils

import java.security.MessageDigest

actual object HashUtils {
    actual fun sha256(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }
}
