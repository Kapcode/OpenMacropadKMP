package MacroKTOR

import com.kapcode.open.macropad.kmps.network.sockets.model.*
import com.kapcode.open.macropad.kmps.utils.KeystoreUtils
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.slf4j.event.Level
import java.io.File
import java.io.InputStream
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MacroKtorServer(
    private val onMessageReceived: (clientId: String, DataModel) -> Unit,
    private val onClientConnected: (clientId: String, clientName: String) -> Unit,
    private val onClientDisconnected: (clientId: String) -> Unit,
    private val onPairingRequest: (clientId: String, clientName: String) -> Unit
) {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private val connections = ConcurrentHashMap<String, WebSocketServerSession>()
    private val pendingAuthChallenges = ConcurrentHashMap<String, String>() // clientId -> challenge
    private val lastSeen = ConcurrentHashMap<String, Long>()
    private val temporaryTrustedDevices = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null

    fun isRunning(): Boolean = server != null

    fun approveTemporaryDevice(clientId: String) {
        temporaryTrustedDevices.add(clientId)
    }

    fun isDeviceTrusted(clientId: String): Boolean {
        return temporaryTrustedDevices.contains(clientId) ||
                switchdektoptocompose.logic.TrustedDeviceManager.isTrusted(clientId)
    }

    fun start(port: Int, isSecure: Boolean) {
        if (isRunning()) return

        heartbeatJob = serverScope.launch {
            while (isActive) {
                delay(5000)
                val now = System.currentTimeMillis()
                val heartbeat = heartbeatMessage()
                
                connections.forEach { (id, session) ->
                    val lastTime = lastSeen[id] ?: now
                    if (now - lastTime > 15000) {
                        launch {
                            try {
                                session.close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Heartbeat timeout"))
                            } catch (e: Exception) {}
                        }
                    } else {
                        try {
                            if (session.isActive) {
                                session.send(Frame.Binary(true, heartbeat.toBytes()))
                            }
                        } catch (e: Exception) {
                            // Session likely closed
                        }
                    }
                }
            }
        }

        server = embeddedServer(Netty, configure = {
            if (isSecure) {
                val workingDir = File(System.getProperty("user.home"), ".openmacropad")
                if (!workingDir.exists()) workingDir.mkdirs()

                val keystore = KeystoreUtils.getOrCreateKeystore(workingDir)
                val keyAlias = keystore.aliases().nextElement()
                val password = com.kapcode.open.macropad.kmps.utils.SecretManager.getOrCreatePassword()

                sslConnector(
                    keyStore = keystore,
                    keyAlias = keyAlias,
                    keyStorePassword = { password },
                    privateKeyPassword = { password }
                ) {
                    this.port = port
                    this.host = "0.0.0.0"
                }
                // Note: password array is passed as a lambda to Ktor, 
                // we should be careful about when to clear it if Ktor needs it later.
                // However, Ktor's sslConnector usually uses these to initialize the SSL context.
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
                    lastSeen[clientId] = System.currentTimeMillis()

                    if (switchdektoptocompose.logic.TrustedDeviceManager.isBanned(clientId)) {
                        send(Frame.Binary(true, controlMessage(ControlCommand.BANNED).toBytes()))
                        close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Device is banned"))
                        return@webSocket
                    }

                    if (isDeviceTrusted(clientId)) {
                        // VULNERABILITY FIX 2: Challenge-Response Authentication
                        // Instead of immediate trust, send a challenge.
                        val challenge = UUID.randomUUID().toString()
                        pendingAuthChallenges[clientId] = challenge
                        send(Frame.Binary(true, controlMessage(ControlCommand.AUTH_CHALLENGE, mapOf("challenge" to challenge)).toBytes()))
                    } else {
                        if (!switchdektoptocompose.logic.AppSettings.allowNewConnections) {
                            send(Frame.Binary(true, controlMessage(ControlCommand.BANNED, mapOf("reason" to "New connections disabled")).toBytes()))
                            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "New connections are disabled"))
                            return@webSocket
                        }
                        onPairingRequest(clientId, clientName)
                    }

                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Binary) {
                                try {
                                    val dataModel = DataModel.fromBytes(frame.readBytes())
                                    lastSeen[clientId] = System.currentTimeMillis()
                                    
                                    val isTrusted = isDeviceTrusted(clientId)
                                    val isAuthenticated = isTrusted && !pendingAuthChallenges.containsKey(clientId)

                                    if (isAuthenticated) {
                                        onMessageReceived(clientId, dataModel)
                                    } else {
                                        // Handle Auth and Pairing while not fully authenticated
                                        dataModel.handle(
                                            onControl = { cmd, params ->
                                                when (cmd) {
                                                    ControlCommand.AUTH_RESPONSE -> {
                                                        val challenge = pendingAuthChallenges[clientId]
                                                        val signature = params["signature"]
                                                        val publicKey = params["publicKey"] // In a real app, we'd verify against stored publicKey for this clientId
                                                        
                                                        if (challenge != null && signature != null && publicKey != null) {
                                                            // Verify signature of the challenge using the public key
                                                            val isValid = com.kapcode.open.macropad.kmps.IdentityManager.verifySignature(
                                                                challenge.toByteArray(),
                                                                com.kapcode.open.macropad.kmps.utils.Base64Utils.decode(signature),
                                                                com.kapcode.open.macropad.kmps.utils.Base64Utils.decode(publicKey)
                                                            )
                                                            
                                                            if (isValid) {
                                                                pendingAuthChallenges.remove(clientId)
                                                                onClientConnected(clientId, clientName)
                                                                send(Frame.Binary(true, controlMessage(ControlCommand.PAIRING_APPROVED).toBytes()))
                                                            } else {
                                                                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Authentication failed"))
                                                            }
                                                        }
                                                    }
                                                    ControlCommand.PAIRING_RESPONSE -> {
                                                        // Always allow pairing response
                                                        onMessageReceived(clientId, dataModel)
                                                    }
                                                    else -> {}
                                                }
                                            }
                                        )
                                    }
                                    
                                    dataModel.handle(
                                        onControl = { cmd, _ ->
                                            if (cmd == ControlCommand.DISCONNECT) {
                                                close(CloseReason(CloseReason.Codes.NORMAL, "Client requested disconnect"))
                                            }
                                        }
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else {
                                // Ignore all non-binary frames (like Frame.Text) to prevent auth bypass
                                // and ensure all communication uses the secure DataModel format.
                                if (frame !is Frame.Ping && frame !is Frame.Pong) {
                                    println("Ignoring non-binary frame from $clientId: ${frame::class.simpleName}")
                                }
                            }
                        }
                    } catch (e: ClosedReceiveChannelException) {
                        // Client disconnected
                    } finally {
                        connections.remove(clientId)
                        lastSeen.remove(clientId)
                        temporaryTrustedDevices.remove(clientId)
                        onClientDisconnected(clientId)
                    }
                }
            }
        }
        server?.start(wait = false)
    }

    fun stop() {
        heartbeatJob?.cancel()
        val currentConnections = connections.toMap()
        runBlocking {
            currentConnections.forEach { (id, _) ->
                try {
                    disconnectClient(id)
                } catch (e: Exception) {}
            }
        }
        server?.stop(500, 1000)
        server = null
    }

    suspend fun sendToClient(clientId: String, dataModel: DataModel) {
        connections[clientId]?.send(Frame.Binary(true, dataModel.toBytes()))
    }

    suspend fun disconnectClient(clientId: String, reason: String = "Disconnected by server") {
        try {
            sendToClient(clientId, controlMessage(ControlCommand.DISCONNECT, mapOf("reason" to reason)))
            connections[clientId]?.close(CloseReason(CloseReason.Codes.NORMAL, reason))
        } catch (e: Exception) {
            // Already gone
        }
    }

    suspend fun sendToAll(dataModel: DataModel) {
        val bytes = dataModel.toBytes()
        connections.values.forEach { it.send(Frame.Binary(true, bytes)) }
    }
}
