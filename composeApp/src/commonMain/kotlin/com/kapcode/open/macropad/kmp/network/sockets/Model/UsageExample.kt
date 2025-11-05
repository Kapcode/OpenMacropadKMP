package Model

import java.net.ServerSocket
import java.net.Socket

/**
 * Example usage of the encrypted DataModel system
 * These are example functions showing how to use the library
 */

object UsageExample {

    /**
     * Example: Server with automatic key exchange
     */
    fun serverExample() {
        val serverSocket = ServerSocket(9999)
        println("Server listening on port 9999...")

        while (true) {
            val clientSocket = serverSocket.accept()
            println("Client connected: ${clientSocket.inetAddress}")

            // Handle client in separate thread
            Thread {
                handleClient(clientSocket)
            }.start()
        }
    }

    private fun handleClient(socket: Socket) {
        // Perform handshake and create secure socket
        val secureSocket = SecureSocket.serverHandshake(socket)
        println("Secure connection established!")

        try {
            while (secureSocket.isConnected()) {
                val message = secureSocket.receive() ?: break

                // Handle different message types
                message.handle(
                    onText = { text ->
                        println("Received text: $text")
                        secureSocket.send(message.createResponse(true, "Text received"))
                    },
                    onCommand = { command, params ->
                        println("Received command: $command with params: $params")
                        when (command) {
                            "ping" -> secureSocket.sendText("pong")
                            "echo" -> secureSocket.sendText(params["message"] ?: "")
                            else -> secureSocket.send(
                                message.createResponse(false, "Unknown command")
                            )
                        }
                    },
                    onHeartbeat = { timestamp ->
                        println("Heartbeat received at $timestamp")
                    }
                )
            }
        } finally {
            secureSocket.close()
            println("Client disconnected")
        }
    }

    /**
     * Example: Client with automatic key exchange
     */
    fun clientExample() {
        val socket = Socket("localhost", 9999)
        val secureSocket = SecureSocket.clientHandshake(socket)
        println("Connected to server!")

        try {
            // Send a text message
            secureSocket.sendText("Hello, server!")

            // Send a command
            secureSocket.sendCommand("ping")

            // Send a command with parameters
            secureSocket.sendCommand("echo", mapOf("message" to "Hello World!"))

            // Send with builder pattern
            val dataModel = DataModelBuilder()
                .text("Custom message")
                .addMetadata("sender", "client1")
                .addMetadata("session", "abc123")
                .priority(DataModel.Priority.HIGH)
                .build()
            secureSocket.send(dataModel)

            // Receive responses
            repeat(4) {
                val response = secureSocket.receive()
                if (response != null) {
                    println("Received: ${response.messageType}")
                }
            }
        } finally {
            secureSocket.close()
        }
    }

    /**
     * Example: Using pre-shared key (no handshake needed)
     */
    fun preSharedKeyExample() {
        // Generate a key once and share it securely
        val key = DataModel.generateKey()
        val keyString = DataModel.keyToString(key)
        println("Share this key securely: $keyString")

        // Server side
        Thread {
            val serverSocket = ServerSocket(9998)
            val clientSocket = serverSocket.accept()
            val secureSocket = SecureSocket.withPreSharedKey(clientSocket, keyString)

            val message = secureSocket.receive()
            println("Server received: ${message?.messageType}")
            secureSocket.close()
        }.start()

        // Wait for server to start
        Thread.sleep(100)

        // Client side
        val socket = Socket("localhost", 9998)
        val secureSocket = SecureSocket.withPreSharedKey(socket, keyString)
        secureSocket.sendText("Hello with pre-shared key!")
        secureSocket.close()
    }

    /**
     * Example: Manual encryption (low-level usage)
     */
    fun manualEncryptionExample() {
        // Generate or load a key
        val key = DataModel.generateKey()

        // Create a message
        val message = textMessage("Secret message")

        // Manually encrypt
        val encryptedBytes = message.toEncryptedBytes(key)
        println("Encrypted size: ${encryptedBytes.size} bytes")

        // Manually decrypt
        val decryptedMessage = DataModel.fromEncryptedBytes(encryptedBytes, key)
        println("Decrypted: ${decryptedMessage.messageType}")
    }
}
