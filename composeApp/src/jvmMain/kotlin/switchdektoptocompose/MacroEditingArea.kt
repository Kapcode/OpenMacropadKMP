package switchdektoptocompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState

@Composable
fun MacroEditingArea(
    macroManagerViewModel: MacroManagerViewModel,
    macroEditorViewModel: MacroEditorViewModel,
    macroTimelineViewModel: MacroTimelineViewModel // Added Timeline ViewModel
) {
    val verticalSplitter = rememberSplitPaneState(initialPositionPercentage = 0.7f)
    val horizontalSplitter = rememberSplitPaneState(initialPositionPercentage = 0.2f)

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
            MacroTimelineScreen(viewModel = macroTimelineViewModel) // Pass Timeline ViewModel
        }
    }
}