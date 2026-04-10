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
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import switchdektoptocompose.logic.AppSettings
import switchdektoptocompose.logic.TrustedDeviceManager
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

class MacroKtorServer(
    private val appSettings: AppSettings,
    private val trustedDeviceManager: TrustedDeviceManager,
    private val onMessageReceived: (clientId: String, DataModel) -> Unit,
    private val onClientConnected: (clientId: String, clientName: String) -> Unit,
    private val onClientDisconnected: (clientId: String) -> Unit,
    private val onPairingRequest: (clientId: String, clientName: String) -> Unit
) {
    private val logger = LoggerFactory.getLogger(MacroKtorServer::class.java)
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private val clients = ConcurrentHashMap<String, ConnectedClient>()
    private val temporaryTrustedDevices = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val serverScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var watchdogJob: Job? = null

    private enum class AuthStatus {
        CHALLENGE_PENDING,
        AUTHENTICATED,
        BANNED
    }

    private data class ConnectedClient(
        val id: String,
        val name: String,
        val session: DefaultWebSocketServerSession,
        var authStatus: AuthStatus,
        var lastSeen: Long,
        var pendingChallenge: String? = null,
        var pongsSinceLastPing: Int = 0
    )

    fun isRunning(): Boolean = server != null

    fun approveTemporaryDevice(clientId: String) {
        temporaryTrustedDevices.add(clientId)
    }

    fun isDeviceTrusted(clientId: String): Boolean {
        return temporaryTrustedDevices.contains(clientId) ||
                trustedDeviceManager.isTrusted(clientId)
    }

    /**
     * Explicitly marks a client as authenticated.
     * Useful for manual pairing approval flow.
     */
    fun authenticateClient(clientId: String) {
        clients[clientId]?.let { client ->
            client.authStatus = AuthStatus.AUTHENTICATED
            client.pendingChallenge = null
            client.lastSeen = System.currentTimeMillis() // Reset timeout upon authentication
            logger.info("Client {} manually promoted to AUTHENTICATED", clientId)
            onClientConnected(client.id, client.name)
        }
    }

    fun start(port: Int, isSecure: Boolean) {
        if (isRunning()) return

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
            } else {
                connector {
                    this.port = port
                    this.host = "0.0.0.0"
                }
            }
        }) {
            install(WebSockets) {
                if (appSettings.enableWebsocketPings) {
                    pingPeriod = 5.seconds
                    timeout = 60.seconds
                } else {
                    pingPeriod = null
                    timeout = 3600.seconds // Effectively disabled native timeout
                }
            }
            install(CallLogging) {
                level = Level.INFO
                filter { call -> call.request.path().startsWith("/") }
            }
            routing {
                webSocket("/") {
                    val queryParams = call.request.queryParameters
                    val clientName = queryParams["name"] ?: queryParams["deviceName"] ?: "Unknown"
                    val clientId = queryParams["id"] ?: UUID.randomUUID().toString()

                    val client = ConnectedClient(
                        id = clientId,
                        name = clientName,
                        session = this,
                        authStatus = AuthStatus.CHALLENGE_PENDING,
                        lastSeen = System.currentTimeMillis()
                    )
                    clients[clientId] = client

                    handleSession(client)
                }
            }
        }
        server?.start(wait = false)
        logger.info("Server started on port {} (secure={})", port, isSecure)

        // Mandatory application-level heartbeat watchdog
        watchdogJob?.cancel()
        watchdogJob = serverScope.launch {
            var reportTimer = 0
            while (isActive) {
                delay(5000)
                reportTimer += 5000
                val now = System.currentTimeMillis()
                val isReportIteration = reportTimer >= 30000

                clients.forEach { (id, client) ->
                    if (isReportIteration) {
                        if (logger.isTraceEnabled) {
                            logger.trace("ping---- echo verbose ({}/12 expected) pongs received from {}", client.pongsSinceLastPing, id)
                        }
                        client.pongsSinceLastPing = 0
                    }

                    // For authenticated clients, disconnect if no heartbeat for 60 seconds.
                    // For clients in pairing (CHALLENGE_PENDING), be much more lenient (5 minutes).
                    val timeout = if (client.authStatus == AuthStatus.AUTHENTICATED) 60000 else 300000
                    
                    if (now - client.lastSeen > timeout) {
                        logger.warn("Watchdog: Client {} ({}) timed out. Last seen {}ms ago.", id, client.authStatus, now - client.lastSeen)
                        launch { 
                            try {
                                disconnectClient(id, "Heartbeat timeout")
                            } catch (e: Exception) {
                                logger.error("Failed to disconnect timed-out client {}", id, e)
                            }
                        }
                    }
                }
                if (isReportIteration) reportTimer = 0
            }
        }
    }

    private suspend fun DefaultWebSocketServerSession.handleSession(client: ConnectedClient) {
        logger.info("New connection from {} ({})", client.name, client.id)

        try {
            if (trustedDeviceManager.isBanned(client.id)) {
                client.authStatus = AuthStatus.BANNED
                send(Frame.Binary(true, controlMessage(ControlCommand.BANNED).toBytes()))
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Device is banned"))
                return
            }

            if (isDeviceTrusted(client.id)) {
                val challenge = UUID.randomUUID().toString()
                client.pendingChallenge = challenge
                logger.info("Sending AUTH_CHALLENGE to trusted device {}", client.id)
                send(Frame.Binary(true, controlMessage(ControlCommand.AUTH_CHALLENGE, mapOf("challenge" to challenge)).toBytes()))
                // Notify the ViewModel that a trusted client has connected
                onClientConnected(client.id, client.name)
            } else {
                if (!appSettings.allowNewConnections) {
                    send(Frame.Binary(true, controlMessage(ControlCommand.BANNED, mapOf("reason" to "New connections disabled")).toBytes()))
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "New connections are disabled"))
                    return
                }
                logger.info("New pairing request for device {} ({})", client.name, client.id)
                onPairingRequest(client.id, client.name)
                
                val workingDir = File(System.getProperty("user.home"), ".openmacropad")
                val keystore = KeystoreUtils.getOrCreateKeystore(workingDir)
                val fingerprint = KeystoreUtils.getCertificateFingerprint(keystore)
                send(Frame.Binary(true, controlMessage(ControlCommand.PAIRING_PENDING, mapOf("fingerprint" to fingerprint)).toBytes()))
            }

            for (frame in incoming) {
                when (frame) {
                    is Frame.Binary -> processBinaryFrame(client, frame.readBytes())
                    is Frame.Text -> logger.warn("Ignoring text frame from {}", client.id)
                    is Frame.Pong -> {
                        client.pongsSinceLastPing++
                        client.lastSeen = System.currentTimeMillis()
                    }
                    else -> { /* Ktor handles Ping/Pong */ }
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            logger.info("Client disconnected: {}", client.id)
        } catch (e: Exception) {
            logger.error("Error in session for {}", client.id, e)
        } finally {
            // Only remove from clients if this is still the active session for this ID
            if (clients[client.id] === client) {
                clients.remove(client.id)
                temporaryTrustedDevices.remove(client.id)
                onClientDisconnected(client.id)
            } else {
                logger.debug("Session for {} finished, but a newer session is already active. Skipping removal.", client.id)
            }
        }
    }

    private suspend fun processBinaryFrame(client: ConnectedClient, bytes: ByteArray) {
        try {
            val dataModel = DataModel.fromBytes(bytes)
            client.lastSeen = System.currentTimeMillis()

            // Handle heartbeats immediately and reply to keep the client's watchdog happy
            if (dataModel.messageType is MessageType.Heartbeat) {
                client.pongsSinceLastPing++
                try {
                    client.session.send(Frame.Binary(true, heartbeatMessage().toBytes()))
                } catch (e: Exception) {
                    logger.error("Failed to send heartbeat reply to {}", client.id)
                }
                return 
            }

            // Auto-promote to AUTHENTICATED if the device is now trusted and we aren't waiting for a challenge.
            // This handles the transition after manual pairing approval in the UI.
            if (client.authStatus == AuthStatus.CHALLENGE_PENDING && client.pendingChallenge == null && isDeviceTrusted(client.id)) {
                logger.info("Client {} automatically promoted to AUTHENTICATED (now trusted)", client.id)
                client.authStatus = AuthStatus.AUTHENTICATED
            }
            
            if (client.authStatus == AuthStatus.AUTHENTICATED) {
                logger.info("Received message from authenticated client {}: {}", client.id, dataModel.messageType)
                onMessageReceived(client.id, dataModel)
            } else {
                handleUnauthenticatedMessage(client, dataModel)
            }
            
            dataModel.handle(
                onControl = { cmd, _ ->
                    if (cmd == ControlCommand.DISCONNECT) {
                        client.session.close(CloseReason(CloseReason.Codes.NORMAL, "Client requested disconnect"))
                    }
                }
            )
        } catch (e: Exception) {
            logger.error("Failed to process binary frame from {}", client.id, e)
        }
    }

    private suspend fun handleUnauthenticatedMessage(client: ConnectedClient, dataModel: DataModel) {
        dataModel.handle(
            onControl = { cmd, params ->
                when (cmd) {
                    ControlCommand.AUTH_RESPONSE -> handleAuthResponse(client, params)
                    ControlCommand.PAIRING_RESPONSE -> {
                        logger.info("Forwarding PAIRING_RESPONSE from unauthenticated client {}", client.id)
                        onMessageReceived(client.id, dataModel)
                    }
                    else -> logger.warn("Ignoring control command {} from unauthenticated client {}", cmd, client.id)
                }
            },
            onCommand = { cmd, _ ->
                logger.warn("Ignoring macro command {} from unauthenticated client {}", cmd, client.id)
            },
            onText = { text ->
                logger.warn("Ignoring text message '{}' from unauthenticated client {}", text, client.id)
            }
        )
    }

    private suspend fun handleAuthResponse(client: ConnectedClient, params: Map<String, String>) {
        val challenge = client.pendingChallenge
        val signature = params["signature"]
        val publicKeyBase64 = params["publicKey"]

        if (challenge != null && signature != null && publicKeyBase64 != null) {
            val hashedPublicKey = hashPublicKey(publicKeyBase64)
            if (client.id != hashedPublicKey) {
                logger.error("Security Alert: Identity mismatch for {}. Expected {}, received publicKey hash {}", client.id, client.id, hashedPublicKey)
                client.session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Identity mismatch"))
                // Do not remove here, finally block handles it correctly with the identity check
                return
            }

            val isValid = com.kapcode.open.macropad.kmps.IdentityManager.verifySignature(
                challenge.toByteArray(),
                com.kapcode.open.macropad.kmps.utils.Base64Utils.decode(signature),
                com.kapcode.open.macropad.kmps.utils.Base64Utils.decode(publicKeyBase64)
            )

            if (isValid) {
                client.authStatus = AuthStatus.AUTHENTICATED
                client.pendingChallenge = null
                client.lastSeen = System.currentTimeMillis()
                // Do not call onClientConnected(client.id, client.name) here as it was already called in handleSession or approveDevice
                client.session.send(Frame.Binary(true, controlMessage(ControlCommand.PAIRING_APPROVED).toBytes()))
                onMessageReceived(client.id, getMacrosRequest())
            } else {
                logger.warn("Authentication failed for {}", client.id)
                client.session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Authentication failed"))
            }
        }
    }

    private fun hashPublicKey(publicKeyBase64: String): String {
        val bytes = com.kapcode.open.macropad.kmps.utils.Base64Utils.decode(publicKeyBase64)
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun stop() {
        watchdogJob?.cancel()
        val currentClients = clients.values.toList()
        runBlocking {
            currentClients.forEach { client ->
                try {
                    disconnectClient(client.id)
                } catch (e: Exception) {}
            }
        }
        server?.stop(500, 1000)
        server = null
    }

    suspend fun sendToClient(clientId: String, dataModel: DataModel) {
        clients[clientId]?.session?.send(Frame.Binary(true, dataModel.toBytes()))
    }

    suspend fun disconnectClient(clientId: String, reason: String = "Disconnected by server") {
        clients[clientId]?.let { client ->
            try {
                client.session.send(Frame.Binary(true, controlMessage(ControlCommand.DISCONNECT, mapOf("reason" to reason)).toBytes()))
                client.session.close(CloseReason(CloseReason.Codes.NORMAL, reason))
            } catch (e: Exception) {}
            clients.remove(clientId)
        }
    }

    suspend fun sendToAll(dataModel: DataModel) {
        val bytes = dataModel.toBytes()
        clients.values.forEach { it.session.send(Frame.Binary(true, bytes)) }
    }
}
