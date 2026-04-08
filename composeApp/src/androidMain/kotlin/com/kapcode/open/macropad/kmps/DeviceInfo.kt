package com.kapcode.open.macropad.kmps

import android.os.Build
import android.provider.Settings
import java.security.MessageDigest

actual object DeviceInfo {
    actual val name: String
        get() = Build.MODEL

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
}
