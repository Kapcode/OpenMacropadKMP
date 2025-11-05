package com.kapcode.open.macropad.kmp

import android.os.Build
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kapcode.open.macropad.kmp.network.sockets.Client.Client
import com.kapcode.open.macropad.kmp.network.sockets.Client.ClientBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClientActivity : ComponentActivity() {

    private var client: Client? = null
    private val activityScope = CoroutineScope(Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serverAddressFull = intent.getStringExtra("SERVER_ADDRESS")
        val addressParts = serverAddressFull?.split(":")
        val ipAddress = addressParts?.getOrNull(0)
        val port = addressParts?.getOrNull(1)?.toIntOrNull() ?: 9999

        if (ipAddress == null) {
            Log.e(TAG, "No server address provided. Finishing activity.")
            finish()
            return
        }

        Log.d(TAG, "Attempting to connect to $ipAddress:$port")

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var connectionStatus by remember { mutableStateOf("Connecting...") }
                    var serverName by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        connectToServer(ipAddress, port) { status, name ->
                            connectionStatus = status
                            serverName = name
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Connected to:")
                        Text(serverName ?: serverAddressFull ?: "N/A", style = MaterialTheme.typography.headlineMedium)
                        Text("Status: $connectionStatus")
                    }
                }
            }
        }
    }

    private suspend fun connectToServer(ipAddress: String, port: Int, onUpdate: (status: String, serverName: String?) -> Unit) {
        val clientName = Build.MODEL // Get Android device name
        withContext(Dispatchers.IO) {
            try {
                client = ClientBuilder()
                    .serverAddress(ipAddress)
                    .serverPort(port)
                    .autoReconnect(false)
                    .onConnected { receivedServerName ->
                        activityScope.launch { onUpdate("Connected", receivedServerName) }
                        Log.d(TAG, "Client connected to $receivedServerName ($ipAddress:$port)")
                    }
                    .onDisconnected {
                        Log.d(TAG, "Client disconnected")
                        activityScope.launch {
                            onUpdate("Disconnected", null)
                            finish()
                        }
                    }
                    .onError { tag: String, exception: Exception ->
                        activityScope.launch { onUpdate("Error: ${exception.message ?: "Unknown error"}", null) }
                        Log.e(TAG, "Client error [$tag]: ${exception.message}")
                        exception.printStackTrace()
                    }
                    .build()

                client?.connect(clientName)
            } catch (e: Exception) {
                activityScope.launch { onUpdate("Connection Failed: ${e.message ?: "Unknown error"}", null) }
                Log.e(TAG, "Failed to build or connect client: ${e.message}")
                e.printStackTrace()
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
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.launch {
            withContext(Dispatchers.IO) {
                client?.disconnect()
            }
        }
        activityScope.cancel()
        Log.d(TAG, "ClientActivity destroyed")
    }
}