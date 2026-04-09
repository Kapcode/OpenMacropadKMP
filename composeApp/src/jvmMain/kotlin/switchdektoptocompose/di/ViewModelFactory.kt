package switchdektoptocompose.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import switchdektoptocompose.viewmodel.*

data class DesktopViewModels(
    val desktopViewModel: DesktopViewModel,
    val consoleViewModel: ConsoleViewModel,
    val inspectorViewModel: InspectorViewModel,
    val recordMacroViewModel: RecordMacroViewModel,
    val macroEditorViewModel: MacroEditorViewModel,
    val macroManagerViewModel: MacroManagerViewModel,
    val settingsViewModel: SettingsViewModel,
    val macroTimelineViewModel: MacroTimelineViewModel,
    val newEventViewModel: NewEventViewModel
)

object ViewModelFactory {
    @Composable
    fun createViewModels(): DesktopViewModels {
        val settingsViewModel = remember { SettingsViewModel() }
        val newEventViewModel = remember { NewEventViewModel() }
        val consoleViewModel = remember { ConsoleViewModel() }
        val inspectorViewModel = remember { InspectorViewModel(consoleViewModel) }
        val desktopViewModel = remember { DesktopViewModel(settingsViewModel, consoleViewModel) }
        
        val macroManagerViewModel = remember {
            MacroManagerViewModel(
                settingsViewModel = settingsViewModel,
                consoleViewModel = consoleViewModel,
                onEditMacroRequested = { }, // Wired below
                onMacrosUpdated = {
                    desktopViewModel.sendMacroListToAllClients()
                }
            )
        }
        
        val recordMacroViewModel = remember { RecordMacroViewModel(macroManagerViewModel) }
        
        val macroEditorViewModel = remember {
            MacroEditorViewModel(settingsViewModel) {
                macroManagerViewModel.refresh()
            }
        }

        // Wire up late dependencies and circular references
        remember(macroManagerViewModel, macroEditorViewModel) {
            macroManagerViewModel.onEditMacroRequested = { macroState ->
                macroEditorViewModel.openOrSwitchToTab(macroState)
            }
            desktopViewModel.macroManagerViewModel = macroManagerViewModel
            Unit
        }

        val macroTimelineViewModel = remember { MacroTimelineViewModel(macroEditorViewModel) }

        return DesktopViewModels(
            desktopViewModel = desktopViewModel,
            consoleViewModel = consoleViewModel,
            inspectorViewModel = inspectorViewModel,
            recordMacroViewModel = recordMacroViewModel,
            macroEditorViewModel = macroEditorViewModel,
            macroManagerViewModel = macroManagerViewModel,
            settingsViewModel = settingsViewModel,
            macroTimelineViewModel = macroTimelineViewModel,
            newEventViewModel = newEventViewModel
        )
    }
}
