package MacroKTOR

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.slf4j.event.Level
import java.io.InputStream
import java.security.KeyStore

class MacroKtorServer(
    private val onMessageReceived: (clientId: String, message: String) -> Unit,
    private val onClientConnected: (clientId: String, clientName: String) -> Unit,
    private val onClientDisconnected: (clientId: String) -> Unit
) {
    private var server: NettyApplicationEngine? = null
    private val connections = mutableMapOf<String, WebSocketServerSession>()

    fun isRunning(): Boolean = server != null

    fun start(port: Int, isSecure: Boolean) {
        if (isRunning()) return

        val environment = applicationEngineEnvironment {
            // Define the application modules
            module {
                install(WebSockets)
                install(CallLogging) {
                    level = Level.INFO
                    filter { call -> call.request.path().startsWith("/") }
                }
                routing {
                    webSocket("/") {
                        val clientId = call.request.queryParameters["id"] ?: "UnknownDevice"
                        val clientName = call.request.queryParameters["name"] ?: "Unknown"
                        connections[clientId] = this
                        onClientConnected(clientId, clientName)
                        try {
                            for (frame in incoming) {
                                if (frame is Frame.Text) {
                                    onMessageReceived(clientId, frame.readText())
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

            // Configure connectors at the top level
            if (isSecure) {
                val keystoreStream: InputStream? = this::class.java.classLoader.getResourceAsStream("keystore.p12")
                if (keystoreStream == null) {
                    throw RuntimeException("Keystore not found in resources. Cannot start encrypted server.")
                }

                val keystorePassword = "n678nbccfibliboo"
                val privateKeyPassword = "n678nbccfibliboo"
                val keyAlias = "your-alias-name"

                val keystore = KeyStore.getInstance("PKCS12")
                keystore.load(keystoreStream, keystorePassword.toCharArray())

                sslConnector(
                    keyStore = keystore,
                    keyAlias = keyAlias,
                    keyStorePassword = { keystorePassword.toCharArray() },
                    privateKeyPassword = { privateKeyPassword.toCharArray() }
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
        }

        server = embeddedServer(Netty, environment)
        server?.start(wait = false)
    }

    suspend fun stop() {
        server?.stop(1000, 5000)
        server = null
    }

    suspend fun sendToClient(clientId: String, message: String) {
        connections[clientId]?.send(Frame.Text(message))
    }

    suspend fun sendToAll(message: String) {
        connections.values.forEach { it.send(Frame.Text(message)) }
    }
}