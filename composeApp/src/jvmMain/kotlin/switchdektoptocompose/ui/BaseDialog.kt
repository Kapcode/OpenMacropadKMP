package switchdektoptocompose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme
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
    alwaysOnTop: Boolean = true,
    selectedTheme: String,
    closeOnMinimize: Boolean = true,
    consoleViewModel: ConsoleViewModel? = null,
    content: @Composable () -> Unit
) {
    Window(
        onCloseRequest = onCloseRequest,
        state = state,
        title = title,
        resizable = resizable,
        alwaysOnTop = alwaysOnTop,
        focusable = true
    ) {
        LaunchedEffect(state.isMinimized) {
            if (state.isMinimized && closeOnMinimize) {
                consoleViewModel?.addLog(LogLevel.Info, "DIALOG CLOSED: $title minimized and was closed automatically.")
                onCloseRequest()
            }
        }
        AppTheme(useDarkTheme = selectedTheme == "Dark Blue") {
            Surface(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}
