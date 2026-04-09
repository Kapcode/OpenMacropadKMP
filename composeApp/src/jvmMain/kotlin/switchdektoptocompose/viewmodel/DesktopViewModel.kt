package switchdektoptocompose.viewmodel

import MacroKTOR.MacroKtorServer
import com.kapcode.open.macropad.kmps.network.sockets.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import switchdektoptocompose.logic.ServerDiscoveryAnnouncer
import switchdektoptocompose.model.*
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * The ViewModel for the desktop application.
 */
class DesktopViewModel(
    private val settingsViewModel: SettingsViewModel,
    val consoleViewModel: ConsoleViewModel
) {

    lateinit var macroManagerViewModel: MacroManagerViewModel

    private val _encryptionEnabled = MutableStateFlow(true)
    val encryptionEnabled: StateFlow<Boolean> = _encryptionEnabled.asStateFlow()

    private val _isMacroExecutionEnabled = MutableStateFlow(true)
    val isMacroExecutionEnabled: StateFlow<Boolean> = _isMacroExecutionEnabled.asStateFlow()

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _connectedDevices = MutableStateFlow<List<ClientInfo>>(emptyList())
    val connectedDevices: StateFlow<List<ClientInfo>> = _connectedDevices.asStateFlow()

    private val _pendingPairingRequests = MutableStateFlow<List<ClientInfo>>(emptyList())
    val pendingPairingRequests: StateFlow<List<ClientInfo>> = _pendingPairingRequests.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _serverError = MutableStateFlow<String?>(null)
    val serverError: StateFlow<String?> = _serverError.asStateFlow()

    private val _serverIpAddress = MutableStateFlow("Determining IP...")
    val serverIpAddress: StateFlow<String> = _serverIpAddress.asStateFlow()

    private val server = MacroKtorServer(
        onMessageReceived = { clientId, dataModel -> onDataReceived(clientId, dataModel) },
        onClientConnected = ::onClientConnected,
        onClientDisconnected = ::onClientDisconnected,
        onPairingRequest = ::onPairingRequest
    )
    private val discoveryAnnouncer = ServerDiscoveryAnnouncer()

    private val _bannedDevices = MutableStateFlow<Map<String, String>>(emptyMap())
    val bannedDevices: StateFlow<Map<String, String>> = _bannedDevices.asStateFlow()

    private val _trustedDevices = MutableStateFlow<Map<String, String>>(emptyMap())
    val trustedDevices: StateFlow<Map<String, String>> = _trustedDevices.asStateFlow()

    init {
        findLocalIpAddresses()
        _bannedDevices.value = switchdektoptocompose.logic.TrustedDeviceManager.getBannedDevices()
        _trustedDevices.value = switchdektoptocompose.logic.TrustedDeviceManager.getTrustedDevices()
        consoleViewModel.addLog(LogLevel.Info, "DesktopViewModel Initialized")
    }

    fun setMacroExecutionEnabled(enabled: Boolean) {
        _isMacroExecutionEnabled.value = enabled
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
        if (!_isServerRunning.value) {
            _encryptionEnabled.value = enabled
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
        _serverIpAddress.value = if (ips.isBlank()) "Not Found" else ips
        consoleViewModel.addLog(LogLevel.Verbose, "Found local IPs: $ips")
    }

    fun startServer(forceRecreateKeystore: Boolean = false) {
        if (server.isRunning() && !forceRecreateKeystore) return
        if (forceRecreateKeystore) stopServer()
        
        consoleViewModel.addLog(LogLevel.Info, if (forceRecreateKeystore) "Recreating keystore and starting server..." else "Starting server...")
        try {
            val port = if (_encryptionEnabled.value) {
                settingsViewModel.secureServerPort.value
            } else {
                settingsViewModel.serverPort.value
            }
            
            if (_encryptionEnabled.value) {
                try {
                    val workingDir = java.io.File(System.getProperty("user.home"), ".openmacropad")
                    if (!workingDir.exists()) workingDir.mkdirs()
                    com.kapcode.open.macropad.kmps.utils.KeystoreUtils.getOrCreateKeystore(workingDir, forceRecreateKeystore)
                } catch (e: com.kapcode.open.macropad.kmps.utils.KeystorePasswordException) {
                    consoleViewModel.addLog(LogLevel.Error, "Keystore Error: ${e.message}")
                    _serverError.value = "Keystore password mismatch. Would you like to reset your server identity?"
                    return
                }
            }

            server.start(port, _encryptionEnabled.value)
            _isServerRunning.value = server.isRunning()
            if (server.isRunning()) {
                val serverName = System.getProperty("user.name") ?: "OpenMacropad Server"
                discoveryAnnouncer.start(serverName, port, _encryptionEnabled.value)
                consoleViewModel.addLog(LogLevel.Info, "Server started on port $port")
            } else {
                consoleViewModel.addLog(LogLevel.Error, "Server failed to start")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isServerRunning.value = false
            consoleViewModel.addLog(LogLevel.Error, "Error starting server: ${e.message}")
        }
    }

    fun stopServer() {
        if (!server.isRunning()) return
        consoleViewModel.addLog(LogLevel.Info, "Stopping server...")
        discoveryAnnouncer.stop()
        viewModelScope.launch {
            server.stop()
            _isServerRunning.value = false
            consoleViewModel.addLog(LogLevel.Info, "Server stopped")
        }
    }

    fun clearServerError() {
        _serverError.value = null
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
        val isTrusted = switchdektoptocompose.logic.TrustedDeviceManager.isTrusted(clientId)
        _connectedDevices.update { currentList ->
            if (currentList.any { it.id == clientId }) {
                currentList.map { if (it.id == clientId) it.copy(isTrusted = isTrusted) else it }
            } else {
                currentList + ClientInfo(id = clientId, name = clientName, isTrusted = isTrusted)
            }
        }
        consoleViewModel.addLog(LogLevel.Info, "Client connected: $clientName ($clientId)")
    }

    private fun onClientDisconnected(clientId: String) {
        val client = _connectedDevices.value.find { it.id == clientId }
        _connectedDevices.update { currentList ->
            currentList.filterNot { it.id == clientId }
        }
        _pendingPairingRequests.update { currentList ->
            currentList.filterNot { it.id == clientId }
        }
        consoleViewModel.addLog(LogLevel.Info, "Client disconnected: ${client?.name ?: clientId}")
    }

    private fun onPairingRequest(clientId: String, clientName: String) {
        _pendingPairingRequests.update { currentList ->
            if (currentList.any { it.id == clientId }) currentList else currentList + ClientInfo(id = clientId, name = clientName)
        }
        consoleViewModel.addLog(LogLevel.Warn, "Pairing request from untrusted device: $clientName ($clientId)")
    }

    fun approveDevice(clientId: String, clientName: String, persistent: Boolean = true) {
        val finalPersistent = if (settingsViewModel.allowOnceOnly.value) false else persistent
        if (finalPersistent) {
            switchdektoptocompose.logic.TrustedDeviceManager.addTrustedDevice(clientId, clientName)
            _trustedDevices.value = switchdektoptocompose.logic.TrustedDeviceManager.getTrustedDevices()
        } else {
            server.approveTemporaryDevice(clientId)
        }
        
        _pendingPairingRequests.update { it.filterNot { client -> client.id == clientId } }
        onClientConnected(clientId, clientName)
        
        // Update connected devices status
        _connectedDevices.update { currentList ->
            currentList.map { if (it.id == clientId) it.copy(isTrusted = finalPersistent) else it }
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
        _pendingPairingRequests.update { it.filterNot { client -> client.id == clientId } }
        viewModelScope.launch {
            server.sendToClient(clientId, pairingRejectedMessage("Pairing rejected by user"))
        }
        consoleViewModel.addLog(LogLevel.Info, "Rejected device: $clientId")
    }

    fun banDevice(clientId: String, clientName: String) {
        switchdektoptocompose.logic.TrustedDeviceManager.banDevice(clientId, clientName)
        _bannedDevices.value = switchdektoptocompose.logic.TrustedDeviceManager.getBannedDevices()
        _trustedDevices.value = switchdektoptocompose.logic.TrustedDeviceManager.getTrustedDevices()
        _pendingPairingRequests.update { it.filterNot { client -> client.id == clientId } }
        _connectedDevices.update { it.filterNot { client -> client.id == clientId } }
        
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
        switchdektoptocompose.logic.TrustedDeviceManager.unbanDevice(clientId)
        _bannedDevices.value = switchdektoptocompose.logic.TrustedDeviceManager.getBannedDevices()
        consoleViewModel.addLog(LogLevel.Info, "Unbanned device: $clientId")
    }

    fun removeTrustedDevice(clientId: String) {
        switchdektoptocompose.logic.TrustedDeviceManager.removeTrustedDevice(clientId)
        _trustedDevices.value = switchdektoptocompose.logic.TrustedDeviceManager.getTrustedDevices()
        _connectedDevices.update { it.filterNot { client -> client.id == clientId } }
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
        viewModelScope.launch {
            try {
                server.disconnectClient(clientId)
                consoleViewModel.addLog(LogLevel.Info, "Disconnected client: $clientId")
            } catch (e: Exception) {
                consoleViewModel.addLog(LogLevel.Error, "Failed to disconnect $clientId: ${e.message}")
            }
        }
    }

    private fun onDataReceived(clientId: String, dataModel: DataModel) {
        dataModel.handle(
            onText = { message ->
                consoleViewModel.addLog(LogLevel.Debug, "Text received from $clientId: $message")
                if (message == "getMacros") {
                    val macroNames = macroManagerViewModel.macroFiles.value.map { it.name }
                    viewModelScope.launch {
                        server.sendToClient(clientId, macroListMessage(macroNames))
                    }
                    consoleViewModel.addLog(LogLevel.Debug, "Sent macro list to $clientId")
                }
            },
            onCommand = { cmd, _ ->
                consoleViewModel.addLog(LogLevel.Debug, "Command received from $clientId: $cmd")
                if (cmd.startsWith("play:")) {
                    if (_isMacroExecutionEnabled.value) {
                        val macroName = cmd.substringAfter("play:")
                        val macroToPlay = macroManagerViewModel.macroFiles.value.find { it.name.equals(macroName, ignoreCase = true) }
                        if (macroToPlay != null) {
                            macroManagerViewModel.onPlayMacro(macroToPlay)
                            consoleViewModel.addLog(LogLevel.Info, "Playing macro '$macroName' for $clientId")
                        } else {
                            consoleViewModel.addLog(LogLevel.Warn, "Macro '$macroName' not found for $clientId")
                        }
                    } else {
                        consoleViewModel.addLog(LogLevel.Info, "Macro execution is disabled. Ignoring play request from $clientId")
                    }
                }
            },
            onHeartbeat = { _ -> }
        )
    }

    fun onError(error: String) {
        consoleViewModel.addLog(LogLevel.Error, "SERVER ERROR: $error")
    }
}