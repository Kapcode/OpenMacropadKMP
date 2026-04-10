package com.kapcode.open.macropad.kmps.utils

expect object HashUtils {
    fun sha256(data: ByteArray): ByteArray
}
