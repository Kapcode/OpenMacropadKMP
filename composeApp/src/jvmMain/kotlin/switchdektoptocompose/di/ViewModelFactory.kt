package switchdektoptocompose.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kapcode.open.macropad.kmps.network.sockets.model.dataMessage
import com.kapcode.open.macropad.kmps.network.sockets.model.macroListMessage
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import switchdektoptocompose.logic.ProcessWatcher
import switchdektoptocompose.viewmodel.*

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
    val sharedSettingsViewModel: com.kapcode.open.macropad.kmps.settings.SettingsViewModel,
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
        val sharedSettingsViewModel = remember { com.kapcode.open.macropad.kmps.settings.SettingsViewModel() }
        val newEventViewModel = remember { NewEventViewModel() }
        val consoleViewModel = remember { ConsoleViewModel() }
        val layoutViewModel = remember { LayoutViewModel(settingsViewModel) }
        
        val processWatcher = remember { ProcessWatcher(CoroutineScope(Dispatchers.Main)) }
        val inspectorViewModel = remember { InspectorViewModel(consoleViewModel, processWatcher) }
        
        val clientCommunicationViewModel = remember { 
            ClientCommunicationViewModel(settingsViewModel, consoleViewModel) 
        }

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

        val desktopViewModel = remember { 
            DesktopViewModel(
                settingsViewModel, 
                consoleViewModel, 
                inspectorViewModel,
                serverViewModel,
                clientCommunicationViewModel
            ) 
        }
        
        var macroManagerViewModelRef: MacroManagerViewModel? = null
        val macroManagerViewModel = remember {
            MacroManagerViewModel(
                settingsViewModel = settingsViewModel,
                consoleViewModel = consoleViewModel,
                onEditMacroRequested = { }, // Wired below
                onMacrosUpdated = {
                    val macroNames = macroManagerViewModelRef?.macroFiles?.value?.map { it.name } ?: emptyList()
                    serverViewModel.sendToAll(macroListMessage(macroNames))
                    
                    val packs = macroManagerViewModelRef?.macroPacks?.value ?: emptyList()
                    if (packs.isNotEmpty()) {
                        val json = Json { ignoreUnknownKeys = true }
                        serverViewModel.sendToAll(dataMessage("installed_packs", json.encodeToString(packs).encodeToByteArray()))
                    }
                }
            ).also { macroManagerViewModelRef = it }
        }
        
        val recordMacroViewModel = remember { RecordMacroViewModel(macroManagerViewModel) }
        
        val macroEditorViewModel = remember {
            MacroEditorViewModel(settingsViewModel) {
                macroManagerViewModel.refresh()
            }
        }

        // Wire up late dependencies and circular references
        remember(macroManagerViewModel, macroEditorViewModel, serverViewModel, clientCommunicationViewModel) {
            macroManagerViewModel.onEditMacroRequested = { macroState ->
                macroEditorViewModel.openOrSwitchToTab(macroState)
            }
            clientCommunicationViewModel.macroManagerViewModel = macroManagerViewModel
            clientCommunicationViewModel.serverViewModel = serverViewModel
            clientCommunicationViewModel.layoutViewModel = layoutViewModel
            desktopViewModel.macroManagerViewModel = macroManagerViewModel
        }

        val macroTimelineViewModel = remember { MacroTimelineViewModel(macroEditorViewModel) }
        val marketplaceViewModel = remember { MarketplaceViewModel(settingsViewModel, macroManagerViewModel) }
        val pairingViewModel = remember { PairingViewModel(settingsViewModel) }

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
