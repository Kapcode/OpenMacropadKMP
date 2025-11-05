package com.kapcode.open.macropad.kmp.network.ktor

import com.kapcode.open.macropad.kmp.network.sockets.Model.DataModel
import com.kapcode.open.macropad.kmp.network.sockets.Model.MessageType
import com.kapcode.open.macropad.kmp.network.sockets.Model.heartbeatMessage
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
// import kotlinx.serialization.json.Json // Removed, using DataModel.Json
import java.util.concurrent.ConcurrentHashMap

class KtorServer(
    private val port: Int = 9999,
    private val onClientConnected: ((String) -> Unit)? = null,
    private val onClientDisconnected: ((String) -> Unit)? = null,
    private val onMessageReceived: ((String, DataModel) -> Unit)? = null,
    private val onError: ((String, Throwable) -> Unit)? = null
) {

    private var server: NettyApplicationEngine? = null
    private val connections = ConcurrentHashMap<String, DefaultWebSocketSession>()
    private val clientEncryptionManagers = ConcurrentHashMap<String, EncryptionManager>()
    private val clientKeyExchangeStatus = ConcurrentHashMap<String, Boolean>()

    private var clientCounter = 0 // Simple counter for client IDs

    fun start() {
        server = embeddedServer(Netty, port = port) {
            install(WebSockets) {
                pingPeriodMillis = 15_000
                timeoutMillis = 15_000
                maxFrameSize = 500_000
                masking = false
            }
            install(ContentNegotiation) {
                json(DataModel.Json) // Use shared Json instance
            }
            routing {
                webSocket("/") {
                    val currentSession = this
                    val clientId = "client-${clientCounter++}"
                    connections[clientId] = currentSession
                    clientEncryptionManagers[clientId] = EncryptionManager()
                    clientKeyExchangeStatus[clientId] = false // Key exchange not complete initially

                    onClientConnected?.invoke(clientId)
                    log("Client $clientId connected! Starting key exchange.")

                    try {
                        val encryptionManager = clientEncryptionManagers[clientId]!!

                        // --- Key Exchange Phase ---\n                        // Expect client's public key as an unencrypted text frame
                        val clientPublicKeyFrame = incoming.receive() // Await the first frame
                        if (clientPublicKeyFrame is Frame.Text) {
                            val clientPublicKeyJson = clientPublicKeyFrame.readText()
                            val clientPublicKeyDataModel = DataModel.Json.decodeFromString(DataModel.serializer(), clientPublicKeyJson)

                            if (clientPublicKeyDataModel.messageType is MessageType.PublicKeyExchange) {
                                encryptionManager.completeKeyExchange(clientPublicKeyDataModel.messageType.publicKey)
                                log("Client $clientId public key received and processed.")

                                // Send server's public key back to client
                                val serverPublicKey = encryptionManager.initializeKeyExchange()
                                val serverPublicKeyMessage = DataModel(messageType = MessageType.PublicKeyExchange(serverPublicKey))
                                val serverPublicKeyJson = DataModel.Json.encodeToString(DataModel.serializer(), serverPublicKeyMessage)
                                currentSession.send(Frame.Text(serverPublicKeyJson))
                                log("Server public key sent to client $clientId. Key exchange complete.")

                                clientKeyExchangeStatus[clientId] = true // Mark key exchange as complete for this client
                            } else {
                                throw IllegalStateException("Unexpected message type during key exchange from client $clientId: ${clientPublicKeyDataModel.messageType::class.simpleName}")
                            }
                        } else {
                            throw IllegalStateException("Expected text frame for public key exchange from client $clientId, but got ${clientPublicKeyFrame::class.simpleName}")
                        }

                        // --- Encrypted Communication Phase ---\n                        for (frame in incoming) {
                            val dataModel: DataModel = when (frame) {
                                is Frame.Binary -> {
                                    if (!encryptionManager.isReady() || clientKeyExchangeStatus[clientId] != true) {
                                        throw IllegalStateException("Received encrypted frame but encryption not ready for client $clientId.")
                                    }
                                    val encryptedBytes = frame.readBytes()
                                    val decryptedBytes = encryptionManager.decrypt(encryptedBytes)
                                    val jsonString = decryptedBytes.decodeToString()
                                    DataModel.Json.decodeFromString(DataModel.serializer(), jsonString)
                                }
                                is Frame.Text -> {
                                    // After key exchange, text frames are generally unexpected for application data
                                    log("Client $clientId sent unencrypted text frame after key exchange. Treating as error.")
                                    throw IllegalStateException("Received unencrypted text frame after key exchange from client $clientId.")
                                }
                                else -> continue // Ignore other frame types
                            }
                            onMessageReceived?.invoke(clientId, dataModel)
                            handleMessage(currentSession, clientId, dataModel) // Handle the decrypted message
                        }
                    } catch (e: ClosedReceiveChannelException) {
                        log("Client $clientId disconnected normally: ${e.message}")
                    } catch (e: Exception) {
                        log("Client $clientId error: ${e.message}")
                        onError?.invoke(clientId, e)
                    } finally {
                        connections.remove(clientId)
                        clientEncryptionManagers.remove(clientId)
                        clientKeyExchangeStatus.remove(clientId)
                        onClientDisconnected?.invoke(clientId)
                        log("Client $clientId disconnected. Cleaned up resources.")
                    }
                }
            }
        }.start(wait = false)
        log("Ktor Server started on port $port")
    }

    fun stop() {
        server?.stop(1000, 2000)
        log("Ktor Server stopped")
    }

    private suspend fun sendEncryptedOrUnencrypted(clientId: String, session: DefaultWebSocketSession, message: DataModel): Boolean {
        val encryptionManager = clientEncryptionManagers[clientId]
        val isKeyExchangeDone = clientKeyExchangeStatus[clientId] ?: false

        return try {
            if (message.messageType is MessageType.PublicKeyExchange || !isKeyExchangeDone) {
                // Send public key exchange messages or if encryption isn't ready yet, unencrypted
                val jsonString = DataModel.Json.encodeToString(DataModel.serializer(), message)
                session.send(Frame.Text(jsonString))
                true
            } else if (encryptionManager != null && encryptionManager.isReady()) {
                // Encrypt and send as binary frame
                val jsonString = DataModel.Json.encodeToString(DataModel.serializer(), message)
                val rawBytes = jsonString.encodeToByteArray()
                val encryptedBytes = encryptionManager.encrypt(rawBytes)
                session.send(Frame.Binary(true, encryptedBytes))
                true
            } else {
                log("Cannot send message to client $clientId: Encryption not ready or manager not found.")
                false
            }
        } catch (e: Exception) {
            log("Failed to send to client $clientId: ${e.message}")
            onError?.invoke(clientId, e)
            false
        }
    }

    suspend fun sendToClient(clientId: String, message: DataModel): Boolean {
        val targetSession = connections[clientId]
        return if (targetSession != null) {
            sendEncryptedOrUnencrypted(clientId, targetSession, message)
        } else {
            log("Client $clientId not found for sending message.")
            false
        }
    }

    suspend fun broadcast(message: DataModel, excludeClientId: String? = null) {
        connections.forEach { (clientId, session) ->
            if (clientId != excludeClientId) {
                sendEncryptedOrUnencrypted(clientId, session, message)
            }
        }
    }
    
    fun getConnectedClients(): List<String> {
        return connections.keys.toList()
    }

    fun getClientCount(): Int {
        return connections.size
    }

    private suspend fun handleMessage(session: DefaultWebSocketSession, clientId: String, message: DataModel) {
        // Messages received here are already decrypted
        when (val msg = message.messageType) {
            is MessageType.Text -> {
                log("$clientId sent text: '${msg.content}'")
                sendEncryptedOrUnencrypted(clientId, session, message.createResponse(true, "Text received: ${msg.content}"))
            }
            is MessageType.Command -> {
                log("$clientId sent command: ${msg.command} with params: ${msg.parameters}")
                handleCommand(session, clientId, message, msg.command, msg.parameters)
            }
            is MessageType.Data -> {
                log("$clientId sent data: ${msg.key} (${msg.value.size} bytes)")
                sendEncryptedOrUnencrypted(clientId, session, message.createResponse(true, "Data received"))
            }
            is MessageType.Heartbeat -> {
                // Respond to heartbeat
                sendEncryptedOrUnencrypted(clientId, session, heartbeatMessage())
            }
            is MessageType.Response -> {
                log("$clientId sent response: ${msg.message}")
            }
            is MessageType.PublicKeyExchange -> {
                // This should have been handled during the initial key exchange phase.
                log("Client $clientId sent PublicKeyExchange message after handshake. Ignoring.")
            }
        }
    }

    private suspend fun handleCommand(
        session: DefaultWebSocketSession,
        clientId: String,
        originalMessage: DataModel,
        command: String,
        params: Map<String, String>
    ) {
        when (command.lowercase()) {
            "ping" -> {
                sendEncryptedOrUnencrypted(clientId, session, originalMessage.createResponse(true, "pong"))
            }
            "echo" -> {
                val text = params["message"] ?: "no message"
                sendEncryptedOrUnencrypted(clientId, session, originalMessage.createResponse(true, text))
            }
            "info" -> {
                sendEncryptedOrUnencrypted(clientId, session, originalMessage.createResponse(true, "Server info", DataModel.Json.encodeToString(mapOf(
                    "connectedClients" to connections.size.toString(),
                    "serverPort" to port.toString()
                )) ))
            }
            "disconnect" -> {
                sendEncryptedOrUnencrypted(clientId, session, originalMessage.createResponse(true, "Disconnecting..."))
                session.close(CloseReason(CloseReason.Codes.NORMAL, "Client requested disconnect"))
            }
            else -> {
                sendEncryptedOrUnencrypted(clientId, session, originalMessage.createResponse(false, "Unknown command: $command"))
            }
        }
    }

    private fun log(message: String) {
        println("[KtorServer] $message")
    }
}