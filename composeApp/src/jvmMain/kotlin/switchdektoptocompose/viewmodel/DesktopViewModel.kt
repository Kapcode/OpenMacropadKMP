package switchdektoptocompose.viewmodel

import MacroKTOR.MacroKtorServer
import switchdektoptocompose.logic.AppSettings
import switchdektoptocompose.logic.TrustedDeviceManager
import com.kapcode.open.macropad.kmps.network.sockets.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import switchdektoptocompose.logic.ConnectionHistoryManager
import switchdektoptocompose.logic.ServerDiscoveryAnnouncer
import switchdektoptocompose.model.*
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * The UI state for the desktop application.
 */
data class DesktopUiState(
    val isServerRunning: Boolean = false,
    val serverIpAddress: String = "Determining IP...",
    val encryptionEnabled: Boolean = true,
    val isMacroExecutionEnabled: Boolean = true,
    val connectedDevices: List<ClientInfo> = emptyList(),
    val pendingPairingRequests: List<ClientInfo> = emptyList(),
    val serverError: String? = null,
    val bannedDevices: Map<String, String> = emptyMap(),
    val trustedDevices: Map<String, String> = emptyMap(),
    val connectionHistory: List<ConnectionHistoryManager.ConnectionEvent> = emptyList(),
    val totalCurrencySpent: Long = 0
)

/**
 * The ViewModel for the desktop application.
 */
