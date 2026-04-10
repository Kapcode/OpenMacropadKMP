package switchdektoptocompose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                splitter {
                    visiblePart {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier
                                    .width(8.dp)
                                    .height(48.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = MaterialTheme.shapes.extraSmall
                                    )
                                    .align(Alignment.Center)
                            )
                        }
                    }
                    handle {
                        Box(
                            Modifier
                                .markAsHandle()
                                .fillMaxHeight()
                                .width(16.dp)
                        )
                    }
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
        splitter {
            visiblePart {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .width(48.dp)
                            .height(8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.extraSmall
                            )
                            .align(Alignment.Center)
                    )
                }
            }
            handle {
                Box(
                    Modifier
                        .markAsHandle()
                        .fillMaxWidth()
                        .height(16.dp)
                )
            }
        }
    }
}
