package MacroKTOR

import com.kapcode.open.macropad.kmps.network.sockets.model.handle
import com.kapcode.open.macropad.kmps.utils.HashUtils
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
        
        // Calculate the fingerprint as a SHA-256 hash (hex) to match server's new security requirements
        val digest = HashUtils.sha256(publicKey)
        val fingerprint = digest.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

        println("MacroKtorClient: Connecting to $host:$port (Secure: $isSecure) with id: $fingerprint")

        // Use safe URL builder to ensure parameters are properly encoded (e.g., '+' in Base64)
        session = client.webSocketSession {
            url {
                protocol = if (isSecure) URLProtocol.WSS else URLProtocol.WS
                this.host = this@MacroKtorClient.host
                this.port = this@MacroKtorClient.port
                path("/")
                parameters.append("name", deviceName)
                parameters.append("deviceName", deviceName)
                parameters.append("id", fingerprint)
            }
        }

        println("MacroKtorClient: WebSocket session established")

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
                session?.let { s ->
                    for (frame in s.incoming) {
                        if (frame is Frame.Binary) {
                            try {
                                val dataModel = com.kapcode.open.macropad.kmps.network.sockets.model.DataModel.fromBytes(frame.readBytes())
                                dataModel.handle(
                                    onControl = { cmd: com.kapcode.open.macropad.kmps.network.sockets.model.ControlCommand, params: Map<String, String> ->
                                        if (cmd == com.kapcode.open.macropad.kmps.network.sockets.model.ControlCommand.AUTH_CHALLENGE) {
                                            val challenge = params["challenge"]
                                            if (challenge != null) {
                                                clientScope.launch {
                                                    println("MacroKtorClient: Received challenge, signing...")
                                                    val sig = identityManager.signMessage(challenge.encodeToByteArray())
                                                    val response = com.kapcode.open.macropad.kmps.network.sockets.model.controlMessage(
                                                        com.kapcode.open.macropad.kmps.network.sockets.model.ControlCommand.AUTH_RESPONSE,
                                                        mapOf(
                                                            "signature" to com.kapcode.open.macropad.kmps.utils.Base64Utils.encode(sig),
                                                            "publicKey" to com.kapcode.open.macropad.kmps.utils.Base64Utils.encode(publicKey),
                                                            "metadata" to com.kapcode.open.macropad.kmps.DeviceInfo.hardwareMetadata
                                                        )
                                                    )
                                                    send(response.toBytes())
                                                    println("MacroKtorClient: Auth response sent")
                                                }
                                            }
                                        }
                                    }
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
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

    fun close() {
        heartbeatJob?.cancel()
        clientScope.launch {
            session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client closing connection"))
            // Do not close the underlying HttpClient here, as it's managed by the Activity.
            println("Client session closed.")
        }
    }
}