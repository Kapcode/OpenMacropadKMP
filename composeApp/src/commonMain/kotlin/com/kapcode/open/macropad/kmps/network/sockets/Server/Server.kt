package com.kapcode.open.macropad.kmps.network.sockets.Server

import Model.*
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class Server(
    private val port: Int = 9999,
    private val maxClients: Int = 50,
    private val serverName: String = "OpenMacropad Server",
    private val onClientConnected: ((clientId: String, clientName: String, secureSocket: SecureSocket) -> Unit)? = null,
    private val onClientDisconnected: ((String) -> Unit)? = null,
    private val onMessageReceived: ((String, DataModel) -> Unit)? = null,
    private val onError: ((String, DataModel) -> Unit)? = null
) {

    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val clientCounter = AtomicInteger(0)
    private val executorService = Executors.newFixedThreadPool(maxClients)
    private val clients = ConcurrentHashMap<String, ClientHandler>()

    fun start() {
        if (isRunning.get()) {
            log("Server is already running")
            return
        }

        try {
            serverSocket = ServerSocket(port)
            isRunning.set(true)
            log("Server started on port $port")

            Thread {
                acceptConnections()
            }.apply {
                name = "Server-Accept-Thread"
                isDaemon = false
            }.start()

        } catch (e: Exception) {
            log("Failed to start server: ${e.message}")
            onError?.invoke("start", errorMessage("Failed to start server: ${e.message}"))
            e.printStackTrace()
            throw e
        }
    }

    fun stop() {
        if (!isRunning.get()) {
            log("Server is not running")
            return
        }

        log("Stopping server...")
        isRunning.set(false)
        clients.values.forEach { it.disconnect() }
        clients.clear()
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            log("Error closing server socket: ${e.message}")
            e.printStackTrace()
        }
        executorService.shutdown()
        log("Server stopped")
    }

    private fun acceptConnections() {
        while (isRunning.get()) {
            try {
                val clientSocket = serverSocket?.accept() ?: break
                if (clients.size >= maxClients) {
                    log("Max clients reached, rejecting connection from ${clientSocket.inetAddress}")
                    clientSocket.close()
                    continue
                }
                val clientId = "Client-${clientCounter.incrementAndGet()}"
                log("$clientId connected from ${clientSocket.inetAddress}")
                executorService.submit {
                    handleClient(clientId, clientSocket)
                }
            } catch (e: SocketException) {
                if (isRunning.get()) {
                    log("Socket error: ${e.message}")
                    onError?.invoke("accept", errorMessage("Socket error: ${e.message}"))
                    e.printStackTrace()
                }
                break
            } catch (e: Exception) {
                log("Error accepting connection: ${e.message}")
                onError?.invoke("accept", errorMessage("Error accepting connection: ${e.message}"))
                e.printStackTrace()
            }
        }
    }

    private fun handleClient(clientId: String, socket: Socket) {
        var secureSocket: SecureSocket? = null
        try {
            log("$clientId: Performing handshake...")
            val (newSecureSocket, clientName) = SecureSocket.serverHandshake(socket, serverName)
            secureSocket = newSecureSocket
            log("$clientId: Handshake complete. Client name: $clientName")

            val clientHandler = ClientHandler(clientId, secureSocket)
            clients[clientId] = clientHandler
            onClientConnected?.invoke(clientId, clientName, secureSocket)

            while (isRunning.get() && secureSocket.isConnected()) {
                val message = secureSocket.receive()
                if (message == null) {
                    log("$clientId: Connection closed by client")
                    break
                }
                log("$clientId: Received ${message.messageType::class.simpleName}")
                onMessageReceived?.invoke(clientId, message)
                clientHandler.handleMessage(message)
            }
        } catch (e: Exception) {
            log("$clientId: Error - ${e.message}")
            onError?.invoke(clientId, errorMessage("Error handling client: ${e.message}"))
            e.printStackTrace()
        } finally {
            clients.remove(clientId)
            secureSocket?.close()
            onClientDisconnected?.invoke(clientId)
            log("$clientId: Disconnected")
        }
    }
    
    // ... (rest of the Server class is unchanged)

    fun sendToClient(clientId: String, message: DataModel): Boolean {
        val client = clients[clientId]
        return if (client != null) {
            try {
                client.send(message)
                true
            } catch (e: Exception) {
                log("Failed to send to $clientId: ${e.message}")
                e.printStackTrace()
                false
            }
        } else {
            log("Client $clientId not found")
            false
        }
    }

    fun broadcast(message: DataModel, excludeClientId: String? = null) {
        clients.forEach { (clientId, client) ->
            if (clientId != excludeClientId) {
                try {
                    client.send(message)
                } catch (e: Exception) {
                    log("Failed to broadcast to $clientId: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    fun getConnectedClients(): List<String> = clients.keys.toList()
    fun getClientCount(): Int = clients.size
    fun isRunning(): Boolean = isRunning.get()
    fun disconnectClient(clientId: String) {
        clients[clientId]?.disconnect()
    }

    private fun log(message: String) = println("[Server] $message")

    private inner class ClientHandler(
        val clientId: String,
        private val secureSocket: SecureSocket
    ) {
        fun send(message: DataModel) = secureSocket.send(message)

        fun handleMessage(message: DataModel) {
            message.handle(
                onText = { text ->
                    log("$clientId sent text: '$text'")
                    send(message.createResponse(true, "Text received: $text"))
                },
                onCommand = { command, params ->
                    log("$clientId sent command: $command with params: $params")
                    handleCommand(message, command, params)
                },
                onData = { key, value ->
                    log("$clientId sent data: $key (${value.size} bytes)")
                    send(message.createResponse(true, "Data received"))
                },
                onHeartbeat = { timestamp ->
                    send(heartbeatMessage())
                },
                onResponse = { _, _, _ -> }
            )
        }

        private fun handleCommand(original: DataModel, command: String, params: Map<String, String>) {
            when (command.lowercase()) {
                "ping" -> send(original.createResponse(true, "pong"))
                "echo" -> send(textMessage(params["message"] ?: "no message"))
                "info" -> send(original.createResponse(true, "Server info", mapOf(
                    "connectedClients" to getClientCount().toString(),
                    "serverPort" to port.toString()
                )))
                "disconnect" -> {
                    send(original.createResponse(true, "Disconnecting..."))
                    disconnect()
                }
                else -> send(original.createResponse(false, "Unknown command: $command"))
            }
        }

        fun disconnect() {
            try {
                secureSocket.close()
            } catch (e: Exception) {
                log("Error disconnecting $clientId: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}