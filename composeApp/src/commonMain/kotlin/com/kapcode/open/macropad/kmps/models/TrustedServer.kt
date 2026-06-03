package com.kapcode.open.macropad.kmps.models

import kotlinx.serialization.Serializable

@Serializable
data class TrustedServer(
    val serverId: String, // Public Key or UUID
    val displayName: String,
    val lastIpAddress: String,
    val port: Int,
    val isSecure: Boolean,
    val lastConnectedTimestamp: Long,
) {
    companion object {
        const val ACTION_SET_DEFAULT = "SET_DEFAULT"
    }
}
