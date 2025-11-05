package com.kapcode.open.macropad.kmp

import Model.DataModel
import Model.handle
import Model.dataMessage // Added import for dataMessage
import Model.errorMessage // Added import for errorMessage
import com.kapcode.open.macropad.kmp.network.sockets.Server.Server
import kotlin.concurrent.thread

//purpose is to bridge the gap between the UI and the connection library I have not created yet.
//this will also handle communication between usb and bluetooth as well as Wi-Fi. (the network library will, this is the stand in for that)
//UI will communicate with this interface


interface ConnectionUIBridge {
    fun startListening()
    fun stopListening()
    fun sendData(data: ByteArray)
    fun isListening(): Boolean
    fun getConnectedClients(): List<String> // or return client info
    fun disconnectClient(clientId: String)
    // Add callback/listener for when clients connect/disconnect
    fun setConnectionListener(listener: ConnectionListener)
}

interface ConnectionListener {
    fun onClientConnected(clientId: String)
    fun onClientDisconnected(clientId: String)
    fun onDataReceived(clientId: String, data: ByteArray)
    fun onError(error: String)
}


abstract class Wifi : ConnectionUIBridge {
    protected var listener: ConnectionListener? = null
    protected val connectedClients = mutableSetOf<String>()

    override fun setConnectionListener(listener: ConnectionListener) {//runs on connect or disconnect
        this.listener = listener
    }

    override fun getConnectedClients(): List<String> {
        return connectedClients.toList()
    }

    protected fun handleNewConnection(clientId: String) {
        connectedClients.add(clientId)
        listener?.onClientConnected(clientId)
    }

    protected fun handleDisconnection(clientId: String) {
        connectedClients.remove(clientId)
        listener?.onClientDisconnected(clientId)
    }

    override fun disconnectClient(clientId: String) {
        if (connectedClients.contains(clientId)) {
            // Subclass should implement actual disconnection logic
            handleDisconnection(clientId)
        }
    }



    // Then in your subclasses, you can call the listener methods like:
    // listener?.onClientConnected(clientId)
    // listener?.onDataReceived(clientId, data)
}

/**
 * WiFi server implementation using the KotlinNetworkLibrary Server
 */
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
        
        // Create server with callbacks
        server = Server(
            port = port,
            maxClients = maxClients,
            onClientConnected = { clientId, secureSocket ->
                handleNewConnection(clientId)
            },
            onClientDisconnected = { clientId ->
                handleDisconnection(clientId)
            },
            onMessageReceived = { clientId, dataModel ->
                // Extract data from DataModel and notify listener
                handleReceivedMessage(clientId, dataModel)
            },
            onError = { context, dataModel -> // Now expects a DataModel as the second argument
                listener?.onError("Server error in $context: ${dataModel.messageType}") // Use dataModel for error info
            }
        )
        
        try {
            server?.start()
        } catch (e: Exception) {
            listener?.onError("Failed to start server: ${e.message ?: "Unknown error"}") // Safely access message
        }
    }
    
    override fun stopListening() {
        server?.stop()
        connectedClients.clear()
    }
    
    override fun sendData(data: ByteArray) {
        // Broadcast data to all connected clients
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
        // Handle different message types
        dataModel.handle(
            onText = { text ->
                listener?.onDataReceived(clientId, text.toByteArray())
            },
            onCommand = { command, params ->
                // You can handle commands differently if needed
                val commandString = "COMMAND:$command:$params"
                listener?.onDataReceived(clientId, commandString.toByteArray())
            },
            onData = { key, value ->
                listener?.onDataReceived(clientId, value)
            },
            onHeartbeat = { timestamp ->
                // Heartbeat - typically no need to notify UI
            },
            onResponse = { success, message, data ->
                val responseString = "RESPONSE:$success:$message:$data"
                listener?.onDataReceived(clientId, responseString.toByteArray())
            }
        )
    }
    
    private fun createDataMessage(data: ByteArray): DataModel {
        // Create a DataModel message with the data
        // Assuming you have a way to create data messages
        // You may need to import the appropriate factory methods
        return dataMessage("data", data)
    }
}