package com.kapcode.open.macropad.kmps.utils

import com.github.javakeyring.Keyring
import java.security.SecureRandom
import java.util.Base64

object SecretManager {
    private const val SERVICE_NAME = "com.kapcode.openmacropad"
    private const val ACCOUNT_NAME = "server-keystore-password"
    
    private val keyring: Keyring? by lazy {
        try {
            Keyring.create()
        } catch (e: Exception) {
            println("Warning: Could not initialize native keyring: ${e.message}")
            null
        }
    }

    /**
     * Gets the keystore password from the native keyring.
     * If not found, it tries to get it from system properties (migration path).
     * If still not found, it generates a new random password and stores it.
     */
    fun getOrCreatePassword(): CharArray {
        // 1. Try native keyring
        try {
            val existing = keyring?.getPassword(SERVICE_NAME, ACCOUNT_NAME)
            if (!existing.isNullOrBlank()) {
                return existing.toCharArray()
            }
        } catch (e: Exception) {
            println("Error reading from keyring: ${e.message}")
        }

        // 2. Fallback to system property (for dev or migration)
        val sysProp = System.getProperty("keystore.password")
        if (!sysProp.isNullOrBlank()) {
            // Store it in the keyring for next time if possible
            savePassword(sysProp)
            return sysProp.toCharArray()
        }

        // 3. Generate new random password
        val newPassword = generateRandomPassword()
        savePassword(newPassword)
        return newPassword.toCharArray()
    }

    private fun savePassword(password: String) {
        try {
            keyring?.setPassword(SERVICE_NAME, ACCOUNT_NAME, password)
        } catch (e: Exception) {
            println("Error saving to keyring: ${e.message}")
        }
    }

    private fun generateRandomPassword(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }
}
