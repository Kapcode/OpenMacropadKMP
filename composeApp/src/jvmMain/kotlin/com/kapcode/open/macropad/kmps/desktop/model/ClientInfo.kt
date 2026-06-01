package com.kapcode.open.macropad.kmps.desktop.model

data class ClientInfo(
    val id: String,
    val name: String,
    val isTrusted: Boolean = false,
    val verificationCode: String? = null,
    val codeMatched: Boolean = false,
    val metadata: String? = null,
    val currency: Long = 0,
    val pairingAttempts: Int = 0
)
