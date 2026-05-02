package com.kapcode.open.macropad.kmps.ui.navigation

import com.kapcode.open.macropad.kmps.ServerInfo

sealed class Screen {
    data object Discovery : Screen()
    data class MacroGrid(val serverInfo: ServerInfo, val deviceName: String) : Screen()
    data object Marketplace : Screen()
}
