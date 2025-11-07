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

// ... (Interfaces and Wifi abstract class remain the same)
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
    private var discovery: ServerDiscovery? = null
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sessions = ConcurrentHashMap<String, WebSocketServerSession>()

    override fun startListening() {
        if (server != null) {
            listener?.onError("Server is already running")
            return
        }

        val keystoreStream: InputStream? = Thread.currentThread().contextClassLoader.getResourceAsStream("keystore.jks")

        if (keystoreStream == null) {
            listener?.onError("Keystore not found in resources. Please ensure 'keystore.jks' is in 'src/jvmMain/resources'.")
            return
        }

        val keyAlias = "selfsigned"
        val keystorePassword = "password"
        val privateKeyPassword = "password"

        val keyStore = KeyStore.getInstance("JKS")
        keystoreStream.use { keyStore.load(it, keystorePassword.toCharArray()) }

        val environment = applicationEngineEnvironment {
            sslConnector(
                keyStore = keyStore,
                keyAlias = keyAlias,
                keyStorePassword = { keystorePassword.toCharArray() },
                privateKeyPassword = { privateKeyPassword.toCharArray() }
            ) {
                port = this@WifiServer.port
                host = "0.0.0.0"
            }
            module {
                install(WebSockets) { /* ... */ }
                routing {
                    webSocket("/ws") { /* ... */ }
                }
            }
        }

        server = embeddedServer(Netty, environment)
        serverScope.launch {
            try {
                // Start broadcasting server presence
                val serverName = try { InetAddress.getLocalHost().hostName } catch (e: Exception) { "OpenMacropad Server" }
                discovery = ServerDiscovery(serverName, port)
                discovery?.start()

                server?.start(wait = true)
            } catch (e: Exception) {
                listener?.onError("Failed to start server: ${e.message}")
                discovery?.stop()
            }
        }
    }

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
    
    // ... rest of the class is the same ...
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