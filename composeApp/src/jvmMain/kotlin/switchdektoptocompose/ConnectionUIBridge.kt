package switchdektoptocompose

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import java.io.InputStream
import java.net.InetAddress
import java.security.KeyStore
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

// ... (interfaces and abstract class are unchanged)
interface ConnectionUIBridge {
    fun startListening(encryptionEnabled: Boolean)
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
    protected val connectedClients = ConcurrentHashMap<String, String>()

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
class WifiServer : Wifi() {

    private var server: NettyApplicationEngine? = null
    private var discovery: ServerDiscovery? = null
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sessions = ConcurrentHashMap<String, WebSocketServerSession>()

    private var currentPort: Int = 0

    override fun startListening(encryptionEnabled: Boolean) {
        if (server != null) {
            listener?.onError("Server is already running")
            return
        }
        
        currentPort = if (encryptionEnabled) 8443 else 8080

        val environment = if (encryptionEnabled) {
            createSecureEnvironment(currentPort)
        } else {
            createPlainEnvironment(currentPort)
        }

        if (environment == null) return // Error creating environment

        server = embeddedServer(Netty, environment)
        serverScope.launch {
            try {
                val serverName = try { InetAddress.getLocalHost().hostName } catch (e: Exception) { "OpenMacropad Server" }
                discovery = ServerDiscovery(serverName, currentPort)
                discovery?.start()

                server?.start(wait = true)
            } catch (e: Exception) {
                listener?.onError("Failed to start server: ${e.message}")
                discovery?.stop()
            }
        }
    }

    private fun createPlainEnvironment(port: Int): ApplicationEngineEnvironment {
        return applicationEngineEnvironment {
            connector {
                this.port = port
                host = "0.0.0.0"
            }
            module { moduleWithWebsockets() }
        }
    }

    private fun createSecureEnvironment(port: Int): ApplicationEngineEnvironment? {
        val keystoreStream: InputStream? = Thread.currentThread().contextClassLoader.getResourceAsStream("keystore.jks")
        if (keystoreStream == null) {
            listener?.onError("Encryption enabled, but keystore not found in resources.")
            return null
        }

        val keyStore = KeyStore.getInstance("JKS").apply {
            load(keystoreStream, "password".toCharArray())
        }

        return applicationEngineEnvironment {
            sslConnector(
                keyStore = keyStore,
                keyAlias = "selfsigned",
                keyStorePassword = { "password".toCharArray() },
                privateKeyPassword = { "password".toCharArray() }
            ) {
                this.port = port
                host = "0.0.0.0"
               // clientAuth = io.ktor.server.engine.ClientAuth.NONE // No client auth for now
            }
            module { moduleWithWebsockets() }
        }
    }

    private fun Application.moduleWithWebsockets() {
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
    
    // ... rest of WifiServer is unchanged ...
    override fun stopListening() {
        discovery?.stop()
        server?.stop(1000, 5000)
        server = null
        sessions.clear()
        connectedClients.clear()
    }

    override fun isListening(): Boolean {
        return server != null
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

    override fun disconnectClient(clientId: String) {
        serverScope.launch {
            sessions[clientId]?.close(CloseReason(CloseReason.Codes.NORMAL, "Disconnected by server"))
            sessions.remove(clientId)
            super.disconnectClient(clientId)
        }
    }
}