package MacroKTOR

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MacroKtorClient(private val host: String, private val port: Int) {

    private val client = HttpClient {
        install(WebSockets)
    }

    private var session: DefaultClientWebSocketSession? = null
    val incomingMessages = Channel<Frame>(Channel.UNLIMITED)

    fun connect() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.webSocket(
                    method = HttpMethod.Get,
                    host = host,
                    port = port,
                    path = "/ws"
                ) {
                    session = this
                    while (true) {
                        val frame = incoming.receive()
                        incomingMessages.send(frame)
                    }
                }
            } catch (e: Exception) {
                println("Error connecting: ${e.message}")
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
            session?.close()
            client.close()
        }
    }
}
