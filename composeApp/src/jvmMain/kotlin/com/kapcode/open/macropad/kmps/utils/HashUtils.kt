package com.kapcode.open.macropad.kmps.utils

import java.security.MessageDigest

actual object HashUtils {
    actual fun sha256(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }

    actual fun sha256String(input: String): String {
        val hashBytes = sha256(input.encodeToByteArray())
        return hashBytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }
}
