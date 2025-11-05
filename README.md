# OpenMacropadKMP

This is a Kotlin Multiplatform project, focusing on creating an open-source Macropad solution.

## Project Structure

-   `composeApp`: Contains the shared code for Android and Desktop platforms using Compose Multiplatform, as well as platform-specific implementations.

## Network Library

This project uses Ktor for WebSocket communication, leveraging Kotlinx Serialization for `DataModel` objects and a custom `EncryptionManager` for secure, end-to-end encrypted messaging.

### Usage Example

Below are basic examples demonstrating how to set up and use the Ktor client and server with Diffie-Hellman key exchange and AES/GCM encryption.

#### Ktor Server Setup

```kotlin
import com.kapcode.open.macropad.kmp.network.ktor.KtorServer
import com.kapcode.open.macropad.kmp.network.sockets.Model.DataModel
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val server = KtorServer(
        port = 9999,
        onClientConnected = { clientId ->
            println("Server: Client $clientId connected!")
        },
        onClientDisconnected = { clientId ->
            println("Server: Client $clientId disconnected.")
        },
        onMessageReceived = { clientId, message ->
            println("Server: Received from $clientId: ${message.messageType::class.simpleName}")
            // Example: Broadcast text messages to all other clients
            if (message.messageType is DataModel.MessageType.Text) {
                server.broadcast(message, excludeClientId = clientId)
            }
        },
        onError = { clientId, error ->
            println("Server: Error for $clientId: ${error.message}")
            error.printStackTrace()
        }
    )

    server.start()
    println("Server is running. Press Enter to stop.")
    readln() // Keep the server running until Enter is pressed
    server.stop()
}
```

#### Ktor Client Setup

```kotlin
import com.kapcode.open.macropad.kmp.network.ktor.KtorClient
import com.kapcode.open.macropad.kmp.network.sockets.Model.DataModel
import com.kapcode.open.macropad.kmp.network.sockets.Model.MessageType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = KtorClient(
        host = "localhost", // Or your server's IP address
        port = 9999,
        onConnected = {
            println("Client: Connected to server and encryption established!")
        },
        onDisconnected = {
            println("Client: Disconnected from server.")
        },
        onMessageReceived = { message ->
            when (val msg = message.messageType) {
                is MessageType.Text -> println("Client: Received text: ${msg.content}")
                is MessageType.Response -> println("Client: Received response: ${msg.message} (Success: ${msg.success})")
                is MessageType.Heartbeat -> println("Client: Received heartbeat.")
                else -> println("Client: Received message of type ${msg::class.simpleName}")
            }
        },
        onError = { error ->
            println("Client: Error: ${error.message}")
            error.printStackTrace()
        }
    )

    client.connect()
    delay(5000) // Give time for connection and key exchange

    if (client.isConnected()) {
        client.sendText("Hello from Ktor client!")
        delay(1000)
        client.sendCommand("ping")
        delay(1000)
        client.sendMouseMove(100, 200)
        delay(1000)
    }

    println("Client: Press Enter to disconnect.")
    readln()
    client.disconnect()
}
```
