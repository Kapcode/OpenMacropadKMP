package switchdektoptocompose.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import switchdektoptocompose.logic.ConnectionHistoryManager
import switchdektoptocompose.logic.TriggerBridge
import switchdektoptocompose.logic.UnifiedTrigger
import switchdektoptocompose.model.*
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * The UI state for the desktop application.
 * Note: Many of these are moving to more specific ViewModels.
 */
data class DesktopUiState(
    val encryptionEnabled: Boolean = true,
    val isMacroExecutionEnabled: Boolean = true,
    val serverError: String? = null,
    val totalCurrencySpent: Long = 0
)

/**
 * The primary ViewModel for coordinating the desktop application.
 * Now refactored to delegate to ServerViewModel and ClientCommunicationViewModel.
 */
open class DesktopViewModel(
    val settingsViewModel: SettingsViewModel,
    val consoleViewModel: ConsoleViewModel,
    val inspectorViewModel: InspectorViewModel,
    val serverViewModel: ServerViewModel,
    val clientCommunicationViewModel: ClientCommunicationViewModel
) : TriggerBridge {
    lateinit var macroManagerViewModel: MacroManagerViewModel
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    // Legacy state for UI compatibility during transition
    private val _uiState = MutableStateFlow(DesktopUiState())
    val uiState = _uiState.asStateFlow()

    init {
        consoleViewModel.addLog(LogLevel.Info, "DesktopViewModel Refactored Initialized")
    }

    // Delegation methods for UI compatibility
    fun startServer(forceRecreateKeystore: Boolean = false) = serverViewModel.startServer(forceRecreateKeystore)
    fun stopServer() = serverViewModel.stopServer()
    fun clearServerError() = serverViewModel.clearServerError()
    fun setEncryption(enabled: Boolean) = serverViewModel.setEncryption(enabled)

    fun setMacroExecutionEnabled(enabled: Boolean) = clientCommunicationViewModel.setMacroExecutionEnabled(enabled)
    
    override fun stopAllMacros() {
        consoleViewModel.addLog(LogLevel.Warn, "E-STOP ACTIVATED - Stopping all macros")
        if (::macroManagerViewModel.isInitialized) {
            macroManagerViewModel.cancelAllMacros()
        }
        if (settingsViewModel.hardEstop.value) {
            setMacroExecutionEnabled(false)
        }
    }

    fun approveDevice(clientId: String, clientName: String, persistent: Boolean = true) = 
        clientCommunicationViewModel.approveDevice(clientId, clientName, persistent)
    
    fun rejectDevice(clientId: String) = clientCommunicationViewModel.denyDevice(clientId)
    
    fun rejectAllPendingDevices() {
        clientCommunicationViewModel.pendingPairingRequests.value.toList().forEach {
            clientCommunicationViewModel.denyDevice(it.id)
        }
    }

    fun banDevice(clientId: String, clientName: String) = clientCommunicationViewModel.banDevice(clientId, clientName)
    fun unbanDevice(clientId: String) = clientCommunicationViewModel.unbanDevice(clientId)
    fun unpairDevice(clientId: String) = clientCommunicationViewModel.removeTrustedDevice(clientId)
    
    fun disconnectClient(clientId: String) {
        viewModelScope.launch {
            serverViewModel.server.disconnectClient(clientId)
        }
    }

    fun clearConnectionHistory() {
        ConnectionHistoryManager.clearHistory()
        // ClientCommunicationViewModel should ideally expose the history flow
    }

    fun shutdown() {
        serverViewModel.stopServer()
    }

    // TriggerBridge Implementation
    override fun addLog(level: LogLevel, msg: String) {
        consoleViewModel.addLog(level, msg)
    }

    override fun cancelAllMacros() {
        if (::macroManagerViewModel.isInitialized) {
            macroManagerViewModel.cancelAllMacros()
        }
    }

    override fun toggleInspector() {
        // No-op - uiEvents removed as it was unused
    }

    override fun showTriggerConfirmation(unified: UnifiedTrigger) {
        // No-op - uiEvents removed as it was unused
    }

    override fun isMacroExecutionEnabled(): Boolean {
        return clientCommunicationViewModel.isMacroExecutionEnabled.value
    }

    override fun getConsoleLogs(): String {
        return consoleViewModel.getConsoleLogs()
    }

    override fun copyLogsToClipboard() {
        val logs = getConsoleLogs()
        val selection = StringSelection(logs)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
        addLog(LogLevel.Info, "Logs copied to clipboard")
    }
}
