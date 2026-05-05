package com.kapcode.open.macropad.kmps.network

import com.kapcode.open.macropad.kmps.network.sockets.model.DataModel

/**
 * Sealed class representing session-related events.
 */
sealed class SessionEvent {
    data class Connected(val client: ConnectedClient) : SessionEvent()
    data class Disconnected(val clientId: String, val reason: String) : SessionEvent()
    data class MessageReceived(val clientId: String, val message: DataModel) : SessionEvent()
    data class PairingRequest(val client: ConnectedClient, val pairingCode: String) : SessionEvent()
    data class AuthStatusChanged(val clientId: String, val newStatus: AuthStatus) : SessionEvent()
}
