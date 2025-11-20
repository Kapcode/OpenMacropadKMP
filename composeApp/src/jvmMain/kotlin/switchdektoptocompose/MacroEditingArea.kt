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
    macroTimelineViewModel: MacroTimelineViewModel,
    onAddEventClicked: () -> Unit, // Added callback
    onRecordMacroClicked: () -> Unit
) {
    val verticalSplitter = rememberSplitPaneState(initialPositionPercentage = 0.7f)
    val horizontalSplitter = rememberSplitPaneState(initialPositionPercentage = 0.2f)

    VerticalSplitPane(splitPaneState = verticalSplitter) {
        // --- Top Pane (Macro Manager & Editor) ---
        first(minSize = 200.dp) {
            HorizontalSplitPane(splitPaneState = horizontalSplitter) {
                first(minSize = 200.dp) {
                    MacroManagerScreen(viewModel = macroManagerViewModel)
                }
                second(minSize = 300.dp) {
                    MacroEditorScreen(viewModel = macroEditorViewModel)
                }
            }
        }
        // --- Bottom Pane (Timeline) ---
        second(minSize = 150.dp) {
            MacroTimelineScreen(
                viewModel = macroTimelineViewModel,
                onAddEventClicked = onAddEventClicked, // Pass callback down
                onRecordMacroClicked = onRecordMacroClicked
            )
        }
    }
}