package MacroKTOR

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    fun connect() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isSecure = port == 8443 // Simple check for secure port

                client.webSocket(
                    method = HttpMethod.Get,
                    host = host,
                    port = port,
                    path = "/ws"
                ) {
                    // Apply wss scheme if secure
                    if (isSecure) {
                        //url.protocol = URLProtocol.WSS
                    }
                    session = this
                    for (frame in incoming) {
                        incomingMessages.send(frame)
                    }
                }
            } catch (e: Exception) {
                println("Error connecting: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    suspend fun send(message: String) {
        withContext(Dispatchers.IO) {
            session?.send(Frame.Text(message))
        }
    }

    fun close() {
        CoroutineScope(Dispatchers.IO).launch {
            session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client closing connection"))
            client.close()
            println("Client closed.")
        }
    }
}
