package com.kapcode.open.macropad.kmps

import java.net.InetAddress
import java.security.MessageDigest

actual object DeviceInfo {
    actual val name: String
        get() = try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            "Desktop-JVM"
        }

    actual val uniqueId: String
        get() {
            return try {
                // Hash it to make it anonymous while remaining unique and stable.
                val idString = name + System.getProperty("user.name")
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(idString.toByteArray())
                hash.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                "jvm"
            }
        }
}
