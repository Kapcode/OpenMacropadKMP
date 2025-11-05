
package Server

import Model.DataModel
import Model.SecureSocket

/**
 * Builder for creating Server instances with custom configuration
 */
class ServerBuilder {
    private var port: Int = 9999
    private var maxClients: Int = 50
    private var onClientConnected: ((String, SecureSocket) -> Unit)? = null
    private var onClientDisconnected: ((String) -> Unit)? = null
    private var onMessageReceived: ((String, DataModel) -> Unit)? = null
    private var onError: ((String, Exception) -> Unit)? = null

    fun port(port: Int) = apply { this.port = port }

    fun maxClients(max: Int) = apply { this.maxClients = max }

    fun onClientConnected(handler: (String, SecureSocket) -> Unit) = apply {
        this.onClientConnected = handler
    }

    fun onClientDisconnected(handler: (String) -> Unit) = apply {
        this.onClientDisconnected = handler
    }

    fun onMessageReceived(handler: (String, DataModel) -> Unit) = apply {
        this.onMessageReceived = handler
    }

    fun onError(handler: (String, Exception) -> Unit) = apply {
        this.onError = handler
    }

    fun build(): Server {
        return Server(
            port = port,
            maxClients = maxClients,
            onClientConnected = onClientConnected,
            onClientDisconnected = onClientDisconnected,
            onMessageReceived = onMessageReceived,
            onError = onError
        )
    }
}