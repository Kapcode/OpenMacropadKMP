package com.kapcode.open.macropad.kmps.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kapcode.open.macropad.kmps.desktop.viewmodel.*
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.desktop.model.MacroEventState
import com.kapcode.open.macropad.kmps.desktop.model.TriggerState
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
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
    onRecordMacroClicked: () -> Unit,
    onEditEventClicked: (MacroEventState, Int) -> Unit,
    onEditTriggerClicked: (TriggerState) -> Unit,
    onMarketplaceClicked: () -> Unit
) {
    val verticalSplitter = rememberSplitPaneState(
        initialPositionPercentage = settingsViewModel.getSplitterPosition("Macro Editor Vertical", 0.5716f)
    )
    val horizontalSplitter = rememberSplitPaneState(
        initialPositionPercentage = settingsViewModel.getSplitterPosition("Macro Editor Horizontal", 0.6747f)
    )

    MoveableVerticalSplitPane(
        name = "Macro Editor Vertical",
        firstName = "Editor/Manager",
        secondName = "Timeline",
        consoleViewModel = consoleViewModel,
        settingsViewModel = settingsViewModel,
        splitPaneState = verticalSplitter,
        firstMinSize = 200.dp,
        secondMinSize = 150.dp,
        first = {
            MoveableHorizontalSplitPane(
                name = "Macro Editor Horizontal",
                firstName = "Macro Manager",
                secondName = "Macro Editor",
                consoleViewModel = consoleViewModel,
                settingsViewModel = settingsViewModel,
                splitPaneState = horizontalSplitter,
                firstMinSize = 200.dp,
                secondMinSize = 300.dp,
                first = {
                    Box(modifier = Modifier.fillMaxSize().background(panelBackground(4)).padding(8.dp)) {
                        MacroManagerScreen(
                            viewModel = macroManagerViewModel,
                            consoleViewModel = consoleViewModel,
                            selectedTheme = selectedTheme,
                            onNewMacroClicked = onRecordMacroClicked,
                            onMarketplaceClicked = onMarketplaceClicked
                        )
                    }
                },
                second = {
                    Box(modifier = Modifier.fillMaxSize().background(panelBackground(5)).padding(8.dp)) {
                        MacroEditorScreen(viewModel = macroEditorViewModel, settingsViewModel = settingsViewModel)
                    }
                }
            )
        },
        second = {
            Box(modifier = Modifier.fillMaxSize().background(panelBackground(6)).padding(8.dp)) {
                MacroTimelineScreen(
                    viewModel = macroTimelineViewModel,
                    onAddEventClicked = onAddEventClicked,
                    onRecordMacroClicked = onRecordMacroClicked,
                    onEditEventClicked = onEditEventClicked,
                    onEditTriggerClicked = onEditTriggerClicked
                )
            }
        }
    )
}
