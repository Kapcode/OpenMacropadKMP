package switchdektoptocompose.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kapcode.open.macropad.kmps.network.sockets.model.dataMessage
import com.kapcode.open.macropad.kmps.network.sockets.model.macroListMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import switchdektoptocompose.logic.ProcessWatcher
import switchdektoptocompose.viewmodel.*
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel as SharedSettingsViewModel

data class DesktopViewModels(
    val desktopViewModel: DesktopViewModel,
    val serverViewModel: ServerViewModel,
    val clientCommunicationViewModel: ClientCommunicationViewModel,
    val consoleViewModel: ConsoleViewModel,
    val inspectorViewModel: InspectorViewModel,
    val recordMacroViewModel: RecordMacroViewModel,
    val macroEditorViewModel: MacroEditorViewModel,
    val macroManagerViewModel: MacroManagerViewModel,
    val settingsViewModel: SettingsViewModel,
    val sharedSettingsViewModel: SharedSettingsViewModel,
    val macroTimelineViewModel: MacroTimelineViewModel,
    val newEventViewModel: NewEventViewModel,
    val marketplaceViewModel: MarketplaceViewModel,
    val pairingViewModel: PairingViewModel,
    val layoutViewModel: LayoutViewModel
)

object ViewModelFactory {
    @Composable
    fun createViewModels(): DesktopViewModels {
        val settingsViewModel = remember { SettingsViewModel() }
        val sharedSettingsViewModel = remember { SharedSettingsViewModel() }
        val consoleViewModel = remember { ConsoleViewModel() }
        val layoutViewModel = remember { LayoutViewModel(settingsViewModel) }
        
        val processWatcher = remember { 
            ProcessWatcher(
                scope = CoroutineScope(Dispatchers.Main),
                getPollingRate = { settingsViewModel.windowPollingRate.value }
            ) 
        }
        val inspectorViewModel = remember { InspectorViewModel(consoleViewModel, processWatcher) }
        
        val clientCommunicationViewModel = remember { 
            ClientCommunicationViewModel(settingsViewModel, consoleViewModel) 
        }

        val newEventViewModel = remember { NewEventViewModel(clientCommunicationViewModel) }

        var macroManagerViewModelRef: MacroManagerViewModel? = null

        val serverViewModel = remember {
            ServerViewModel(
                settingsViewModel = settingsViewModel,
                consoleViewModel = consoleViewModel,
                processWatcher = processWatcher,
                onMessageReceived = { clientId, dataModel -> 
                    clientCommunicationViewModel.onDataReceived(clientId, dataModel) 
                },
                onClientConnected = { clientId, name -> 
                    clientCommunicationViewModel.onClientConnected(clientId, name) 
                },
                onClientDisconnected = { clientId -> 
                    clientCommunicationViewModel.onClientDisconnected(clientId) 
                },
                onPairingRequest = { clientId, name -> 
                    clientCommunicationViewModel.onPairingRequest(clientId, name) 
                },
                onUpgradeRequest = { clientId, jarBytes, hash, isSimulation ->
                    clientCommunicationViewModel.onUpgradeRequest(clientId, jarBytes, hash, isSimulation)
                }
            )
        }

        val macroManagerViewModel = remember {
            MacroManagerViewModel(
                settingsViewModel = settingsViewModel,
                consoleViewModel = consoleViewModel,
                serverViewModel = serverViewModel,
                onEditMacroRequested = { }, // Wired below
                onMacrosUpdated = {
                    val macroNames = macroManagerViewModelRef?.macroFiles?.value?.map { it.name } ?: emptyList()
                    serverViewModel.sendToAll(macroListMessage(macroNames))
                    
                    val packs = macroManagerViewModelRef?.macroPacks?.value ?: emptyList()
                    if (packs.isNotEmpty()) {
                        val json = Json { ignoreUnknownKeys = true }
                        val packsToSerialize = packs.map { it.pack }
                        serverViewModel.sendToAll(dataMessage("installed_packs", json.encodeToString(packsToSerialize).encodeToByteArray()))
                    }
                }
            ).also { viewModel ->
                macroManagerViewModelRef = viewModel
                processWatcher.activeProcess.onEach { processName ->
                    viewModel.onActiveProcessChanged(processName)
                }.launchIn(CoroutineScope(Dispatchers.Main))
            }
        }

        val controllerManager = remember {
            switchdektoptocompose.logic.ControllerManager(
                settingsViewModel,
                macroManagerViewModel,
                layoutViewModel
            )
        }
        
        remember(controllerManager) {
            serverViewModel.controllerManager = controllerManager
            Unit
        }

        val desktopViewModel = remember { 
            DesktopViewModel(
                settingsViewModel, 
                consoleViewModel, 
                inspectorViewModel,
                serverViewModel,
                clientCommunicationViewModel
            ) 
        }

        desktopViewModel.macroManagerViewModel = macroManagerViewModel
        clientCommunicationViewModel.macroManagerViewModel = macroManagerViewModel
        clientCommunicationViewModel.serverViewModel = serverViewModel
        clientCommunicationViewModel.layoutViewModel = layoutViewModel

        val macroEditorViewModel = remember {
            MacroEditorViewModel(
                settingsViewModel = settingsViewModel,
                consoleViewModel = consoleViewModel,
                macroManagerViewModel = macroManagerViewModel
            )
        }
        val macroTimelineViewModel = remember { MacroTimelineViewModel(macroEditorViewModel) }
        val recordMacroViewModel = remember { RecordMacroViewModel(macroManagerViewModel, clientCommunicationViewModel) }
        val marketplaceViewModel = remember { MarketplaceViewModel(settingsViewModel, macroManagerViewModel) }
        val pairingViewModel = remember { PairingViewModel(settingsViewModel) }

        // Final wiring
        macroManagerViewModel.onEditMacroRequested = { macroState ->
            macroEditorViewModel.openOrSwitchToTab(macroState)
            layoutViewModel.setMainTab(1) // Switch to Editor tab
        }

        return DesktopViewModels(
            desktopViewModel = desktopViewModel,
            serverViewModel = serverViewModel,
            clientCommunicationViewModel = clientCommunicationViewModel,
            consoleViewModel = consoleViewModel,
            inspectorViewModel = inspectorViewModel,
            recordMacroViewModel = recordMacroViewModel,
            macroEditorViewModel = macroEditorViewModel,
            macroManagerViewModel = macroManagerViewModel,
            settingsViewModel = settingsViewModel,
            sharedSettingsViewModel = sharedSettingsViewModel,
            macroTimelineViewModel = macroTimelineViewModel,
            newEventViewModel = newEventViewModel,
            marketplaceViewModel = marketplaceViewModel,
            pairingViewModel = pairingViewModel,
            layoutViewModel = layoutViewModel
        )
    }
}
