package switchdektoptocompose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme
import kotlinx.coroutines.delay
import switchdektoptocompose.model.LogLevel
import switchdektoptocompose.viewmodel.ConsoleViewModel

/**
 * A reusable wrapper for all JVM Windows/Dialogs in the application.
 * Handles theme application and standard window configurations.
 */
@Composable
fun AppDialog(
    title: String,
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    resizable: Boolean = false,
    alwaysOnTop: Boolean = false,
    selectedTheme: String,
    closeOnMinimize: Boolean = true,
    consoleViewModel: ConsoleViewModel? = null,
    icon: Painter? = null,
    content: @Composable () -> Unit
) {
    Window(
        onCloseRequest = onCloseRequest,
        state = state,
        title = title,
        resizable = resizable,
        alwaysOnTop = alwaysOnTop,
        focusable = true,
        icon = icon
    ) {
        // Force focus and repaint on init to avoid "glitched" non-responsive states.
        // In VM environments, we perform an aggressive sequence to ensure the Skia surface renders.
        LaunchedEffect(Unit) {
            repeat(10) { stage ->
                window.toFront()
                window.requestFocus()
                window.revalidate()
                window.repaint()
                
                // On several stages, perform a tiny move to trigger window manager refresh
                if (stage % 3 == 2) {
                    val pos = window.location
                    window.setLocation(pos.x + 1, pos.y)
                    delay(5)
                    window.setLocation(pos.x, pos.y)
                }

                delay(if (stage == 0) 20 else 100)
            }
        }

        LaunchedEffect(state.isMinimized) {
            if (state.isMinimized && closeOnMinimize) {
                consoleViewModel?.addLog(LogLevel.Info, "DIALOG CLOSED: $title minimized and was closed automatically.")
                onCloseRequest()
            }
        }
        
        // Root Surface with explicit color prevents transparency/smearing before theme is fully applied
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (selectedTheme == "Dark Blue") androidx.compose.ui.graphics.Color(0xFF121212) else androidx.compose.ui.graphics.Color.White
        ) {
            AppTheme(useDarkTheme = selectedTheme == "Dark Blue") {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background // Explicitly set background color to prevent smearing
                ) {
                    content()
                }
            }
        }
    }
}
