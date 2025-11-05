package com.kapcode.open.macropad.kmp.network.sockets.Server

import Model.*
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Secure server that handles encrypted client connections
 */
class Server(
    private val port: Int = 9999,
    private val maxClients: Int = 50,
    private val onClientConnected: ((String, SecureSocket) -> Unit)? = null,
    private val onClientDisconnected: ((String) -> Unit)? = null,
    private val onMessageReceived: ((String, DataModel) -> Unit)? = null,
    private val onError: ((String, DataModel) -> Unit)? = null // Corrected onError to use DataModel
) {

    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val clientCounter = AtomicInteger(0)

    // Thread pool for handling client connections
    private val executorService = Executors.newFixedThreadPool(maxClients)

    // Store active client connections
    private val clients = ConcurrentHashMap<String, ClientHandler>()

    /**
     * Start the server
     */
    fun start() {
        if (isRunning.get()) {
            log("Server is already running")
            return
        }

        try {
            serverSocket = ServerSocket(port)
            isRunning.set(true)
            log("Server started on port $port")

            // Accept connections in a separate thread
            Thread {
                acceptConnections()
            }.apply {
                name = "Server-Accept-Thread"
                isDaemon = false
            }.start()

        } catch (e: Exception) {
            log("Failed to start server: ${e.message}")
            onError?.invoke("start", errorMessage("Failed to start server: ${e.message}")) // Use errorMessage
            e.printStackTrace() // Log stack trace
            throw e
        }
    }

    /**
     * Stop the server and disconnect all clients
     */
    fun stop() {
        if (!isRunning.get()) {
            log("Server is not running")
            return
        }

        log("Stopping server...")
        isRunning.set(false)

        // Close all client connections
        clients.values.forEach { it.disconnect() }
        clients.clear()

        // Close server socket
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            log("Error closing server socket: ${e.message}")
            e.printStackTrace() // Log stack trace
        }

        // Shutdown thread pool
        executorService.shutdown()

        log("Server stopped")
    }

    /**
     * Accept incoming client connections
     */
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

                // Handle client in thread pool
                executorService.submit {
                    handleClient(clientId, clientSocket)
                }

            } catch (e: SocketException) {
                if (isRunning.get()) {
                    log("Socket error: ${e.message}")
                    onError?.invoke("accept", errorMessage("Socket error: ${e.message}")) // Use errorMessage
                    e.printStackTrace() // Log stack trace
                }
                // Socket closed, server is stopping
                break
            } catch (e: Exception) {
                log("Error accepting connection: ${e.message}")
                onError?.invoke("accept", errorMessage("Error accepting connection: ${e.message}")) // Use errorMessage
                e.printStackTrace() // Log stack trace
            }
        }
    }

    /**
     * Handle a client connection
     */
    private fun handleClient(clientId: String, socket: Socket) {
        var secureSocket: SecureSocket? = null

        try {
            // Perform handshake
            log("$clientId: Performing handshake...")
            secureSocket = SecureSocket.serverHandshake(socket)
            log("$clientId: Handshake complete")

            // Create client handler
            val clientHandler = ClientHandler(clientId, secureSocket)
            clients[clientId] = clientHandler

            // Notify connection
            onClientConnected?.invoke(clientId, secureSocket)

            // Message loop
            while (isRunning.get() && secureSocket.isConnected()) {
                val message = secureSocket.receive()

                if (message == null) {
                    log("$clientId: Connection closed by client")
                    break
                }

                log("$clientId: Received ${message.messageType::class.simpleName}")

                // Notify message received
                onMessageReceived?.invoke(clientId, message)

                // Handle message
                clientHandler.handleMessage(message)
            }

        } catch (e: Exception) {
            log("$clientId: Error - ${e.message}")
            onError?.invoke(clientId, errorMessage("Error handling client: ${e.message}")) // Use errorMessage
            e.printStackTrace() // Log stack trace
        } finally {
            // Cleanup
            clients.remove(clientId)
            secureSocket?.close()
            onClientDisconnected?.invoke(clientId)
            log("$clientId: Disconnected")
        }
    }

    /**
     * Send a message to a specific client
     */
    fun sendToClient(clientId: String, message: DataModel): Boolean {
        val client = clients[clientId]
        return if (client != null) {
            try {
                client.send(message)
                true
            } catch (e: Exception) {
                log("Failed to send to $clientId: ${e.message}")
                e.printStackTrace() // Log stack trace
                false
            }
        } else {
            log("Client $clientId not found")
            false
        }
    }

    /**
     * Broadcast a message to all connected clients
     */
    fun broadcast(message: DataModel, excludeClientId: String? = null) {
        clients.forEach { (clientId, client) ->
            if (clientId != excludeClientId) {
                try {
                    client.send(message)
                } catch (e: Exception) {
                    log("Failed to broadcast to $clientId: ${e.message}")
                    e.printStackTrace() // Log stack trace
                }
            }
        }
    }

    /**
     * Get list of connected client IDs
     */
    fun getConnectedClients(): List<String> {
        return clients.keys.toList()
    }

    /**
     * Get number of connected clients
     */
    fun getClientCount(): Int {
        return clients.size
    }

    /**
     * Check if server is running
     */
    fun isRunning(): Boolean {
        return isRunning.get()
    }

    /**
     * Disconnect a specific client
     */
    fun disconnectClient(clientId: String) {
        clients[clientId]?.disconnect()
    }

    private fun log(message: String) {
        println("[Server] $message")
    }

    /**
     * Internal class to handle individual client connections
     */
    private inner class ClientHandler(
        val clientId: String,
        private val secureSocket: SecureSocket
    ) {
        private val messageHandler: ((DataModel) -> Unit)? = null

        fun send(message: DataModel) {
            secureSocket.send(message)
        }

        fun handleMessage(message: DataModel) {
            // Default message handling - can be overridden
            message.handle(
                onText = { text ->
                    log("$clientId sent text: '$text'")
                    // Echo back
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
                    // Respond to heartbeat
                    send(heartbeatMessage())
                },
                onResponse = { success, msg, data ->
                    log("$clientId sent response: $msg")
                }
            )
        }

        private fun handleCommand(original: DataModel, command: String, params: Map<String, String>) {
            when (command.lowercase()) {
                "ping" -> {
                    send(original.createResponse(true, "pong"))
                }
                "echo" -> {
                    val text = params["message"] ?: "no message"
                    send(textMessage(text))
                }
                "info" -> {
                    send(original.createResponse(true, "Server info", mapOf(
                        "connectedClients" to getClientCount().toString(),
                        "serverPort" to port.toString()
                    )))
                }
                "disconnect" -> {
                    send(original.createResponse(true, "Disconnecting..."))
                    disconnect()
                }
                else -> {
                    send(original.createResponse(false, "Unknown command: $command"))
                }
            }
        }

        fun disconnect() {
            try {
                secureSocket.close()
            } catch (e: Exception) {
                log("Error disconnecting $clientId: ${e.message}")
                e.printStackTrace() // Log stack trace
            }
        }
    }
}