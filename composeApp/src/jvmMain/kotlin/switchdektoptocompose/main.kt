package switchdektoptocompose

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.kapcode.open.macropad.kmp.App
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf


fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "OpenMacropadKMP",
    ) {
        // For desktop, scanning is not implemented, so we pass an empty mutableStateListOf
        val foundServers = remember { mutableStateListOf<String>() }
        App(
            scanServers = { println("DesktopApp: Scan button clicked on Desktop (no scan implemented)") },
            foundServers = foundServers,
            onConnectClick = { serverAddress -> 
                println("DesktopApp: ConnectionItem clicked on Desktop: $serverAddress (no connect implemented)")
            }
        )
    }
}