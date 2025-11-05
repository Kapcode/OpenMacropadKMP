package com.kapcode.open.macropad.kmp.network.sockets.Client

import Model.*
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Client(
    private val serverAddress: String = "localhost",
    private val serverPort: Int = 9999,
    private val autoReconnectEnabled: Boolean = true,
    private val maxReconnectAttempts: Int = 7,
    private val reconnectDelays: List<Long> = listOf(0, 100, 300, 600, 1000, 2000, 3000),
    private val maxQueueSize: Int = 20,
    private val messageExpiryMs: Long = 2000,
    private val onConnected: ((serverName: String) -> Unit)? = null,
    private val onDisconnected: (() -> Unit)? = null,
    private val onReconnecting: ((attempt: Int, maxAttempts: Int) -> Unit)? = null,
    private val onReconnectSuccess: (() -> Unit)? = null,
    private val onReconnectFailed: (() -> Unit)? = null,
    private val onMessageReceived: ((DataModel) -> Unit)? = null,
    private val onError: ((String, Exception) -> Unit)? = null
) {
    private var socket: Socket? = null
    private var secureSocket: SecureSocket? = null
    private val isConnected = AtomicBoolean(false)
    private val isRunning = AtomicBoolean(false)
    var serverName: String? = null
        private set

    suspend fun connect(clientName: String) {
        if (isConnected.get()) {
            log("Already connected")
            return
        }
        try {
            log("Connecting to $serverAddress:$serverPort...")
            socket = withContext(Dispatchers.IO) { Socket(serverAddress, serverPort) }
            log("Socket connected")

            log("Performing handshake...")
            val (newSecureSocket, receivedServerName) = withContext(Dispatchers.IO) {
                SecureSocket.clientHandshake(socket!!, clientName)
            }
            secureSocket = newSecureSocket
            serverName = receivedServerName
            log("Handshake complete. Server name: $serverName")

            isConnected.set(true)
            isRunning.set(true)
            
            // This is where you might want to start your read/write threads/coroutines
            onConnected?.invoke(serverName!!)
            log("Connected successfully")

        } catch (e: Exception) {
            log("Connection failed: ${e.message}")
            onError?.invoke("connect", e)
            cleanup()
            throw e
        }
    }

    suspend fun disconnect() {
        if (!isConnected.get()) {
            log("Not connected")
            return
        }
        log("Disconnecting...")
        isRunning.set(false)
        isConnected.set(false)
        withContext(Dispatchers.IO) {
            cleanup()
        }
        onDisconnected?.invoke()
        log("Disconnected")
    }
    
    // ... (rest of the Client class methods like send, etc. remain the same for now)
    
    private fun cleanup() {
        try {
            secureSocket?.close()
            socket?.close()
        } catch (e: Exception) {
            log("Error during cleanup: ${e.message}")
        }
        secureSocket = null
        socket = null
    }

    private fun log(message: String) {
        println("[Client] $message")
    }
}