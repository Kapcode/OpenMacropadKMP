package switchdektoptocompose

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.kapcode.open.macropad.kmp.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "OpenMacropadKMP",
    ) {
        App()
    }
}