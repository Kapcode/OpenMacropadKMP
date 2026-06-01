package com.kapcode.open.macropad.kmps.desktop.logic

import com.kapcode.open.macropad.kmps.desktop.utils.ProjectPaths
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages the server's authoritative state, including session locks and monetization.
 */
object ServerStateManager {
    private val workingDir = ProjectPaths.workingDir
    private val stateFile = File(workingDir, "server_state.json")
    private val premiumUsersFile = File(workingDir, "premium_users.json")

    private val mutex = Mutex()
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Serializable
    private data class ServerState(
        val activeRewardSessionId: String? = null,
        val lockExpiration: Long = 0
    )

    @Serializable
    private data class PremiumUserMap(
        val users: Map<String, Long> = emptyMap()
    )

    private var state = ServerState()
    private var premiumUsers = mutableMapOf<String, Long>() // Fingerprint -> Expiry (0 for lifetime)

    init {
        if (!workingDir.exists()) {
            workingDir.mkdirs()
        }
        // Load is done synchronously at init for simplicity, or we could use runBlocking
        // but since it's an 'object' init, we have to be careful.
        // For now, keeping it simple as it's a small file read.
        loadSync()
    }

    private fun loadSync() {
        if (stateFile.exists()) {
            try {
                state = json.decodeFromString(stateFile.readText())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (premiumUsersFile.exists()) {
            try {
                val premiumMap = json.decodeFromString<PremiumUserMap>(premiumUsersFile.readText())
                premiumUsers.putAll(premiumMap.users)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun save() = withContext(Dispatchers.IO) {
        try {
            stateFile.writeText(json.encodeToString(state))
            premiumUsersFile.writeText(json.encodeToString(PremiumUserMap(premiumUsers)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getActiveSession(): String? = mutex.withLock {
        val now = System.currentTimeMillis()
        if (now > state.lockExpiration) {
            if (state.activeRewardSessionId != null) {
                state = state.copy(activeRewardSessionId = null)
                save()
            }
            return null
        }
        return state.activeRewardSessionId
    }

    suspend fun getRemainingLockTimeMs(): Long = mutex.withLock {
        val now = System.currentTimeMillis()
        return if (now > state.lockExpiration) 0L else state.lockExpiration - now
    }

    suspend fun claimSession(sessionId: String): Boolean = mutex.withLock {
        val now = System.currentTimeMillis()
        if (state.activeRewardSessionId == null || now > state.lockExpiration) {
            state = ServerState(
                activeRewardSessionId = sessionId,
                lockExpiration = now + (48 * 60 * 60 * 1000) // 48 Hours
            )
            save()
            return true
        }
        return state.activeRewardSessionId == sessionId
    }

    suspend fun isPremium(fingerprint: String): Boolean = mutex.withLock {
        val expiry = premiumUsers[fingerprint] ?: return false
        return expiry == 0L || expiry > System.currentTimeMillis()
    }

    suspend fun addPremiumUser(fingerprint: String, durationMs: Long = 0) = mutex.withLock {
        val expiry = if (durationMs == 0L) 0L else System.currentTimeMillis() + durationMs
        premiumUsers[fingerprint] = expiry
        save()
    }
}
