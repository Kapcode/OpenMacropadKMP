package com.kapcode.open.macropad.kmps

data class ServerInfo(
    val name: String,
    val address: String, // e.g., "192.168.1.10:8443"
    val isSecure: Boolean
)
