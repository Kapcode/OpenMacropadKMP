package com.kapcode.open.macropad.kmps

import MacroKTOR.MacroKtorClient
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.ui.components.CommonAppBar
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

@OptIn(ExperimentalMaterial3Api::class)
class ClientActivity : ComponentActivity() {

    private var client: MacroKtorClient? = null
    private val activityScope = CoroutineScope(Dispatchers.Main.immediate)
    private var clientJob: Job? = null
    private val macros = mutableStateListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serverAddressFull = intent.getStringExtra("SERVER_ADDRESS")
        val deviceName = intent.getStringExtra("DEVICE_NAME") ?: "Android Device"
        val isSecure = intent.getBooleanExtra("IS_SECURE", false)
        val addressParts = serverAddressFull?.split(":")
        val ipAddress = addressParts?.getOrNull(0)
        val port = addressParts?.getOrNull(1)?.toIntOrNull()

        if (ipAddress == null || port == null) {
            Log.e("ClientActivity", "Invalid server address provided. Finishing activity.")
            finish()
            return
        }

        Log.d("ClientActivity", "Attempting to connect to $ipAddress:$port (Secure: $isSecure)")

        setContent {
            AppTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            LazyColumn {
                                items(macros) { macro ->
                                    Text(text = macro, modifier = Modifier.padding(all = 16.dp))
                                }
                            }
                        }
                    },
                    gesturesEnabled = drawerState.isOpen
                ) {
                    Scaffold(
                        topBar = {
                            CommonAppBar(
                                title = "Open Macropad",
                                onSettingsClick = {
                                    Toast.makeText(context, "Settings Clicked", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        bottomBar = {
                            BottomAppBar {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                client?.send("getMacros")
                                                drawerState.open()
                                            }
                                            val macroListString = macros.joinToString(", ")
                                            Toast.makeText(context, "Macros: $macroListString", Toast.LENGTH_LONG).show()
                                        },
                                        modifier = Modifier.align(Alignment.CenterEnd)
                                    ) {
                                        Text("Macros")
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            var connectionStatus by remember { mutableStateOf("Connecting...") }
                            var serverName by remember { mutableStateOf<String?>(null) }

                            LaunchedEffect(Unit) {
                                connectToServer(ipAddress, port, deviceName, isSecure) { status, name ->
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
                                    Text(
                                        serverName ?: serverAddressFull ?: "N/A",
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    Text("Status: $connectionStatus")
                                }
                            }
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

    private fun connectToServer(
        ipAddress: String,
        port: Int,
        deviceName: String,
        isSecure: Boolean,
        onUpdate: (status: String, serverName: String?) -> Unit
    ) {

        val ktorHttpClient = HttpClient(OkHttp) {
            install(WebSockets)

            engine {
                if (isSecure) {
                    preconfigured = createUnsafeOkHttpClient()
                }
            }
        }

        client = MacroKtorClient(ktorHttpClient, ipAddress, port, isSecure)

        clientJob = activityScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    client?.connect(deviceName)
                }

                onUpdate("Connected", ipAddress)

                client?.send("getMacros")

                client?.incomingMessages?.receiveAsFlow()?.collect { frame ->
                    val receivedText = when (frame) {
                        is Frame.Text -> frame.readText()
                        is Frame.Binary -> frame.readBytes().toString(Charsets.UTF_8)
                        else -> ""
                    }

                    Log.d("ClientActivity", "Received from server: $receivedText")
                    if (receivedText.startsWith("macros:")) {
                        val macroNames = receivedText.substringAfter("macros:").split(",")
                        macros.clear()
                        macros.addAll(macroNames)
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
    
    private fun createUnsafeOkHttpClient(): OkHttpClient {
        try {
            val trustAllCerts = arrayOf<X509TrustManager>(
                @SuppressLint("CustomX509TrustManager")
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            return OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0])
                .hostnameVerifier { _, _ -> true }.build()
        } catch (e: Exception) {
            throw RuntimeException(e)
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
