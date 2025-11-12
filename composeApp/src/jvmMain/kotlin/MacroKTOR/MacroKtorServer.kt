package MacroKTOR

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import java.io.File
import java.security.KeyStore
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MacroKtorServer(
    private val onMessageReceived: (clientId: String, message: String) -> Unit,
    private val onClientConnected: (clientId: String, clientName: String) -> Unit,
    private val onClientDisconnected: (clientId: String) -> Unit
) {

    private var server: NettyApplicationEngine? = null
    private val connections = ConcurrentHashMap<String, WebSocketSession>()

    fun start(port: Int, useEncryption: Boolean) {
        if (server != null) {
            println("Server is already running.")
            return
        }

        val environment = applicationEngineEnvironment {
            if (useEncryption) {
                // Using the keystore from the project directory.
                val keystoreFile = File("keystore.p12")
                if (keystoreFile.exists()) {
                    val keyStore = KeyStore.getInstance("PKCS12")
                    keystoreFile.inputStream().use { keyStore.load(it, "hoopla".toCharArray()) }
                    sslConnector(
                        keyStore = keyStore,
                        keyAlias = "macropad",
                        keyStorePassword = { "hoopla".toCharArray() },
                        privateKeyPassword = { "hoopla".toCharArray() }
                    ) { this.port = port }
                } else {
                    // Fail hard if encryption is requested but the keystore is missing.
                    throw RuntimeException("Keystore not found at ${keystoreFile.absolutePath}. Cannot start encrypted server.")
                }
            } else {
                connector { this.port = port }
            }
            module(createModule())
        }

        server = embeddedServer(Netty, environment).apply {
            start(wait = false)
        }
    }

    fun stop() {
        server?.stop(1, 5, TimeUnit.SECONDS)
        server = null
        connections.clear()
    }

    fun isRunning(): Boolean {
        return server != null
    }

    suspend fun sendToClient(clientId: String, message: String) {
        connections[clientId]?.send(Frame.Text(message))
    }

    fun sendToAll(message: String) {
        connections.values.forEach { session ->
            session.launch {
                session.send(Frame.Text(message))
            }
        }
    }

    private fun createModule(): Application.() -> Unit = {
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(15)
            timeout = Duration.ofSeconds(15)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }
        routing {
            webSocket("/ws") {
                val clientId = call.request.queryParameters["clientId"] ?: "Unknown-${java.util.UUID.randomUUID()}"
                val clientName = call.request.queryParameters["deviceName"] ?: "Unknown Device"
                connections[clientId] = this
                onClientConnected(clientId, clientName)
                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            onMessageReceived(clientId, text)
                        }
                    }
                } finally {
                    connections.remove(clientId)
                    onClientDisconnected(clientId)
                }
            }
        }
    }
}

// Main function for standalone testing
fun main() {
    println("Starting test server...")
    val server = MacroKtorServer(
        onMessageReceived = { clientId, message ->
            println("Message from $clientId: $message")
        },
        onClientConnected = { clientId, clientName ->
            println("Client connected: $clientName ($clientId)")
        },
        onClientDisconnected = { clientId ->
            println("Client disconnected: $clientId")
        }
    )
    // Example: Start an encrypted server on port 8443
    // Make sure keystore.p12 exists in your user home .open-macropad directory
    server.start(8443, true)

    // To stop the server, you would call server.stop()
    // For this standalone test, it will run until the process is killed.
    Runtime.getRuntime().addShutdownHook(Thread {
        println("Stopping server...")
        server.stop()
        println("Server stopped.")
    })

    Thread.currentThread().join()
}