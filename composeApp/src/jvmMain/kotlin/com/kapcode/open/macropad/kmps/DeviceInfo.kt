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
                // Use a combination of hostname and username to create a stable, unique ID for the JVM
                val idString = name + System.getProperty("user.name")
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(idString.toByteArray())
                hash.joinToString("") { "%02x".format(it) }.take(4)
            } catch (e: Exception) {
                "jvm"
            }
        }
}
