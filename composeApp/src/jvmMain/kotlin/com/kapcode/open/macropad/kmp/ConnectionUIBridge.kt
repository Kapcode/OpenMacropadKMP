package com.kapcode.open.macropad.kmp

import Model.DataModel
import Model.dataMessage
import Model.errorMessage
import Model.handle
import com.kapcode.open.macropad.kmp.network.sockets.Server.Server
import java.net.InetAddress
import kotlin.concurrent.thread

interface ConnectionUIBridge {
    fun startListening()
    fun stopListening()
    fun sendData(data: ByteArray)
    fun isListening(): Boolean
    fun getConnectedClients(): List<String>
    fun disconnectClient(clientId: String)
    fun setConnectionListener(listener: ConnectionListener)
}

interface ConnectionListener {
    fun onClientConnected(clientId: String, clientName: String)
    fun onClientDisconnected(clientId: String)
    fun onDataReceived(clientId: String, data: ByteArray)
    fun onError(error: String)
}

abstract class Wifi : ConnectionUIBridge {
    protected var listener: ConnectionListener? = null
    protected val connectedClients = mutableMapOf<String, String>() // Store ID and Name

    override fun setConnectionListener(listener: ConnectionListener) {
        this.listener = listener
    }

    override fun getConnectedClients(): List<String> {
        return connectedClients.values.toList()
    }

    protected fun handleNewConnection(clientId: String, clientName: String) {
        connectedClients[clientId] = clientName
        listener?.onClientConnected(clientId, clientName)
    }

    protected fun handleDisconnection(clientId: String) {
        connectedClients.remove(clientId)
        listener?.onClientDisconnected(clientId)
    }

    override fun disconnectClient(clientId: String) {
        if (connectedClients.containsKey(clientId)) {
            handleDisconnection(clientId)
        }
    }
}

class WifiServer(
    private val port: Int = 9999,
    private val maxClients: Int = 50
) : Wifi() {

    private var server: Server? = null

    override fun startListening() {
        if (server?.isRunning() == true) {
            listener?.onError("Server is already running")
            return
        }

        val serverName = try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            "Desktop Server"
        }

        server = Server(
            port = port,
            maxClients = maxClients,
            serverName = serverName,
            onClientConnected = { clientId, clientName, secureSocket ->
                handleNewConnection(clientId, clientName)
            },
            onClientDisconnected = { clientId ->
                handleDisconnection(clientId)
            },
            onMessageReceived = { clientId, dataModel ->
                handleReceivedMessage(clientId, dataModel)
            },
            onError = { context, dataModel ->
                listener?.onError("Server error in $context: ${dataModel.messageType}")
            }
        )

        try {
            server?.start()
        } catch (e: Exception) {
            listener?.onError("Failed to start server: ${e.message ?: "Unknown error"}")
        }
    }
    
    // ... (rest of WifiServer is unchanged)

    override fun stopListening() {
        server?.stop()
        connectedClients.clear()
    }

    override fun sendData(data: ByteArray) {
        val dataModel = createDataMessage(data)
        server?.broadcast(dataModel)
    }

    fun sendDataToClient(clientId: String, data: ByteArray) {
        val dataModel = createDataMessage(data)
        server?.sendToClient(clientId, dataModel)
    }

    override fun isListening(): Boolean {
        return server?.isRunning() ?: false
    }

    override fun disconnectClient(clientId: String) {
        server?.disconnectClient(clientId)
        super.disconnectClient(clientId)
    }

    private fun handleReceivedMessage(clientId: String, dataModel: DataModel) {
        dataModel.handle(
            onText = { text ->
                listener?.onDataReceived(clientId, text.toByteArray())
            },
            onCommand = { command, params ->
                val commandString = "COMMAND:$command:$params"
                listener?.onDataReceived(clientId, commandString.toByteArray())
            },
            onData = { _, value ->
                listener?.onDataReceived(clientId, value)
            },
            onHeartbeat = { _ -> },
            onResponse = { success, message, data ->
                val responseString = "RESPONSE:$success:$message:$data"
                listener?.onDataReceived(clientId, responseString.toByteArray())
            }
        )
    }

    private fun createDataMessage(data: ByteArray): DataModel {
        return dataMessage("data", data)
    }
}