class DesktopViewModel(
    private val settingsViewModel: SettingsViewModel,
    val consoleViewModel: ConsoleViewModel
) {

    lateinit var macroManagerViewModel: MacroManagerViewModel

    private val _uiState = MutableStateFlow(DesktopUiState())
    val uiState: StateFlow<DesktopUiState> = _uiState.asStateFlow()

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val server = MacroKtorServer(
        appSettings = AppSettings,
        trustedDeviceManager = TrustedDeviceManager,
        onMessageReceived = { clientId, dataModel -> onDataReceived(clientId, dataModel) },
        onClientConnected = ::onClientConnected,
        onClientDisconnected = ::onClientDisconnected,
        onPairingRequest = ::onPairingRequest
    )
    private val discoveryAnnouncer = ServerDiscoveryAnnouncer()

    init {
        findLocalIpAddresses()
        updateHistoryState()
        _uiState.update { it.copy(
            bannedDevices = TrustedDeviceManager.getBannedDevices(),
            trustedDevices = TrustedDeviceManager.getTrustedDevices(),
            totalCurrencySpent = AppSettings.totalCurrencySpent
        ) }
        consoleViewModel.addLog(LogLevel.Info, "DesktopViewModel Initialized")
    }

    fun setMacroExecutionEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isMacroExecutionEnabled = enabled) }
        consoleViewModel.addLog(LogLevel.Info, "Macro execution ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun stopAllMacros() {
        consoleViewModel.addLog(LogLevel.Warn, "E-STOP ACTIVATED - Stopping all macros")
        macroManagerViewModel.cancelAllMacros()
        if (settingsViewModel.hardEstop.value) {
            setMacroExecutionEnabled(false)
        }
    }

    fun setEncryption(enabled: Boolean) {
        if (!_uiState.value.isServerRunning) {
            _uiState.update { it.copy(encryptionEnabled = enabled) }
            consoleViewModel.addLog(LogLevel.Info, "Encryption ${if (enabled) "enabled" else "disabled"}")
        }
    }

    private fun findLocalIpAddresses() {
        val ips = try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.asSequence() }
                .filterIsInstance<Inet4Address>()
                .map { it.hostAddress }
                .joinToString(", ")
        } catch (e: Exception) {
            "Not Found"
        }
        _uiState.update { it.copy(serverIpAddress = if (ips.isBlank()) "Not Found" else ips) }
        consoleViewModel.addLog(LogLevel.Verbose, "Found local IPs: $ips")
    }

    fun startServer(forceRecreateKeystore: Boolean = false) {
        if (server.isRunning() && !forceRecreateKeystore) return
        if (forceRecreateKeystore) stopServer()
        
        consoleViewModel.addLog(LogLevel.Info, if (forceRecreateKeystore) "Recreating keystore and starting server..." else "Starting server...")
        try {
            val encryptionEnabled = _uiState.value.encryptionEnabled
            val port = if (encryptionEnabled) {
                settingsViewModel.secureServerPort.value
            } else {
                settingsViewModel.serverPort.value
            }
            
            if (encryptionEnabled) {
                try {
                    val workingDir = java.io.File(System.getProperty("user.home"), ".openmacropad")
                    if (!workingDir.exists()) workingDir.mkdirs()
                    com.kapcode.open.macropad.kmps.utils.KeystoreUtils.getOrCreateKeystore(workingDir, forceRecreateKeystore)
                } catch (e: com.kapcode.open.macropad.kmps.utils.KeystorePasswordException) {
                    consoleViewModel.addLog(LogLevel.Error, "Keystore Error: ${e.message}")
                    _uiState.update { it.copy(serverError = "Keystore password mismatch. Would you like to reset your server identity?") }
                    return
                }
            }

            server.start(port, encryptionEnabled)
            _uiState.update { it.copy(isServerRunning = server.isRunning()) }
            if (server.isRunning()) {
                val serverName = System.getProperty("user.name") ?: "OpenMacropad Server"
                discoveryAnnouncer.start(serverName, port, encryptionEnabled)
                consoleViewModel.addLog(LogLevel.Info, "Server started on port $port")
            } else {
                consoleViewModel.addLog(LogLevel.Error, "Server failed to start")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(isServerRunning = false) }
            consoleViewModel.addLog(LogLevel.Error, "Error starting server: ${e.message}")
        }
    }

    fun stopServer() {
        if (!server.isRunning()) return
        consoleViewModel.addLog(LogLevel.Info, "Stopping server...")
        discoveryAnnouncer.stop()
        viewModelScope.launch {
            server.stop()
            _uiState.update { it.copy(isServerRunning = false) }
            consoleViewModel.addLog(LogLevel.Info, "Server stopped")
        }
    }

    fun clearServerError() {
        _uiState.update { it.copy(serverError = null) }
    }

    fun shutdown() {
        stopServer()
    }

    fun sendMacroListToAllClients() {
        viewModelScope.launch {
            val macroNames = macroManagerViewModel.macroFiles.value.map { it.name }
            server.sendToAll(macroListMessage(macroNames))
            consoleViewModel.addLog(LogLevel.Debug, "Sent macro list to all clients")
        }
    }

    private fun onClientConnected(clientId: String, clientName: String) {
        val isTrusted = TrustedDeviceManager.isTrusted(clientId)
        _uiState.update { state ->
            val updatedDevices = if (state.connectedDevices.any { it.id == clientId }) {
                state.connectedDevices.map { if (it.id == clientId) it.copy(isTrusted = isTrusted) else it }
            } else {
                state.connectedDevices + ClientInfo(id = clientId, name = clientName, isTrusted = isTrusted)
            }
            state.copy(connectedDevices = updatedDevices)
        }
        ConnectionHistoryManager.logEvent(clientId, clientName, "Connected")
        updateHistoryState()
        consoleViewModel.addLog(LogLevel.Info, "Client connected: $clientName ($clientId)")
    }

    private fun onClientDisconnected(clientId: String) {
        val client = _uiState.value.connectedDevices.find { it.id == clientId }
        _uiState.update { state ->
            state.copy(
                connectedDevices = state.connectedDevices.filterNot { it.id == clientId },
                pendingPairingRequests = state.pendingPairingRequests.filterNot { it.id == clientId }
            )
        }
        client?.let {
            ConnectionHistoryManager.logEvent(it.id, it.name, "Disconnected")
            updateHistoryState()
        }
        consoleViewModel.addLog(LogLevel.Info, "Client disconnected: ${client?.name ?: clientId}")
    }

    private fun onPairingRequest(clientId: String, clientName: String) {
        val verificationCode = (100000..999999).random().toString()
        val metadata = server.getMetadata(clientId)
        _uiState.update { state ->
            if (state.pendingPairingRequests.any { it.id == clientId }) state
            else state.copy(pendingPairingRequests = state.pendingPairingRequests + ClientInfo(
                id = clientId, 
                name = clientName, 
                verificationCode = verificationCode,
                metadata = metadata
            ))
        }
        
        ConnectionHistoryManager.logEvent(clientId, clientName, "Pairing Request", metadata = metadata)
        updateHistoryState()
        
        viewModelScope.launch {
            server.sendToClient(clientId, controlMessage(ControlCommand.PAIRING_PENDING))
        }
        
        consoleViewModel.addLog(LogLevel.Warn, "Pairing request from untrusted device: $clientName ($clientId). Displaying verification code and QR.")
    }

    fun approveDevice(clientId: String, clientName: String, persistent: Boolean = true) {
        val request = _uiState.value.pendingPairingRequests.find { it.id == clientId }
        if (request != null && !request.codeMatched) {
            consoleViewModel.addLog(LogLevel.Warn, "Attempted to approve $clientName ($clientId) before code was correctly entered.")
            return
        }

        val finalPersistent = if (settingsViewModel.allowOnceOnly.value) false else persistent
        if (finalPersistent) {
            // Get the metadata from the server session instead of the initial pairing request
            // to ensure it's not null (as it arrives after the initial pairing request)
            val metadata = server.getMetadata(clientId) ?: request?.metadata

            TrustedDeviceManager.addTrustedDevice(clientId, clientName, metadata)
            _uiState.update { it.copy(trustedDevices = TrustedDeviceManager.getTrustedDevices()) }
            ConnectionHistoryManager.logEvent(clientId, clientName, "Permanently Approved", metadata = metadata)
        } else {
            server.approveTemporaryDevice(clientId)
            ConnectionHistoryManager.logEvent(clientId, clientName, "Temporarily Approved")
        }
        updateHistoryState()
        
        server.authenticateClient(clientId)
        
        _uiState.update { state ->
            state.copy(
                pendingPairingRequests = state.pendingPairingRequests.filterNot { it.id == clientId },
                connectedDevices = state.connectedDevices.map { if (it.id == clientId) it.copy(isTrusted = finalPersistent) else it }
            )
        }
        
        viewModelScope.launch {
            server.sendToClient(clientId, pairingApprovedMessage())
            // Also send the macro list immediately upon approval
            val macroNames = macroManagerViewModel.macroFiles.value.map { it.name }
            server.sendToClient(clientId, macroListMessage(macroNames))
        }
        consoleViewModel.addLog(LogLevel.Info, "${if (persistent) "Permanently approved" else "Temporarily approved"} device: $clientName ($clientId)")
    }

    fun rejectDevice(clientId: String) {
        val clientName = _uiState.value.pendingPairingRequests.find { it.id == clientId }?.name ?: "Unknown"
        _uiState.update { it.copy(pendingPairingRequests = it.pendingPairingRequests.filterNot { it.id == clientId }) }
        ConnectionHistoryManager.logEvent(clientId, clientName, "Rejected")
        updateHistoryState()
        viewModelScope.launch {
            server.sendToClient(clientId, pairingRejectedMessage("Pairing rejected by user"))
        }
        consoleViewModel.addLog(LogLevel.Info, "Rejected device: $clientId")
    }

    fun rejectAllPendingDevices() {
        val currentRequests = _uiState.value.pendingPairingRequests
        if (currentRequests.isEmpty()) return
        
        consoleViewModel.addLog(LogLevel.Warn, "Rejecting all ${currentRequests.size} pending pairing requests.")
        currentRequests.forEach { request ->
            viewModelScope.launch {
                server.sendToClient(request.id, pairingRejectedMessage("Pairing rejected (Mass Cancel)"))
            }
        }
        _uiState.update { it.copy(pendingPairingRequests = emptyList()) }
    }

    fun banDevice(clientId: String, clientName: String) {
        TrustedDeviceManager.banDevice(clientId, clientName)
        _uiState.update { state ->
            state.copy(
                bannedDevices = TrustedDeviceManager.getBannedDevices(),
                trustedDevices = TrustedDeviceManager.getTrustedDevices(),
                pendingPairingRequests = state.pendingPairingRequests.filterNot { it.id == clientId },
                connectedDevices = state.connectedDevices.filterNot { it.id == clientId }
            )
        }
        ConnectionHistoryManager.logEvent(clientId, clientName, "Banned")
        updateHistoryState()
        
        viewModelScope.launch {
            try {
                server.disconnectClient(clientId, "Your device has been banned by the server administrator.")
            } catch (e: Exception) {
                // Ignore if already disconnected
            }
        }
        consoleViewModel.addLog(LogLevel.Warn, "Banned device: $clientName ($clientId)")
    }

    fun unbanDevice(clientId: String) {
        TrustedDeviceManager.unbanDevice(clientId)
        _uiState.update { it.copy(bannedDevices = TrustedDeviceManager.getBannedDevices()) }
        consoleViewModel.addLog(LogLevel.Info, "Unbanned device: $clientId")
    }

    fun removeTrustedDevice(clientId: String) {
        val clientName = TrustedDeviceManager.getTrustedDevices()[clientId] ?: "Unknown"
        TrustedDeviceManager.removeTrustedDevice(clientId)
        _uiState.update { state ->
            state.copy(
                trustedDevices = TrustedDeviceManager.getTrustedDevices(),
                connectedDevices = state.connectedDevices.filterNot { it.id == clientId }
            )
        }
        ConnectionHistoryManager.logEvent(clientId, clientName, "Untrusted")
        updateHistoryState()
        viewModelScope.launch {
            try {
                server.disconnectClient(clientId, "The server has removed this device from its trusted list.")
            } catch (e: Exception) {
                // Ignore
            }
        }
        consoleViewModel.addLog(LogLevel.Info, "Removed trusted device: $clientId")
    }

    fun unpairDevice(clientId: String) {
        removeTrustedDevice(clientId)
    }

    fun disconnectClient(clientId: String) {
        val clientName = _uiState.value.connectedDevices.find { it.id == clientId }?.name ?: "Unknown"
        viewModelScope.launch {
            try {
                server.disconnectClient(clientId)
                ConnectionHistoryManager.logEvent(clientId, clientName, "Force Disconnect")
                updateHistoryState()
                consoleViewModel.addLog(LogLevel.Info, "Disconnected client: $clientId")
            } catch (e: Exception) {
                consoleViewModel.addLog(LogLevel.Error, "Failed to disconnect $clientId: ${e.message}")
            }
        }
    }

    private fun onDataReceived(clientId: String, dataModel: DataModel) {
        dataModel.handle(
            onData = { key, value ->
                if (key == "currency_update") {
                    try {
                        val amount = value.decodeToString().toLong()
                        _uiState.update { state ->
                            val updatedDevices = state.connectedDevices.map { 
                                if (it.id == clientId) it.copy(currency = amount) else it 
                            }
                            state.copy(connectedDevices = updatedDevices)
                        }
                        consoleViewModel.addLog(LogLevel.Verbose, "Currency update from $clientId: $amount")
                    } catch (e: Exception) {
                        consoleViewModel.addLog(LogLevel.Error, "Invalid currency update from $clientId")
                    }
                } else if (key == "currency_spent") {
                    try {
                        val amount = value.decodeToString().toLong()
                        AppSettings.totalCurrencySpent += amount
                        _uiState.update { it.copy(totalCurrencySpent = AppSettings.totalCurrencySpent) }
                        consoleViewModel.addLog(LogLevel.Info, "Currency spent by $clientId: $amount (Total: ${_uiState.value.totalCurrencySpent})")
                    } catch (e: Exception) {
                        consoleViewModel.addLog(LogLevel.Error, "Invalid currency spent from $clientId")
                    }
                }
            },
            onControl = { cmd, params ->
                consoleViewModel.addLog(LogLevel.Debug, "Control received from $clientId: $cmd")
                when (cmd) {
                    ControlCommand.PAIRING_RESPONSE -> {
                        val enteredCode = params["code"]
                        val pendingRequest = _uiState.value.pendingPairingRequests.find { it.id == clientId }
                        if (pendingRequest != null && pendingRequest.verificationCode == enteredCode) {
                            consoleViewModel.addLog(LogLevel.Info, "Correct pairing code entered for $clientId. Waiting for manual approval.")
                            _uiState.update { state ->
                                state.copy(pendingPairingRequests = state.pendingPairingRequests.map { 
                                    if (it.id == clientId) it.copy(codeMatched = true) else it
                                })
                            }
                            // Notify client that code matched and they should wait for desktop approval
                            viewModelScope.launch {
                                server.sendToClient(clientId, controlMessage(ControlCommand.PAIRING_CODE_MATCHED))
                            }
                        } else {
                            consoleViewModel.addLog(LogLevel.Warn, "Incorrect pairing code entered from $clientId.")
                        }
                    }
                    else -> {}
                }
            },
            onText = { message ->
                consoleViewModel.addLog(LogLevel.Debug, "Text received from $clientId: $message")
                if (message == "getMacros") {
                    if (server.isDeviceTrusted(clientId)) {
                        val macroNames = macroManagerViewModel.macroFiles.value.map { it.name }
                        viewModelScope.launch {
                            server.sendToClient(clientId, macroListMessage(macroNames))
                        }
                        consoleViewModel.addLog(LogLevel.Debug, "Sent macro list to $clientId")
                    } else {
                        consoleViewModel.addLog(LogLevel.Warn, "Untrusted device $clientId requested macro list. Ignored.")
                    }
                }
            },
            onCommand = { cmd, _ ->
                consoleViewModel.addLog(LogLevel.Debug, "Command received from $clientId: $cmd")
                if (cmd.startsWith("play:")) {
                    if (_uiState.value.isMacroExecutionEnabled) {
                        val macroName = cmd.substringAfter("play:")
                        val macroToPlay = macroManagerViewModel.macroFiles.value.find { it.name.equals(macroName, ignoreCase = true) }
                        if (macroToPlay != null) {
                            macroManagerViewModel.onPlayMacro(
                                macro = macroToPlay,
                                onStart = {
                                    viewModelScope.launch {
                                        server.sendToClient(clientId, DataModelBuilder().control(ControlCommand.EXECUTION_START, mapOf("macro" to macroName)).build())
                                    }
                                },
                                onComplete = {
                                    viewModelScope.launch {
                                        server.sendToClient(clientId, DataModelBuilder().control(ControlCommand.EXECUTION_COMPLETE, mapOf("macro" to macroName)).build())
                                    }
                                },
                                onFailure = { error ->
                                    viewModelScope.launch {
                                        server.sendToClient(clientId, DataModelBuilder().control(ControlCommand.EXECUTION_FAILED, mapOf("macro" to macroName, "error" to error)).build())
                                    }
                                }
                            )
                            consoleViewModel.addLog(LogLevel.Info, "Playing macro '$macroName' for $clientId")
                        } else {
                            consoleViewModel.addLog(LogLevel.Warn, "Macro '$macroName' not found for $clientId")
                            viewModelScope.launch {
                                server.sendToClient(clientId, DataModelBuilder().control(ControlCommand.EXECUTION_FAILED, mapOf("macro" to macroName, "error" to "Macro not found")).build())
                            }
                        }
                    } else {
                        consoleViewModel.addLog(LogLevel.Info, "Macro execution is disabled. Ignoring play request from $clientId")
                        val macroName = cmd.substringAfter("play:")
                        viewModelScope.launch {
                            server.sendToClient(clientId, DataModelBuilder().control(ControlCommand.EXECUTION_FAILED, mapOf("macro" to macroName, "error" to "Execution disabled")).build())
                        }
                    }
                }
            },
            onHeartbeat = { _ -> }
        )
    }

    fun onError(error: String) {
        consoleViewModel.addLog(LogLevel.Error, "SERVER ERROR: $error")
    }

    private fun updateHistoryState() {
        _uiState.update { it.copy(connectionHistory = ConnectionHistoryManager.getHistory()) }
    }

    fun clearConnectionHistory() {
        ConnectionHistoryManager.clearHistory()
        updateHistoryState()
    }
}
