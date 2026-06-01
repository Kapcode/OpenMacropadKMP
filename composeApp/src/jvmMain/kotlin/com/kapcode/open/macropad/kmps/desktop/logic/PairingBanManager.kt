package com.kapcode.open.macropad.kmps.desktop.logic

import java.util.concurrent.ConcurrentHashMap

object PairingBanManager {
    // Map of Client ID -> Expiration Timestamp (System.currentTimeMillis)
    private val temporaryBans = ConcurrentHashMap<String, Long>()

    fun banDevice(clientId: String, durationMinutes: Int) {
        val expiration = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        temporaryBans[clientId] = expiration
    }

    fun isBanned(clientId: String): Boolean {
        val expiration = temporaryBans[clientId] ?: return false
        if (System.currentTimeMillis() > expiration) {
            temporaryBans.remove(clientId)
            return false
        }
        return true
    }

    fun getRemainingBanTime(clientId: String): Long {
        val expiration = temporaryBans[clientId] ?: return 0L
        return (expiration - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun unbanDevice(clientId: String) {
        temporaryBans.remove(clientId)
    }

    fun clearAllBans() {
        temporaryBans.clear()
    }
}
