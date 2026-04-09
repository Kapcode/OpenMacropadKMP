package MacroKTOR

import com.kapcode.open.macropad.kmps.utils.KeystoreUtils
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.slf4j.event.Level
import java.io.File
import java.io.InputStream
import java.security.KeyStore
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class MacroKtorServer(
    private val onMessageReceived: (clientId: String, message: String) -> Unit,
    private val onClientConnected: (clientId: String, clientName: String) -> Unit,
    private val onClientDisconnected: (clientId: String) -> Unit,
    private val onPairingRequest: (clientId: String, clientName: String) -> Unit
) {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private val connections = mutableMapOf<String, WebSocketServerSession>()

    fun isRunning(): Boolean = server != null

    fun start(port: Int, isSecure: Boolean) {
        if (isRunning()) return

        server = embeddedServer(Netty, configure = {
            if (isSecure) {
                val workingDir = File(System.getProperty("user.home"), ".openmacropad")
                if (!workingDir.exists()) workingDir.mkdirs()

                val keystore = KeystoreUtils.getOrCreateKeystore(workingDir)
                val keyAlias = keystore.aliases().nextElement()
                val password = System.getProperty("keystore.password") ?: "temporary-dev-password"

                sslConnector(
                    keyStore = keystore,
                    keyAlias = keyAlias,
                    keyStorePassword = { password.toCharArray() },
                    privateKeyPassword = { password.toCharArray() }
                ) {
                    this.port = port
                    this.host = "0.0.0.0"
                }
            } else {
                connector {
                    this.port = port
                    this.host = "0.0.0.0"
                }
            }
        }) {
            install(WebSockets)
            install(CallLogging) {
                level = Level.INFO
                filter { call -> call.request.path().startsWith("/") }
            }
            routing {
                webSocket("/") {
                    val queryParams = call.request.queryParameters
                    val clientName = queryParams["name"] ?: queryParams["deviceName"] ?: "Unknown"
                    val clientId = queryParams["id"] ?: UUID.randomUUID().toString()

                    connections[clientId] = this

                    if (switchdektoptocompose.logic.TrustedDeviceManager.isTrusted(clientId)) {
                        onClientConnected(clientId, clientName)
                    } else {
                        send(Frame.Text("pairing_pending"))
                        onPairingRequest(clientId, clientName)
                    }

                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                if (switchdektoptocompose.logic.TrustedDeviceManager.isTrusted(clientId)) {
                                    onMessageReceived(clientId, frame.readText())
                                } else {
                                    // Drop messages from untrusted devices
                                    send(Frame.Text("pairing_pending"))
                                }
                            }
                        }
                    } catch (e: ClosedReceiveChannelException) {
                        // Client disconnected
                    } finally {
                        connections.remove(clientId)
                        onClientDisconnected(clientId)
                    }
                }
            }
        }
        server?.start(wait = false)
    }

    fun stop() {
        // Commented out to debug Ktor 2.x vs 3.x mismatch
        // server?.stop(1000.milliseconds, 5000.milliseconds)
        server = null
    }

    suspend fun sendToClient(clientId: String, message: String) {
        connections[clientId]?.send(Frame.Text(message))
    }

    suspend fun sendToAll(message: String) {
        connections.values.forEach { it.send(Frame.Text(message)) }
    }
}
