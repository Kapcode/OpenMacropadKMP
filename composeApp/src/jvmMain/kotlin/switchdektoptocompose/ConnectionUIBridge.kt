package switchdektoptocompose

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.net.InetAddress
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

interface ConnectionUIBridge {
    fun startListening(port: Int, encryptionEnabled: Boolean)
    suspend fun stopListening()
    fun sendData(data: ByteArray)
    fun isListening(): Boolean
    fun getConnectedClients(): List<String>
    fun disconnectClient(clientId: String)
    fun setConnectionListener(listener: ConnectionListener)
    fun sendDataToClient(clientId: String, data: ByteArray)
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

    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
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

        server = embeddedServer(Netty, configure = {
            if (encryptionEnabled) {
                val keystoreFile = File("keystore.p12")
                val keystorePassword = "hoopla"
                val keyAlias = "macropad"

                if (!keystoreFile.exists()) {
                    try {
                        generateKeystore(keystoreFile, keyAlias, keystorePassword)
                        listener?.onError("New keystore generated at: ${keystoreFile.absolutePath}")
                    } catch (e: Exception) {
                        listener?.onError("Failed to generate keystore: ${e.message}")
                        e.printStackTrace()
                    }
                }

                try {
                    val keyStore = KeyStore.getInstance("PKCS12").apply {
                        load(keystoreFile.inputStream(), keystorePassword.toCharArray())
                    }

                    sslConnector(
                        keyStore = keyStore,
                        keyAlias = keyAlias,
                        keyStorePassword = { keystorePassword.toCharArray() },
                        privateKeyPassword = { keystorePassword.toCharArray() }
                    ) {
                        this.port = currentPort
                        this.host = "0.0.0.0"
                    }
                } catch (e: Exception) {
                    listener?.onError("Error loading keystore: ${e.message}")
                }
            } else {
                connector {
                    this.port = currentPort
                    this.host = "0.0.0.0"
                }
            }
        }) {
            moduleWithWebsockets()
        }

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
        // Commented out to debug Ktor 2.x vs 3.x mismatch
        // server?.stop(1000.milliseconds, 5000.milliseconds)
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

    @Throws(Exception::class)
    private fun generateKeystore(keystoreFile: File, alias: String, password: String) {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair = keyPairGenerator.generateKeyPair()

        val owner = X500Name("CN=OpenMacropad")
        val now = Date()
        val expiry = Date(now.time + 3650.days.inWholeMilliseconds)
        val serialNumber = BigInteger(64, Random())

        val certBuilder = JcaX509v3CertificateBuilder(
            owner,
            serialNumber,
            now,
            expiry,
            owner,
            keyPair.public
        )

        val contentSigner = JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.private)
        val certificate = JcaX509CertificateConverter().getCertificate(certBuilder.build(contentSigner))

        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(null, password.toCharArray())
        keyStore.setKeyEntry(alias, keyPair.private, password.toCharArray(), arrayOf<X509Certificate>(certificate))

        FileOutputStream(keystoreFile).use { fos ->
            keyStore.store(fos, password.toCharArray())
        }
    }

    private fun Application.moduleWithWebsockets() {
        install(WebSockets) {
            pingPeriod = 15.seconds
            timeout = 15.seconds
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