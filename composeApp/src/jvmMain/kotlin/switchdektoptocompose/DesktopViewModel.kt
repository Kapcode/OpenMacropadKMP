package switchdektoptocompose

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
) : ConnectionListener {

    lateinit var macroManagerViewModel: MacroManagerViewModel

    private val _encryptionEnabled = MutableStateFlow(true)
    val encryptionEnabled: StateFlow<Boolean> = _encryptionEnabled.asStateFlow()

    private val _isMacroExecutionEnabled = MutableStateFlow(true)
    val isMacroExecutionEnabled: StateFlow<Boolean> = _isMacroExecutionEnabled.asStateFlow()

    private val wifiServer = WifiServer()
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _connectedDevices = MutableStateFlow<List<ClientInfo>>(emptyList())
    val connectedDevices: StateFlow<List<ClientInfo>> = _connectedDevices.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()
    
    private val _serverIpAddress = MutableStateFlow("Determining IP...")
    val serverIpAddress: StateFlow<String> = _serverIpAddress.asStateFlow()

    init {
        wifiServer.setConnectionListener(this)
        findLocalIpAddresses()
    }

    fun setMacroExecutionEnabled(enabled: Boolean) {
        _isMacroExecutionEnabled.value = enabled
    }

    fun setEncryption(enabled: Boolean) {
        if (!_isServerRunning.value) {
            _encryptionEnabled.value = enabled
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
    }

    fun startServer() {
        if (wifiServer.isListening()) return
        try {
            val port = if (_encryptionEnabled.value) {
                settingsViewModel.secureServerPort.value
            } else {
                settingsViewModel.serverPort.value
            }
            wifiServer.startListening(port, encryptionEnabled.value)
            _isServerRunning.value = wifiServer.isListening()
        } catch (e: Exception) {
            e.printStackTrace()
            _isServerRunning.value = false
        }
    }

    fun stopServer() {
        if (!wifiServer.isListening()) return
        viewModelScope.launch {
            wifiServer.stopListening()
            _isServerRunning.value = false
        }
    }

    fun shutdown() {
        stopServer()
    }

    fun sendMacroListToAllClients() {
        val macroNames = macroManagerViewModel.macroFiles.value.map { it.name }
        val macroListString = "macros:${macroNames.joinToString(",")}"
        wifiServer.sendData(macroListString.toByteArray())
    }

    override fun onClientConnected(clientId: String, clientName: String) {
        _connectedDevices.update { currentList ->
            if (currentList.any { it.id == clientId }) currentList else currentList + ClientInfo(id = clientId, name = clientName)
        }
    }

    override fun onClientDisconnected(clientId: String) {
        _connectedDevices.update { currentList ->
            currentList.filterNot { it.id == clientId }
        }
    }

    override fun onDataReceived(clientId: String, data: ByteArray) {
        val message = String(data).trim()
        println("Data received from $clientId: $message")

        if (message == "getMacros") {
            val macroNames = macroManagerViewModel.macroFiles.value.map { it.name }
            val macroListString = "macros:${macroNames.joinToString(",")}"
            wifiServer.sendDataToClient(clientId, macroListString.toByteArray())
        } else if (message.startsWith("play:")) {
            if (_isMacroExecutionEnabled.value) {
                val macroName = message.substringAfter("play:")
                val macroToPlay = macroManagerViewModel.macroFiles.value.find { it.name.equals(macroName, ignoreCase = true) }
                if (macroToPlay != null) {
                    macroManagerViewModel.onPlayMacro(macroToPlay)
                }
            }
        }
    }

    override fun onError(error: String) {
        System.err.println("SERVER ERROR: $error")
    }
}