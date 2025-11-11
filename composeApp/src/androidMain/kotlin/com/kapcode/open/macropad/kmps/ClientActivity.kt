package com.kapcode.open.macropad.kmps

import MacroKTOR.MacroKtorClient
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
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.receiveAsFlow

class ClientActivity : ComponentActivity() {

    private var client: MacroKtorClient? = null
    private val activityScope = CoroutineScope(Dispatchers.Main.immediate)
    private var clientJob: Job? = null
    private val macros = mutableStateListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serverAddressFull = intent.getStringExtra("SERVER_ADDRESS")
        val deviceName = intent.getStringExtra("DEVICE_NAME") ?: "Android Device"
        val addressParts = serverAddressFull?.split(":")
        val ipAddress = addressParts?.getOrNull(0)
        val port = addressParts?.getOrNull(1)?.toIntOrNull()

        if (ipAddress == null || port == null) {
            Log.e("ClientActivity", "Invalid server address provided. Finishing activity.")
            finish()
            return
        }

        Log.d("ClientActivity", "Attempting to connect to $ipAddress:$port")

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var connectionStatus by remember { mutableStateOf("Connecting...") }
                    var serverName by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        connectToServer(ipAddress, port, deviceName) { status, name ->
                            connectionStatus = status
                            serverName = name
                        }
                    }

                    if (macros.isNotEmpty()) {
                        MacroButtonsScreen(macros = macros, onMacroClick = ::sendMacro)
                    } else {
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
    }

    private fun sendMacro(macroName: String) {
        activityScope.launch {
            client?.send("play:$macroName")
        }
    }

    private fun connectToServer(ipAddress: String, port: Int, deviceName: String, onUpdate: (status: String, serverName: String?) -> Unit) {

        val ktorHttpClient = HttpClient(OkHttp) {
            install(WebSockets)
        }

        client = MacroKtorClient(ktorHttpClient, ipAddress, port)

        clientJob = activityScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    client?.connect(deviceName)
                }

                onUpdate("Connected", ipAddress)

                // Request the list of macros from the server
                client?.send("getMacros")

                client?.incomingMessages?.receiveAsFlow()?.collect { frame ->
                    if (frame is Frame.Text) {
                        val receivedText = frame.readText()
                        Log.d("ClientActivity", "Received from server: $receivedText")
                        if (receivedText.startsWith("macros:")) {
                            val macroNames = receivedText.substringAfter("macros:").split(",")
                            macros.clear()
                            macros.addAll(macroNames)
                        }
                    }
                }
            } catch (e: Exception) {
                onUpdate("Connection Failed: ${e.message ?: "Unknown error"}", null)
                Log.e("ClientActivity", "Failed to connect", e)
            } finally {
                onUpdate("Disconnected", null)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client?.close()
        clientJob?.cancel()
        activityScope.cancel()
        Log.d("ClientActivity", "ClientActivity destroyed")
    }
}