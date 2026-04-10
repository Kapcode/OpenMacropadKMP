package com.kapcode.open.macropad.kmps

expect object DeviceInfo {
    val name: String
    val uniqueId: String
    val hardwareMetadata: String
}
