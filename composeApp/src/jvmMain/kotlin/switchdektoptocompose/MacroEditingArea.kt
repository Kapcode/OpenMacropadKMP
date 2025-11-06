package switchdektoptocompose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmp.MacroManagerViewModel
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState

@Composable
fun MacroEditingArea(
    macroManagerViewModel: MacroManagerViewModel,
    macroEditorViewModel: MacroEditorViewModel
) {
    val verticalSplitter = rememberSplitPaneState(initialPositionPercentage = 0.7f)
    val horizontalSplitter = rememberSplitPaneState(initialPositionPercentage = 0.4f)

    VerticalSplitPane(splitPaneState = verticalSplitter) {
        // --- Top Pane (Macro Manager & Editor) ---
        first(minSize = 200.dp) {
            HorizontalSplitPane(splitPaneState = horizontalSplitter) {
                // Left side: Macro List
                first(minSize = 200.dp) {
                    MacroManagerScreen(viewModel = macroManagerViewModel)
                }
                // Right side: Tabbed Editor
                second(minSize = 300.dp) {
                    MacroEditorScreen(viewModel = macroEditorViewModel)
                }
            }
        }
        // --- Bottom Pane (Timeline) ---
        second(minSize = 150.dp) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2B2B2B)).padding(8.dp)) {
                Text("Macro Timeline Area", color = Color.White)
            }
        }
    }
}