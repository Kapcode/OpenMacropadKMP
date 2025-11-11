package MacroKTOR

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * A wrapper for a Ktor WebSocket client that handles connection logic.
 */
class MacroKtorClient(
    private val client: HttpClient,
    private val host: String,
    private val port: Int
) {
    private var session: ClientWebSocketSession? = null
    val incomingMessages = Channel<Frame>(Channel.UNLIMITED)
    private val clientScope = CoroutineScope(Dispatchers.IO)

    /**
     * Connects to the server and suspends until the connection is established.
     * Launches a listener for incoming messages.
     * @throws Exception if the connection fails.
     */
    suspend fun connect(deviceName: String) {
        // This function now suspends until the WebSocket session is created.
        session = client.webSocketSession {
            val isSecure = port == 8443
            url(
                scheme = if (isSecure) "wss" else "ws",
                host = this@MacroKtorClient.host,
                port = this@MacroKtorClient.port,
                path = "/ws?deviceName=$deviceName"
            )
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
            }
        }
    }

    suspend fun send(message: String) {
        session?.send(Frame.Text(message))
    }

    fun close() {
        clientScope.launch {
            session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client closing connection"))
            client.close()
            println("Client closed.")
        }
    }
}