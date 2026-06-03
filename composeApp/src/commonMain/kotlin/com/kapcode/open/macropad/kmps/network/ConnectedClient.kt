package com.kapcode.open.macropad.kmps.network

/**
 * Enum representing the authentication status of a connected client.
 */
enum class AuthStatus {
    CHALLENGE_PENDING,
    AUTHENTICATED,
    BANNED
}

/**
 * Data class representing a connected client session.
 * This is a platform-agnostic representation used across the common module.
 */
data class ConnectedClient(
    val id: String,
    val name: String,
    var authStatus: AuthStatus,
    var lastSeen: Long,
    var metadata: String? = null,
)
