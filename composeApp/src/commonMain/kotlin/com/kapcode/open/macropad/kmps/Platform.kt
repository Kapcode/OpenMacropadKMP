package com.kapcode.open.macropad.kmps

expect fun openFolder(path: String)

expect fun generateUuid(): String

expect fun currentTimeMillis(): Long

expect object CryptoUtils {
    fun encrypt(data: ByteArray, key: ByteArray): ByteArray
    fun decrypt(encryptedData: ByteArray, key: ByteArray): ByteArray
    fun generateKey(): ByteArray
}
