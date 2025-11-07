package MacroKTOR

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A wrapper for a Ktor WebSocket client that handles connection logic.
 *
 * @param client A pre-configured Ktor HttpClient. The platform-specific
 *               code is responsible for creating and configuring this client
 *               (e.g., setting up mTLS).
 * @param host The hostname or IP address of the server.
 * @param port The port to connect to on the server.
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
                // Use the webSocketSession block for a more robust connection
                client.webSocketSession {
                    url("wss", host, port, "/ws")
                }.also { session = it }

                // Once connected, listen for incoming messages in a separate coroutine
                // so the connect function can complete.
                launch {
                    try {
                        for (frame in session!!.incoming) {
                            incomingMessages.send(frame)
                        }
                    } catch (e: Exception) {
                        println("Error during message reception: ${e.message}")
                    } finally {
                        println("Incoming message loop ended.")
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
