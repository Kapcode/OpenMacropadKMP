package com.kapcode.open.macropad.kmps.network

import com.kapcode.open.macropad.kmps.network.sockets.model.DataModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Common interface for managing client sessions and authentication.
 * Abstracted to be platform-agnostic (JVM Server vs possible future use cases).
 */
interface SessionManager {
    /**
     * Get a list of all currently connected clients.
     */
    val activeClients: StateFlow<Map<String, ConnectedClient>>

    /**
     * Flow of session events (connections, disconnections, messages).
     */
    val events: SharedFlow<SessionEvent>

    /**
     * Send a message to a specific client.
     */
    suspend fun sendMessage(clientId: String, message: DataModel)

    /**
     * Broadcast a message to all connected and authenticated clients.
     */
    suspend fun broadcastMessage(message: DataModel)

    /**
     * Manually authenticate a client (e.g., after pairing approval).
     */
    fun authenticateClient(clientId: String)

    /**
     * Disconnect a specific client.
     */
    fun disconnectClient(clientId: String, reason: String)

    /**
     * Update the last seen timestamp for a client to prevent watchdog timeout.
     */
    fun updateHeartbeat(clientId: String)

    /**
     * Check if a device is trusted (either temporarily or permanently).
     */
    fun isDeviceTrusted(clientId: String): Boolean

    /**
     * Approve a device for the current session only.
     */
    fun approveTemporaryDevice(clientId: String)
}
