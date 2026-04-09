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
    private val lastSeen = ConcurrentHashMap<String, Long>()
    private val temporaryTrustedDevices = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null

    fun isRunning(): Boolean = server != null

    fun approveTemporaryDevice(clientId: String) {
        temporaryTrustedDevices.add(clientId)
    }

    private fun isDeviceTrusted(clientId: String): Boolean {
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
                        onClientConnected(clientId, clientName)
                    } else {
                        if (!switchdektoptocompose.logic.AppSettings.allowNewConnections) {
                            send(Frame.Binary(true, controlMessage(ControlCommand.BANNED, mapOf("reason" to "New connections disabled")).toBytes()))
                            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "New connections are disabled"))
                            return@webSocket
                        }
                        // We no longer send an empty PAIRING_PENDING here.
                        // DesktopViewModel.onPairingRequest will send it with the verification code.
                        onPairingRequest(clientId, clientName)
                    }

                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Binary) {
                                try {
                                    val dataModel = DataModel.fromBytes(frame.readBytes())
                                    lastSeen[clientId] = System.currentTimeMillis()
                                    if (isDeviceTrusted(clientId)) {
                                        dataModel.handle(
                                            onHeartbeat = { /* Already handled by Ktor or just ignore */ },
                                            onControl = { cmd, _ ->
                                                if (cmd == ControlCommand.DISCONNECT) {
                                                    close(CloseReason(CloseReason.Codes.NORMAL, "Client requested disconnect"))
                                                }
                                            },
                                            onText = { _ -> onMessageReceived(clientId, dataModel) },
                                            onCommand = { _, _ -> onMessageReceived(clientId, dataModel) }
                                        )
                                    } else {
                                        // Still handle heartbeats to update lastSeen, but drop other messages
                                        dataModel.handle(
                                            onHeartbeat = { /* Already updated lastSeen */ },
                                            onText = { _ -> /* Dropped */ },
                                            onCommand = { _, _ -> /* Dropped */ }
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else if (frame is Frame.Text) {
                                // Support legacy text messages for now, but wrap them
                                if (isDeviceTrusted(clientId)) {
                                    onMessageReceived(clientId, textMessage(frame.readText()))
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
