package com.kapcode.open.macropad.kmps

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import java.io.File
import java.security.KeyStore
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

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
    protected val connectedClients = ConcurrentHashMap<String, String>() // Store ID and Name

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
    private val port: Int = 8443,
) : Wifi() {

    private var server: NettyApplicationEngine? = null
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sessions = ConcurrentHashMap<String, WebSocketServerSession>()

    override fun startListening() {
        if (server != null) {
            listener?.onError("Server is already running")
            return
        }

        val keystoreFile = File("keystore.jks") // Ensure this is in the right path
        if (!keystoreFile.exists()) {
            listener?.onError("Keystore not found. Cannot start secure server.")
            return
        }

        val keyAlias = "selfsigned"
        val keystorePassword = "password"
        val privateKeyPassword = "password"

        val environment = applicationEngineEnvironment {
            sslConnector(
                keyStore = KeyStore.getInstance(keystoreFile, keystorePassword.toCharArray()),
                keyAlias = keyAlias,
                keyStorePassword = { keystorePassword.toCharArray() },
                privateKeyPassword = { privateKeyPassword.toCharArray() }
            ) {
                port = this@WifiServer.port
                host = "0.0.0.0"
            }

            module {
                install(WebSockets) {
                    pingPeriod = Duration.ofSeconds(15)
                    timeout = Duration.ofSeconds(15)
                    maxFrameSize = Long.MAX_VALUE
                    masking = false
                }
                routing {
                    webSocket("/ws") {
                        val clientId = call.request.queryParameters["clientId"] ?: "UnknownDevice"
                        sessions[clientId] = this
                        handleNewConnection(clientId, clientId)
                        try {
                            for (frame in incoming) {
                                if (frame is Frame.Binary) {
                                    val data = frame.readBytes()
                                    listener?.onDataReceived(clientId, data)
                                }
                            }
                        } finally {
                            sessions.remove(clientId)
                            handleDisconnection(clientId)
                        }
                    }
                }
            }
        }

        server = embeddedServer(Netty, environment)
        serverScope.launch {
            try {
                server?.start(wait = true)
            } catch (e: Exception) {
                listener?.onError("Server failed to start: ${e.message}")
            }
        }
    }

    override fun stopListening() {
        server?.stop(1000, 5000)
        server = null
        sessions.clear()
        connectedClients.clear()
    }

    override fun sendData(data: ByteArray) {
        serverScope.launch {
            sessions.values.forEach { session ->
                session.send(Frame.Binary(fin = true, data))
            }
        }
    }

    fun sendDataToClient(clientId: String, data: ByteArray) {
        serverScope.launch {
            sessions[clientId]?.send(Frame.Binary(fin = true, data))
        }
    }

    override fun isListening(): Boolean {
        return server != null
    }

    override fun disconnectClient(clientId: String) {
        serverScope.launch {
            sessions[clientId]?.close(CloseReason(CloseReason.Codes.NORMAL, "Disconnected by server"))
            sessions.remove(clientId)
            super.disconnectClient(clientId)
        }
    }
}