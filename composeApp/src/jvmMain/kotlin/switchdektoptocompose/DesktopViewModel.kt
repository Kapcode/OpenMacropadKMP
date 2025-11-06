package switchdektoptocompose

import com.kapcode.open.macropad.kmps.ConnectionListener
import com.kapcode.open.macropad.kmps.WifiServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A data class to hold information about a connected client.
 */
data class ClientInfo(val id: String, val name: String)

/**
 * The ViewModel for the desktop application.
 *
 * This class owns the WifiServer, manages its lifecycle, and exposes its state
 * to the Compose UI in a reactive way using StateFlow.
 */
class DesktopViewModel : ConnectionListener {

    private val wifiServer = WifiServer()

    private val _connectedDevices = MutableStateFlow<List<ClientInfo>>(emptyList())
    val connectedDevices: StateFlow<List<ClientInfo>> = _connectedDevices.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    init {
        // Set this ViewModel as the listener for server events.
        wifiServer.setConnectionListener(this)
    }

    fun startServer() {
        if (wifiServer.isListening()) return
        try {
            wifiServer.startListening()
            _isServerRunning.value = wifiServer.isListening()
        } catch (e: Exception) {
            // In a real app, you'd expose this error to the UI.
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
        // Add the new client to our list, ensuring no duplicates.
        _connectedDevices.update { currentList ->
            if (currentList.any { it.id == clientId }) {
                currentList // Already exists, do nothing
            } else {
                currentList + ClientInfo(id = clientId, name = clientName)
            }
        }
    }

    override fun onClientDisconnected(clientId: String) {
        // Remove the client with the matching ID.
        _connectedDevices.update { currentList ->
            currentList.filterNot { it.id == clientId }
        }
    }

    override fun onDataReceived(clientId: String, data: ByteArray) {
        // Placeholder for future console/logging UI.
        println("Data received from $clientId")
    }

    override fun onError(error: String) {
        // Placeholder for future error display in the UI.
        System.err.println("SERVER ERROR: $error")
    }
}