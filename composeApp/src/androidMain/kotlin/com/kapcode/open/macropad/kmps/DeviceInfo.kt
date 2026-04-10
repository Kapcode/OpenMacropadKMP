package com.kapcode.open.macropad.kmps

import android.os.Build
import android.provider.Settings
import java.security.MessageDigest

actual object DeviceInfo {
    actual val name: String
        get() {
            val resolver = MacroApplication.instance.contentResolver
            
            // Comprehensive list of settings keys that might contain a user-defined device name.
            // "device_name" is common on modern Android and Amazon Fire tablets.
            // "bluetooth_name" is a very reliable fallback as users often name their device for BT.
            val keys = listOf(
                "device_name" to "global",
                "device_name" to "secure",
                "device_name" to "system",
                "bluetooth_name" to "secure",
                "bluetooth_name" to "system"
            )

            for ((key, type) in keys) {
                try {
                    val value = when (type) {
                        "global" -> Settings.Global.getString(resolver, key)
                        "secure" -> Settings.Secure.getString(resolver, key)
                        "system" -> Settings.System.getString(resolver, key)
                        else -> null
                    }
                    if (!value.isNullOrBlank() && value != Build.MODEL) {
                        return value
                    }
                } catch (e: Exception) {
                    // Ignore and try next key
                }
            }

            // Fallback to Model (e.g., "Pixel 9")
            return Build.MODEL
        }

    actual val uniqueId: String
        get() {
            return try {
                // Settings.Secure.ANDROID_ID is stable across uninstalls for the same app signing key.
                val androidId = Settings.Secure.getString(
                    MacroApplication.instance.contentResolver, 
                    Settings.Secure.ANDROID_ID
                )
                
                // Hash it to make it anonymous while remaining unique and stable.
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(androidId.toByteArray())
                
                // Take 4 characters (2 bytes) for a short suffix: "Pixel 9a-a1b2"
                hash.joinToString("") { "%02x".format(it) }.take(4)
            } catch (e: Exception) {
                // Fallback to a random-ish ID if anything fails
                "ext"
            }
        }

    actual val hardwareMetadata: String
        get() {
            // FINGERPRINT is too volatile (changes on security updates). 
            // We use a combination of stable hardware identifiers.
            return "${Build.MANUFACTURER}|${Build.MODEL}|${Build.BOARD}|${Build.HARDWARE}"
        }
}
