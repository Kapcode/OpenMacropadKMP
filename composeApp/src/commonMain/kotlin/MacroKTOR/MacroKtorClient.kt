package MacroKTOR

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

/**
 * A wrapper for a Ktor WebSocket client that handles connection logic.
 */
class MacroKtorClient(
    private val client: HttpClient,
    private val host: String,
    private val port: Int,
    private val isSecure: Boolean // Explicitly tell the client if WSS should be used
) {
    private var session: ClientWebSocketSession? = null
    val incomingMessages = Channel<Frame>(Channel.UNLIMITED)
    private val clientScope = CoroutineScope(Dispatchers.IO)
    private var heartbeatJob: Job? = null

    /**
     * Connects to the server and suspends until the connection is established.
     * Launches a listener for incoming messages.
     * @throws Exception if the connection fails.
     */
    suspend fun connect(deviceName: String) {
        val identityManager = com.kapcode.open.macropad.kmps.IdentityManager()
        val publicKey = identityManager.getIdentityPublicKey()
        val fingerprint = com.kapcode.open.macropad.kmps.utils.Base64Utils.encode(publicKey)

        // This function now suspends until the WebSocket session is created.
        session = client.webSocketSession {
            url(
                scheme = if (isSecure) "wss" else "ws",
                host = this@MacroKtorClient.host,
                port = this@MacroKtorClient.port,
                path = "/?name=$deviceName&deviceName=$deviceName&id=$fingerprint"
            )
        }

        // Start heartbeat sender
        heartbeatJob?.cancel()
        heartbeatJob = clientScope.launch {
            while (isActive) {
                delay(5000)
                try {
                    session?.send(Frame.Binary(true, com.kapcode.open.macropad.kmps.network.sockets.model.heartbeatMessage().toBytes()))
                } catch (e: Exception) {
                    break
                }
            }
        }

        // Launch a separate coroutine to listen for messages.
        clientScope.launch {
            try {
                session?.let {
                    for (frame in it.incoming) {
                        incomingMessages.send(frame)
                    }
                }
            } catch (e: Exception) {
                println("Message listener error: ${e.message}")
            } finally {
                heartbeatJob?.cancel()
            }
        }
    }

    suspend fun send(bytes: ByteArray) {
        session?.send(Frame.Binary(true, bytes))
    }

    suspend fun send(message: String) {
        session?.send(Frame.Text(message))
    }

    fun close() {
        heartbeatJob?.cancel()
        clientScope.launch {
            session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client closing connection"))
            // Do not close the underlying HttpClient here, as it's managed by the Activity.
            println("Client session closed.")
        }
    }
}