package com.kapcode.open.macropad.kmp.network.sockets.Client

import Model.DataModel

class ClientBuilder {
    private var serverAddress: String = "localhost"
    private var serverPort: Int = 9999
    private var autoReconnectEnabled: Boolean = true
    private var maxReconnectAttempts: Int = 7
    private var reconnectDelays: List<Long> = listOf(0, 100, 300, 600, 1000, 2000, 3000)
    private var maxQueueSize: Int = 20
    private var messageExpiryMs: Long = 2000
    private var onConnected: ((serverName: String) -> Unit)? = null
    private var onDisconnected: (() -> Unit)? = null
    private var onReconnecting: ((Int, Int) -> Unit)? = null
    private var onReconnectSuccess: (() -> Unit)? = null
    private var onReconnectFailed: (() -> Unit)? = null
    private var onMessageReceived: ((DataModel) -> Unit)? = null
    private var onError: ((String, Exception) -> Unit)? = null

    fun serverAddress(address: String) = apply { this.serverAddress = address }
    fun serverPort(port: Int) = apply { this.serverPort = port }
    fun autoReconnect(enabled: Boolean) = apply { this.autoReconnectEnabled = enabled }
    fun maxReconnectAttempts(max: Int) = apply { this.maxReconnectAttempts = max }
    fun reconnectDelays(delays: List<Long>) = apply { this.reconnectDelays = delays }
    fun maxQueueSize(size: Int) = apply { this.maxQueueSize = size }
    fun messageExpiry(ms: Long) = apply { this.messageExpiryMs = ms }
    fun onConnected(handler: (serverName: String) -> Unit) = apply { this.onConnected = handler }
    fun onDisconnected(handler: () -> Unit) = apply { this.onDisconnected = handler }
    fun onReconnecting(handler: (attempt: Int, maxAttempts: Int) -> Unit) = apply { this.onReconnecting = handler }
    fun onReconnectSuccess(handler: () -> Unit) = apply { this.onReconnectSuccess = handler }
    fun onReconnectFailed(handler: () -> Unit) = apply { this.onReconnectFailed = handler }
    fun onMessageReceived(handler: (DataModel) -> Unit) = apply { this.onMessageReceived = handler }
    fun onError(handler: (String, Exception) -> Unit) = apply { this.onError = handler }

    fun build(): Client {
        return Client(
            serverAddress = serverAddress,
            serverPort = serverPort,
            autoReconnectEnabled = autoReconnectEnabled,
            maxReconnectAttempts = maxReconnectAttempts,
            reconnectDelays = reconnectDelays,
            maxQueueSize = maxQueueSize,
            messageExpiryMs = messageExpiryMs,
            onConnected = onConnected,
            onDisconnected = onDisconnected,
            onReconnecting = onReconnecting,
            onReconnectSuccess = onReconnectSuccess,
            onReconnectFailed = onReconnectFailed,
            onMessageReceived = onMessageReceived,
            onError = onError
        )
    }
}