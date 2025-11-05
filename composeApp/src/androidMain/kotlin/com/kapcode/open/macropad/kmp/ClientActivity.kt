package com.kapcode.open.macropad.kmp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.* // Added for remember, mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kapcode.open.macropad.kmp.network.sockets.Client.Client // Corrected import
import com.kapcode.open.macropad.kmp.network.sockets.Client.ClientBuilder // Corrected import
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel // Added import for cancel

class ClientActivity : ComponentActivity() {

    private var client: Client? = null
    private var serverAddressFull: String? = null // Renamed to avoid confusion
    private var serverPort: Int = 9999
    private val activityScope = CoroutineScope(Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        serverAddressFull = intent.getStringExtra("SERVER_ADDRESS")
        val addressParts = serverAddressFull?.split(":")
        val ipAddress = addressParts?.getOrNull(0)
        val port = addressParts?.getOrNull(1)?.toIntOrNull() ?: 9999
        serverPort = port

        if (ipAddress == null) {
            Log.e(TAG, "No server address provided. Finishing activity.")
            finish()
            return
        }

        Log.d(TAG, "Attempting to connect to $ipAddress:$serverPort")

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var connectionStatus by remember { mutableStateOf("Connecting...") }

                    LaunchedEffect(Unit) {
                        // Connect when the composable is launched
                        connectToServer(ipAddress, serverPort) { newStatus ->
                            connectionStatus = newStatus
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Connected to:")
                        Text(serverAddressFull ?: "N/A", style = MaterialTheme.typography.headlineMedium)
                        Text("Status: $connectionStatus")
                        // Add more UI elements for controlling the macropad here
                    }
                }
            }
        }
    }

    private suspend fun connectToServer(ipAddress: String, port: Int, onStatusUpdate: (String) -> Unit) { // Marked as suspend
        withContext(Dispatchers.IO) {
            try {
                client = ClientBuilder()
                    .serverAddress(ipAddress)
                    .serverPort(port)
                    .autoReconnect(false)
                    .onConnected { 
                        activityScope.launch { onStatusUpdate("Connected") }
                        Log.d(TAG, "Client connected to $ipAddress:$port")
                    }
                    .onDisconnected { 
                        activityScope.launch { onStatusUpdate("Disconnected") }
                        Log.d(TAG, "Client disconnected from $ipAddress:$port")
                    }
                    .onError { tag: String, exception: Exception ->
                        activityScope.launch { onStatusUpdate("Error: ${exception.message ?: "Unknown error"}") }
                        Log.e(TAG, "Client error [$tag]: ${exception.message}")
                        exception.printStackTrace() // Log stack trace
                    }
                    .build()

                client?.connect()
            } catch (e: Exception) {
                activityScope.launch { onStatusUpdate("Connection Failed: ${e.message ?: "Unknown error"}") }
                Log.e(TAG, "Failed to build or connect client: ${e.message}")
                e.printStackTrace() // Log stack trace
            }
        }
    }

    override fun onStop() {
        super.onStop()
        activityScope.launch { 
            withContext(Dispatchers.IO) { 
                client?.disconnect()
                Log.d(TAG, "Client disconnected in onStop")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.launch { 
            withContext(Dispatchers.IO) { 
                client?.disconnect()
                Log.d(TAG, "Client disconnected in onDestroy")
            }
        }
        activityScope.cancel()
    }
}
