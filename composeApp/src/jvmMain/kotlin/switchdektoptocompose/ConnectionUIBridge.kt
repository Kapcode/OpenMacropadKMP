package switchdektoptocompose

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import java.io.File
import java.net.InetAddress
import java.security.KeyStore
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

interface ConnectionUIBridge {
    fun startListening(port: Int, encryptionEnabled: Boolean)
    suspend fun stopListening()
    fun sendData(data: ByteArray)
    fun isListening(): Boolean
    fun getConnectedClients(): List<String>
    fun disconnectClient(clientId: String)
    fun setConnectionListener(listener: ConnectionListener)
    fun sendDataToClient(clientId: String, data: ByteArray) // Added for direct messaging
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
    private var serverJob: Job? = null
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sessions = ConcurrentHashMap<String, WebSocketServerSession>()

    private var currentPort: Int = 0

    override fun startListening(port: Int, encryptionEnabled: Boolean) {
        if (serverJob?.isActive == true) {
            listener?.onError("Server is already running")
            return
        }

        currentPort = port

        val environment = if (encryptionEnabled) {
            createSecureEnvironment(currentPort)
        } else {
            createPlainEnvironment(currentPort)
        }

        if (environment == null) return

        server = embeddedServer(Netty, environment)
        serverJob = serverScope.launch {
            try {
                val serverName = try { InetAddress.getLocalHost().hostName } catch (e: Exception) { "OpenMacropad Server" }
                discovery = ServerDiscovery(serverName, currentPort)
                discovery?.start()
                server?.start(wait = true)
            } catch (e: Exception) {
                if (e is CancellationException) {
                    println("Server job cancelled.")
                } else {
                    listener?.onError("Failed to start server: ${e.message}")
                }
            } finally {
                discovery?.stop()
            }
        }
    }

    override suspend fun stopListening() {
        serverJob?.cancelAndJoin()
        server?.stop(1000, 5000)
        server = null
        sessions.clear()
        connectedClients.clear()
    }

    override fun isListening(): Boolean {
        return serverJob?.isActive == true
    }

    override fun sendData(data: ByteArray) {
        serverScope.launch {
            sessions.values.forEach { session ->
                if (session.isActive) {
                    session.send(Frame.Binary(fin = true, data))
                }
            }
        }
    }

    override fun sendDataToClient(clientId: String, data: ByteArray) {
        serverScope.launch {
            sessions[clientId]?.let {
                if (it.isActive) {
                    it.send(Frame.Binary(fin = true, data))
                }
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
        val keystoreFile = File("/home/kyle/IMPORTED_ANDROID_STUDIO_PROJECTS/OpenMacropadKMP/keystore.p12")
        val keystorePassword = "hoopla"
        val privateKeyPassword = "hoopla"
        val keyAlias = "macropad"

        if (!keystoreFile.exists()) {
            listener?.onError("Encryption enabled, but keystore not found at: ${keystoreFile.absolutePath}")
            return null
        }

        return try {
            val keyStore = KeyStore.getInstance("PKCS12").apply {
                load(keystoreFile.inputStream(), keystorePassword.toCharArray())
            }

            applicationEngineEnvironment {
                sslConnector(
                    keyStore = keyStore,
                    keyAlias = keyAlias,
                    keyStorePassword = { keystorePassword.toCharArray() },
                    privateKeyPassword = { privateKeyPassword.toCharArray() }
                ) {
                    this.port = port
                    host = "0.0.0.0"
                }
                module { moduleWithWebsockets() }
            }
        } catch (e: Exception) {
            listener?.onError("Error loading keystore: ${e.message}")
            null
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
                val deviceName = call.request.queryParameters["deviceName"] ?: "Unknown Device"
                val clientId = UUID.randomUUID().toString()
                sessions[clientId] = this
                handleNewConnection(clientId, deviceName)
                try {
                    for (frame in incoming) {
                        when(frame) {
                            is Frame.Binary -> listener?.onDataReceived(clientId, frame.readBytes())
                            is Frame.Text -> listener?.onDataReceived(clientId, frame.readBytes())
                            else -> {}
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