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
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.receiveAsFlow
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class ClientActivity : ComponentActivity() {

    private var client: MacroKtorClient? = null
    private val activityScope = CoroutineScope(Dispatchers.Main.immediate)
    private var clientJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serverAddressFull = intent.getStringExtra("SERVER_ADDRESS")
        val addressParts = serverAddressFull?.split(":")
        val ipAddress = addressParts?.getOrNull(0)
        val port = addressParts?.getOrNull(1)?.toIntOrNull() ?: 8443 // Default to Ktor secure port

        if (ipAddress == null) {
            Log.e("ClientActivity", "No server address provided. Finishing activity.")
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

    private fun connectToServer(ipAddress: String, port: Int, onUpdate: (status: String, serverName: String?) -> Unit) {
        try {
            // 1. Get the client's identity
            val identityManager = IdentityManager(applicationContext)
            val clientKeyStore = identityManager.getClientKeyStore()

            // 2. Create a KeyManager to present our certificate to the server
            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            kmf.init(clientKeyStore, "changeit".toCharArray())

            // 3. Create a TrustManager to decide if we trust the server's certificate
            //    !! IMPORTANT !! For now, we are creating a TrustManager that trusts ALL certificates.
            //    This is necessary for the first connection before pairing.
            //    In the next step, we will replace this with a real TrustManager that checks
            //    against a list of known, trusted server certificates.
            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            tmf.init(null as KeyStore?) // Trust all CAs
            val trustManager = tmf.trustManagers[0] as X509TrustManager

            // 4. Create an SSLContext for mutual TLS
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(kmf.keyManagers, tmf.trustManagers, null)

            // 5. Create a pre-configured OkHttpClient
            val okHttpClient = okhttp3.OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustManager)
                .build()

            // 6. Create the Ktor HttpClient
            val ktorHttpClient = HttpClient(OkHttp) {
                engine {
                    preconfigured = okHttpClient
                }
                install(WebSockets)
            }

            // 7. Initialize our client wrapper
            client = MacroKtorClient(ktorHttpClient, ipAddress, port)
            client?.connect()

            clientJob = activityScope.launch {
                onUpdate("Connected", ipAddress)
                client?.incomingMessages?.receiveAsFlow()?.collect { frame ->
                    if (frame is Frame.Text) {
                        val receivedText = frame.readText()
                        Log.d("ClientActivity", "Received from server: $receivedText")
                    }
                }
            }

        } catch (e: Exception) {
            onUpdate("Connection Failed: ${e.message ?: "Unknown error"}", null)
            Log.e("ClientActivity", "Failed to connect: ${e.message}", e)
        }
    }

    override fun onStop() {
        super.onStop()
        client?.close()
        clientJob?.cancel()
        Log.d("ClientActivity", "Client disconnected in onStop")
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        client?.close()
        clientJob?.cancel()
        activityScope.cancel()
        Log.d("ClientActivity", "ClientActivity destroyed")
    }
}
