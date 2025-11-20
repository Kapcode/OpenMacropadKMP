package switchdektoptocompose

import MacroKTOR.MacroKtorServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * A data class to hold information about a connected client.
 */
data class ClientInfo(val id: String, val name: String)

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

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _serverIpAddress = MutableStateFlow("Determining IP...")
    val serverIpAddress: StateFlow<String> = _serverIpAddress.asStateFlow()

    private val server = MacroKtorServer(
        onMessageReceived = ::onDataReceived,
        onClientConnected = ::onClientConnected,
        onClientDisconnected = ::onClientDisconnected
    )
    private val discoveryAnnouncer = ServerDiscoveryAnnouncer()

    init {
        findLocalIpAddresses()
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

    fun startServer() {
        if (server.isRunning()) return
        consoleViewModel.addLog(LogLevel.Info, "Starting server...")
        try {
            val port = if (_encryptionEnabled.value) {
                settingsViewModel.secureServerPort.value
            } else {
                settingsViewModel.serverPort.value
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

    fun shutdown() {
        stopServer()
    }

    fun sendMacroListToAllClients() {
        val macroNames = macroManagerViewModel.macroFiles.value.map { it.name }
        val macroListString = "macros:${macroNames.joinToString(",")}"
        server.sendToAll(macroListString)
        consoleViewModel.addLog(LogLevel.Debug, "Sent macro list to all clients")
    }

    private fun onClientConnected(clientId: String, clientName: String) {
        _connectedDevices.update { currentList ->
            if (currentList.any { it.id == clientId }) currentList else currentList + ClientInfo(id = clientId, name = clientName)
        }
        consoleViewModel.addLog(LogLevel.Info, "Client connected: $clientName ($clientId)")
    }

    private fun onClientDisconnected(clientId: String) {
        val client = _connectedDevices.value.find { it.id == clientId }
        _connectedDevices.update { currentList ->
            currentList.filterNot { it.id == clientId }
        }
        consoleViewModel.addLog(LogLevel.Info, "Client disconnected: ${client?.name ?: clientId}")
    }

    private fun onDataReceived(clientId: String, message: String) {
        consoleViewModel.addLog(LogLevel.Debug, "Data received from $clientId: $message")

        if (message == "getMacros") {
            val macroNames = macroManagerViewModel.macroFiles.value.map { it.name }
            val macroListString = "macros:${macroNames.joinToString(",")}"
            viewModelScope.launch {
                server.sendToClient(clientId, macroListString)
            }
            consoleViewModel.addLog(LogLevel.Debug, "Sent macro list to $clientId")
        } else if (message.startsWith("play:")) {
            if (_isMacroExecutionEnabled.value) {
                val macroName = message.substringAfter("play:")
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
    }

    fun onError(error: String) {
        consoleViewModel.addLog(LogLevel.Error, "SERVER ERROR: $error")
    }
}