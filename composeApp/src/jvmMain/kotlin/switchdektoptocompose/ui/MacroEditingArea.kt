package switchdektoptocompose.ui

import androidx.compose.runtime.Composable
import switchdektoptocompose.viewmodel.*
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState

@OptIn(ExperimentalSplitPaneApi::class)
@Composable
fun MacroEditingArea(
    macroManagerViewModel: MacroManagerViewModel,
    macroEditorViewModel: MacroEditorViewModel,
    macroTimelineViewModel: MacroTimelineViewModel,
    settingsViewModel: SettingsViewModel,
    consoleViewModel: ConsoleViewModel,
    selectedTheme: String,
    onAddEventClicked: () -> Unit,
    onRecordMacroClicked: () -> Unit
) {
    val verticalSplitter = rememberSplitPaneState(initialPositionPercentage = 0.7f)
    val horizontalSplitter = rememberSplitPaneState(initialPositionPercentage = 0.2f)

    VerticalSplitPane(splitPaneState = verticalSplitter) {
        // --- Top Pane (Macro Manager & Editor) ---
        first(minSize = 200.dp) {
            HorizontalSplitPane(splitPaneState = horizontalSplitter) {
                first(minSize = 200.dp) {
                    MacroManagerScreen(
                        viewModel = macroManagerViewModel,
                        consoleViewModel = consoleViewModel,
                        selectedTheme = selectedTheme,
                        onNewMacroClicked = onRecordMacroClicked
                    )
                }
                second(minSize = 300.dp) {
                    MacroEditorScreen(viewModel = macroEditorViewModel, settingsViewModel = settingsViewModel)
                }
            }
        }
        // --- Bottom Pane (Timeline) ---
        second(minSize = 150.dp) {
            MacroTimelineScreen(
                viewModel = macroTimelineViewModel,
                onAddEventClicked = onAddEventClicked,
                onRecordMacroClicked = onRecordMacroClicked
            )
        }
    }
}
