package com.kapcode.open.macropad.kmps

import MacroKTOR.MacroKtorClient
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapcode.open.macropad.kmps.network.sockets.model.*
import com.kapcode.open.macropad.kmps.settings.AppTheme as SettingsAppTheme
import com.kapcode.open.macropad.kmps.settings.ClientSettingsSection
import com.kapcode.open.macropad.kmps.settings.SettingsScreen
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel
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
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
class ClientActivity : ComponentActivity() {

    private var client: MacroKtorClient? = null
    private val activityScope = CoroutineScope(Dispatchers.Main.immediate)
    private var clientJob: Job? = null
    private val macros = mutableStateListOf<String>()
    private val settingsViewModel = SettingsViewModel()
    private val MAX_RETRIES = 5

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

        Log.d("ClientActivity", "Targeting $ipAddress:$port (Secure: $isSecure)")

        setContent {
            val theme by settingsViewModel.theme.collectAsState()
            AppTheme(useDarkTheme = theme == SettingsAppTheme.DarkBlue) {
                var connectionStatus by remember { mutableStateOf("Connecting...") }
                var serverName by remember { mutableStateOf<String?>(null) }
                var disconnectReason by remember { mutableStateOf<String?>(null) }
                var verificationCode by remember { mutableStateOf<String?>(null) }
                val executingMacros = remember { mutableStateOf(setOf<String>()) }
                val failedMacros = remember { mutableStateOf(setOf<String>()) }

                LaunchedEffect(Unit) {
                    connectToServer(
                        ipAddress,
                        port,
                        deviceName,
                        isSecure,
                        onUpdate = { status, name, reason, code ->
                            connectionStatus = status
                            serverName = name
                            disconnectReason = reason
                            verificationCode = code
                        },
                        onExecutionStart = { macro ->
                            executingMacros.value += macro
                            failedMacros.value -= macro
                        },
                        onExecutionComplete = { macro ->
                            executingMacros.value -= macro
                        },
                        onExecutionFailed = { macro, error ->
                            executingMacros.value -= macro
                            failedMacros.value += macro
                            Toast.makeText(this@ClientActivity, "Macro '$macro' failed: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                ClientScreen(
                    connectionStatus = connectionStatus,
                    serverName = serverName ?: serverAddressFull,
                    disconnectReason = disconnectReason,
                    verificationCode = verificationCode,
                    macros = macros,
                    executingMacros = executingMacros.value,
                    failedMacros = failedMacros.value,
                    settingsViewModel = settingsViewModel,
                    onGetMacros = { activityScope.launch { client?.send("getMacros") } },
                    onMacroClick = ::sendMacro,
                    onBackToMain = { finish() }
                )
            }
        }
    }

    private fun sendMacro(macroName: String) {
        val tokenManager = TokenManager.getInstance(this)
        if (tokenManager.spendTokens(BillingConstants.TOKENS_PER_MACRO_PRESS)) {
            activityScope.launch {
                client?.send(commandMessage("play:$macroName").toBytes())
            }
        } else {
            Toast.makeText(this, "Not enough tokens! Watch an ad to get more.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToServer(
        ipAddress: String,
        port: Int,
        deviceName: String,
        isSecure: Boolean,
        onUpdate: (status: String, serverName: String?, reason: String?, verificationCode: String?) -> Unit,
        onExecutionStart: (String) -> Unit = {},
        onExecutionComplete: (String) -> Unit = {},
        onExecutionFailed: (String, String) -> Unit = { _, _ -> }
    ) {
        clientJob = activityScope.launch {
            var backoffMillis = 1000L
            val maxBackoffMillis = 16000L
            var retryCount = 0

            while (isActive) {
                var tempClient: MacroKtorClient? = null
                try {
                    val ktorHttpClient = HttpClient(OkHttp) {
                        install(WebSockets)
                        engine { if (isSecure) { preconfigured = createUnsafeOkHttpClient() } }
                    }

                    tempClient = MacroKtorClient(ktorHttpClient, ipAddress, port, isSecure)
                    this@ClientActivity.client = tempClient

                    onUpdate("Connecting...", ipAddress, null, null)
                    withContext(Dispatchers.IO) {
                        tempClient.connect(deviceName)
                    }

                    onUpdate("Connected", ipAddress, null, null)
                    backoffMillis = 1000L // Reset backoff on success
                    retryCount = 0
                    var lastHeartbeat = System.currentTimeMillis()
                    tempClient.send(textMessage("getMacros").toBytes()) // Request macros on successful connection

                    val watchdogJob = activityScope.launch {
                        while (isActive) {
                            delay(5000)
                            if (System.currentTimeMillis() - lastHeartbeat > 20000) {
                                Log.w("ClientActivity", "Heartbeat timeout! Reconnecting...")
                                this@launch.cancel()
                                tempClient?.close()
                            }
                        }
                    }

                    tempClient.incomingMessages.receiveAsFlow().collect { frame ->
                        if (frame is Frame.Binary) {
                            try {
                                lastHeartbeat = System.currentTimeMillis()
                                val dataModel = DataModel.fromBytes(frame.readBytes())
                                dataModel.handle(
                                    onControl = { command, params ->
                                        when (command) {
                                            ControlCommand.PAIRING_PENDING -> {
                                                macros.clear()
                                                val code = params["code"]
                                                onUpdate("Pending Approval", null, null, code)
                                            }
                                            ControlCommand.PAIRING_APPROVED -> {
                                                onUpdate("Connected", ipAddress, null, null)
                                                activityScope.launch {
                                                    tempClient.send(textMessage("getMacros").toBytes())
                                                }
                                            }
                                            ControlCommand.PAIRING_REJECTED -> {
                                                macros.clear()
                                                onUpdate("Pairing Denied", null, params["reason"] ?: "Server rejected pairing.", null)
                                                this@launch.cancel()
                                            }
                                            ControlCommand.BANNED -> {
                                                macros.clear()
                                                val reason = params["reason"] ?: "Device is banned"
                                                onUpdate("Banned", null, reason, null)
                                                this@launch.cancel()
                                            }
                                            ControlCommand.DISCONNECT -> {
                                                macros.clear()
                                                onUpdate("Disconnected", null, params["reason"] ?: "Disconnected by Server.", null)
                                                this@launch.cancel()
                                            }
                                            ControlCommand.EXECUTION_START -> {
                                                params["macro"]?.let { onExecutionStart(it) }
                                            }
                                            ControlCommand.EXECUTION_COMPLETE -> {
                                                params["macro"]?.let { onExecutionComplete(it) }
                                            }
                                            ControlCommand.EXECUTION_FAILED -> {
                                                val macro = params["macro"] ?: "Unknown"
                                                val error = params["error"] ?: "Unknown error"
                                                onExecutionFailed(macro, error)
                                            }
                                            else -> {}
                                        }
                                    },
                                    onText = { text ->
                                        if (text.startsWith("macros:")) {
                                            onUpdate("Connected", ipAddress, null, null)
                                            val macroNames = text.substringAfter("macros:").split(",").filter { it.isNotBlank() }
                                            macros.clear()
                                            macros.addAll(macroNames)
                                        }
                                    },
                                    onHeartbeat = {
                                        lastHeartbeat = System.currentTimeMillis()
                                    }
                                )
                            } catch (e: Exception) {
                                Log.e("ClientActivity", "Error parsing DataModel", e)
                            }
                        } else if (frame is Frame.Text) {
                            lastHeartbeat = System.currentTimeMillis()
                            val receivedText = frame.readText()
                            Log.d("ClientActivity", "Received legacy text from server: $receivedText")
                        }
                    }
                    watchdogJob.cancel()

                } catch (e: Exception) {
                    if (e is CancellationException) {
                        // onUpdate("Disconnected", null, null) // Don't overwrite explicit reason
                        throw e 
                    }
                    retryCount++
                    if (retryCount > MAX_RETRIES) {
                        Log.e("ClientActivity", "Max retries reached. Finishing activity.")
                        onUpdate("Failed", null, "Max retries reached. Please check your connection.", null)
                        this@launch.cancel()
                        return@launch
                    }
                    val errorMsg = e.message ?: "Unknown error"
                    onUpdate("Connecting...", null, "Retrying ($retryCount/$MAX_RETRIES)...", null)
                    Log.w("ClientActivity", "Connection failed ($errorMsg), retrying in ${backoffMillis / 1000}s")
                } finally {
                    tempClient?.close()
                    this@ClientActivity.client = null
                }

                delay(backoffMillis)
                backoffMillis = (backoffMillis * 2).coerceAtMost(maxBackoffMillis)
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
        clientJob?.cancel()
        activityScope.cancel()
        Log.d("ClientActivity", "ClientActivity destroyed")
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientScreen(
    connectionStatus: String,
    serverName: String?,
    disconnectReason: String?,
    verificationCode: String?,
    macros: List<String>,
    executingMacros: Set<String> = emptySet(),
    failedMacros: Set<String> = emptySet(),
    settingsViewModel: SettingsViewModel,
    onGetMacros: () -> Unit,
    onMacroClick: (String) -> Unit,
    onBackToMain: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    Scaffold(
        topBar = {
            CommonAppBar(
                title = if (showSettings) "Settings" else "Open Macropad",
                onSettingsClick = { showSettings = !showSettings },
                navigationIcon = {
                    if (showSettings) {
                        IconButton(onClick = { showSettings = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        IconButton(onClick = {
                            scope.launch {
                                onGetMacros()
                                drawerState.open()
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Macros")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!showSettings) {
                BottomAppBar { AdmobBanner() }
            }
        }
    ) { innerPadding ->
        if (showSettings) {
            SettingsScreen(
                viewModel = settingsViewModel,
                modifier = Modifier.padding(innerPadding)
            ) {
                ClientSettingsSection()
            }
        } else {
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
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (macros.isNotEmpty()) {
                        MacroButtonsScreen(
                            macros = macros, 
                            executingMacros = executingMacros,
                            failedMacros = failedMacros,
                            onMacroClick = onMacroClick
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (connectionStatus == "Pending Approval") {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text("Please approve this device on your Desktop.")
                                verificationCode?.let { code ->
                                    Spacer(Modifier.height(24.dp))
                                    Text("Verification Code:", style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        code,
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 8.sp
                                    )
                                }
                            } else if (disconnectReason != null) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(16.dp))
                                Text("Disconnected", style = MaterialTheme.typography.headlineMedium)
                                Spacer(Modifier.height(8.dp))
                                Text(disconnectReason, style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.height(32.dp))
                                Button(onClick = onBackToMain) {
                                    Text("Back to Server List")
                                }
                            } else {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text("Connected to:")
                                Text(
                                    serverName ?: "N/A",
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