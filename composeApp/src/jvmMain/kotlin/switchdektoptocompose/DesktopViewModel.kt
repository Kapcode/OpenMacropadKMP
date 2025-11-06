package switchdektoptocompose

import com.kapcode.open.macropad.kmps.ConnectionListener
import com.kapcode.open.macropad.kmps.WifiServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * A data class to hold information about a connected client.
 */
data class ClientInfo(val id: String, val name: String)

/**
 * The ViewModel for the desktop application.
 */
class DesktopViewModel : ConnectionListener {

    private val wifiServer = WifiServer()
    private val port = 9999 // Define port here

    private val _connectedDevices = MutableStateFlow<List<ClientInfo>>(emptyList())
    val connectedDevices: StateFlow<List<ClientInfo>> = _connectedDevices.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _serverIpAddress = MutableStateFlow("Determining IP...")
    val serverIpAddress: StateFlow<String> = _serverIpAddress.asStateFlow()
    
    val serverPort: StateFlow<Int> = MutableStateFlow(port).asStateFlow()

    init {
        wifiServer.setConnectionListener(this)
        findLocalIpAddress()
    }

    private fun findLocalIpAddress() {
        val ip = try {
            NetworkInterface.getNetworkInterfaces().asSequence().flatMap { it.inetAddresses.asSequence() }
                .firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address }?.hostAddress
        } catch (e: Exception) {
            null
        }
        _serverIpAddress.value = ip ?: "Not Found"
    }

    fun startServer() {
        if (wifiServer.isListening()) return
        try {
            wifiServer.startListening()
            _isServerRunning.value = wifiServer.isListening()
        } catch (e: Exception) {
            e.printStackTrace()
            _isServerRunning.value = false
        }
    }

    fun stopServer() {
        if (!wifiServer.isListening()) return
        wifiServer.stopListening()
        _isServerRunning.value = false
    }

    fun shutdown() {
        stopServer()
    }

    // --- ConnectionListener Implementation ---

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
        println("Data received from $clientId")
    }

    override fun onError(error: String) {
        System.err.println("SERVER ERROR: $error")
    }
}