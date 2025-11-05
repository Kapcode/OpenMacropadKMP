package Client

import Model.*
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Secure client with aggressive auto-reconnect for real-time applications
 * Optimized for WiFi Mouse and Macropad use cases
 */
class Client(
    private val serverAddress: String = "localhost",
    private val serverPort: Int = 9999,
    
    // Auto-reconnect configuration
    private val autoReconnectEnabled: Boolean = true,
    private val maxReconnectAttempts: Int = 7,
    private val reconnectDelays: List<Long> = listOf(0, 100, 300, 600, 1000, 2000, 3000), // milliseconds
    
    // Message queue configuration
    private val maxQueueSize: Int = 20,
    private val messageExpiryMs: Long = 2000, // 2 seconds
    
    // Callbacks
    private val onConnected: (() -> Unit)? = null,
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
    private val isReconnecting = AtomicBoolean(false)
    private val reconnectAttempt = AtomicInteger(0)
    
    // Message queue with timestamp for expiry
    private data class TimestampedMessage(val message: DataModel, val timestamp: Long = System.currentTimeMillis())
    private val sendQueue = LinkedBlockingQueue<TimestampedMessage>(maxQueueSize)
    
    // Background threads
    private var receiveThread: Thread? = null
    private var sendThread: Thread? = null
    private var reconnectThread: Thread? = null
    
    /**
     * Connect to the server
     */
    fun connect() {
        if (isConnected.get()) {
            log("Already connected")
            return
        }
        
        try {
            log("Connecting to $serverAddress:$serverPort...")
            
            // Create socket connection
            socket = Socket(serverAddress, serverPort)
            log("Socket connected")
            
            // Perform handshake
            log("Performing handshake...")
            secureSocket = SecureSocket.clientHandshake(socket!!)
            log("Handshake complete")
            
            isConnected.set(true)
            isRunning.set(true)
            reconnectAttempt.set(0) // Reset reconnect counter
            
            // Clean expired messages before reconnect
            cleanExpiredMessages()
            
            // Start background threads
            startReceiveThread()
            startSendThread()
            
            // Notify connection
            onConnected?.invoke()
            log("Connected successfully")
            
        } catch (e: Exception) {
            log("Connection failed: ${e.message}")
            onError?.invoke("connect", e)
            cleanup()
            throw e
        }
    }
    
    /**
     * Disconnect from the server (manual disconnect, no auto-reconnect)
     */
    fun disconnect() {
        if (!isConnected.get()) {
            log("Not connected")
            return
        }
        
        log("Disconnecting...")
        isRunning.set(false)
        isConnected.set(false)
        
        // Stop reconnect attempts if any
        isReconnecting.set(false)
        reconnectThread?.interrupt()
        
        // Clear send queue
        sendQueue.clear()
        
        // Wait for threads to finish
        receiveThread?.interrupt()
        sendThread?.interrupt()
        
        try {
            receiveThread?.join(1000)
            sendThread?.join(1000)
        } catch (e: InterruptedException) {
            // Ignore
        }
        
        // Close connections
        cleanup()
        
        // Notify disconnection
        onDisconnected?.invoke()
        log("Disconnected")
    }
    
    /**
     * Send a message to the server (queued, non-blocking)
     * For real-time apps: drops oldest if queue is full
     */
    fun send(message: DataModel, priority: MessagePriority = MessagePriority.NORMAL) {
        val timestamped = TimestampedMessage(message)
        
        when (priority) {
            MessagePriority.LATEST_ONLY -> {
                // For mouse movement: keep only latest
                sendQueue.clear()
                sendQueue.offer(timestamped)
            }
            MessagePriority.HIGH -> {
                // For important actions: force add (drop oldest if full)
                if (!sendQueue.offer(timestamped)) {
                    sendQueue.poll() // Remove oldest
                    sendQueue.offer(timestamped) // Add new
                }
            }
            MessagePriority.NORMAL -> {
                // Normal priority: add if space available
                if (!sendQueue.offer(timestamped)) {
                    log("Send queue full, dropping message")
                }
            }
        }
    }
    
    /**
     * Send mouse movement (keeps only latest position)
     */
    fun sendMouseMove(x: Int, y: Int) {
        val message = commandMessage("mouse_move", mapOf("x" to x.toString(), "y" to y.toString()))
        send(message, MessagePriority.LATEST_ONLY)
    }
    
    /**
     * Send mouse click (high priority)
     */
    fun sendMouseClick(button: String) {
        val message = commandMessage("mouse_click", mapOf("button" to button))
        send(message, MessagePriority.HIGH)
    }
    
    /**
     * Send key press (high priority)
     */
    fun sendKeyPress(key: String) {
        val message = commandMessage("key_press", mapOf("key" to key))
        send(message, MessagePriority.HIGH)
    }
    
    /**
     * Send text message
     */
    fun sendText(text: String) {
        send(textMessage(text))
    }
    
    /**
     * Send command
     */
    fun sendCommand(command: String, parameters: Map<String, String> = emptyMap()) {
        send(commandMessage(command, parameters))
    }
    
    /**
     * Check if client is connected
     */
    fun isConnected(): Boolean = isConnected.get()
    
    /**
     * Check if client is reconnecting
     */
    fun isReconnecting(): Boolean = isReconnecting.get()
    
    /**
     * Get current reconnect attempt number (0 if not reconnecting)
     */
    fun getReconnectAttempt(): Int = reconnectAttempt.get()
    
    /**
     * Manually trigger reconnect
     */
    fun reconnect() {
        if (isConnected.get()) {
            disconnect()
        }
        attemptReconnect()
    }
    
    /**
     * Start the receive thread
     */
    private fun startReceiveThread() {
        receiveThread = Thread {
            log("Receive thread started")
            
            try {
                while (isRunning.get() && isConnected.get()) {
                    val message = secureSocket?.receive()
                    
                    if (message == null) {
                        log("Server closed connection")
                        handleDisconnect()
                        break
                    }
                    
                    log("Received: ${message.messageType::class.simpleName}")
                    
                    // Notify message received
                    onMessageReceived?.invoke(message)
                    
                    // Handle message
                    handleMessage(message)
                }
            } catch (e: SocketException) {
                if (isRunning.get()) {
                    log("Connection lost: ${e.message}")
                    onError?.invoke("receive", e)
                    handleDisconnect()
                }
            } catch (e: Exception) {
                log("Receive error: ${e.message}")
                onError?.invoke("receive", e)
                handleDisconnect()
            }
            
            log("Receive thread stopped")
        }.apply {
            name = "Client-Receive-Thread"
            isDaemon = true
            start()
        }
    }
    
    /**
     * Start the send thread
     */
    private fun startSendThread() {
        sendThread = Thread {
            log("Send thread started")
            
            try {
                while (isRunning.get() && isConnected.get()) {
                    // Clean expired messages periodically
                    cleanExpiredMessages()
                    
                    // Wait for message in queue (with timeout)
                    val timestampedMsg = sendQueue.poll(100, TimeUnit.MILLISECONDS)
                    
                    if (timestampedMsg != null) {
                        // Check if message expired
                        val age = System.currentTimeMillis() - timestampedMsg.timestamp
                        if (age > messageExpiryMs) {
                            log("Message expired (${age}ms old), dropping")
                            continue
                        }
                        
                        secureSocket?.send(timestampedMsg.message)
                        log("Sent: ${timestampedMsg.message.messageType::class.simpleName}")
                    }
                }
            } catch (e: InterruptedException) {
                // Thread interrupted, exit gracefully
            } catch (e: Exception) {
                log("Send error: ${e.message}")
                onError?.invoke("send", e)
                handleDisconnect()
            }
            
            log("Send thread stopped")
        }.apply {
            name = "Client-Send-Thread"
            isDaemon = true
            start()
        }
    }
    
    /**
     * Handle disconnect and trigger auto-reconnect if enabled
     */
    private fun handleDisconnect() {
        if (!isConnected.get()) {
            return // Already disconnected
        }
        
        isConnected.set(false)
        cleanup()
        
        // Notify disconnection
        onDisconnected?.invoke()
        
        // Attempt auto-reconnect if enabled
        if (autoReconnectEnabled && isRunning.get()) {
            attemptReconnect()
        }
    }
    
    /**
     * Attempt to reconnect with aggressive retry strategy
     */
    private fun attemptReconnect() {
        if (isReconnecting.get()) {
            log("Reconnect already in progress")
            return
        }
        
        isReconnecting.set(true)
        reconnectAttempt.set(0)
        
        reconnectThread = Thread {
            log("Starting reconnect attempts...")
            
            for (attempt in 1..maxReconnectAttempts) {
                if (!isRunning.get() || !isReconnecting.get()) {
                    log("Reconnect cancelled")
                    break
                }
                
                reconnectAttempt.set(attempt)
                log("Reconnect attempt $attempt/$maxReconnectAttempts")
                
                // Notify UI
                onReconnecting?.invoke(attempt, maxReconnectAttempts)
                
                try {
                    // Get delay for this attempt (or use last delay if out of bounds)
                    val delay = reconnectDelays.getOrElse(attempt - 1) { reconnectDelays.last() }
                    if (delay > 0 && attempt > 1) {
                        Thread.sleep(delay)
                    }
                    
                    // Attempt connection
                    connect()
                    
                    // Success!
                    log("Reconnect successful!")
                    isReconnecting.set(false)
                    onReconnectSuccess?.invoke()
                    return@Thread
                    
                } catch (e: Exception) {
                    log("Reconnect attempt $attempt failed: ${e.message}")
                }
            }
            
            // All attempts failed
            log("Reconnect failed after $maxReconnectAttempts attempts")
            isReconnecting.set(false)
            isRunning.set(false)
            onReconnectFailed?.invoke()
            
        }.apply {
            name = "Client-Reconnect-Thread"
            isDaemon = true
            start()
        }
    }
    
    /**
     * Clean expired messages from queue
     */
    private fun cleanExpiredMessages() {
        val now = System.currentTimeMillis()
        sendQueue.removeIf { now - it.timestamp > messageExpiryMs }
    }
    
    /**
     * Handle received messages
     */
    private fun handleMessage(message: DataModel) {
        message.handle(
            onText = { text ->
                log("Server says: '$text'")
            },
            onCommand = { command, params ->
                log("Server command: $command")
            },
            onData = { key, value ->
                log("Server data: $key (${value.size} bytes)")
            },
            onResponse = { success, msg, data ->
                log("Server response: $msg (success=$success)")
            },
            onHeartbeat = { timestamp ->
                // Silently handle heartbeat
            }
        )
    }
    
    /**
     * Clean up resources
     */
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
    
    /**
     * Message priority for queue management
     */
    enum class MessagePriority {
        LATEST_ONLY,  // Keep only latest (e.g., mouse position)
        HIGH,         // Important, force add (e.g., clicks, key presses)
        NORMAL        // Normal priority (e.g., text messages)
    }
}
