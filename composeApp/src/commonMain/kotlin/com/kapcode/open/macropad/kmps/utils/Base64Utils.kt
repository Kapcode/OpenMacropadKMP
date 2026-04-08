package com.kapcode.open.macropad.kmps.utils

expect object Base64Utils {
    fun encode(bytes: ByteArray): String
    fun decode(base64: String): ByteArray
}
