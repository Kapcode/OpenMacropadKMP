package com.kapcode.open.macropad.kmps.desktop.logic

import kotlinx.serialization.Serializable

@Serializable
data class ServerState(
    val activeRewardSessionId: String? = null,
    val claimTimestamp: Long? = null,
    val isPremium: Boolean = false,
    val lastSyncTimestamp: Long = 0L
)
