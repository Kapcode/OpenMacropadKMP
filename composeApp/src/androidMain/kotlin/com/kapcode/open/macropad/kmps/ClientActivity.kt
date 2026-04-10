package com.kapcode.open.macropad.kmps

import MacroKTOR.MacroKtorClient
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import android.view.MotionEvent
import androidx.camera.core.*
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

@ExperimentalGetImage
@OptIn(ExperimentalMaterial3Api::class)
class ClientActivity : ComponentActivity() {

    private var client: MacroKtorClient? = null
    private val activityScope = CoroutineScope(Dispatchers.Main.immediate)
    private var clientJob: Job? = null
    private val macros = mutableStateListOf<String>()
    private val settingsViewModel = SettingsViewModel()
    private val MAX_RETRIES = 5

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, will be handled by UI state change
        } else {
            Toast.makeText(this, "Camera permission is required for QR scanning", Toast.LENGTH_SHORT).show()
        }
    }

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
                var isWaitingForDesktopApproval by remember { mutableStateOf(false) }
                var showQrScanner by remember { mutableStateOf(false) }
                val executingMacros = remember { mutableStateOf(setOf<String>()) }
                val failedMacros = remember { mutableStateOf(setOf<String>()) }

                LaunchedEffect(Unit) {
                    val tokenManager = TokenManager.getInstance(this@ClientActivity)
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
                            // Deduct tokens on start
                            tokenManager.spendTokens(BillingConstants.TOKENS_PER_MACRO_PRESS)
                        },
                        onExecutionComplete = { macro ->
                            executingMacros.value -= macro
                        },
                        onExecutionFailed = { macro, error ->
                            executingMacros.value -= macro
                            failedMacros.value += macro
                            // Refund tokens on failure
                            tokenManager.awardTokens(BillingConstants.TOKENS_PER_MACRO_PRESS)
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
                    showQrScanner = showQrScanner,
                    onQrScannerToggle = { show ->
                        if (show) {
                            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                        showQrScanner = show
                    },
                    onGetMacros = { 
                        Log.d("ClientActivity", "Requesting macros...")
                        activityScope.launch { 
                            client?.send(getMacrosRequest().toBytes()) 
                        } 
                    },
                    onMacroClick = ::sendMacro,
                    onPairingCodeEntered = { code ->
                        Log.d("ClientActivity", "Submitting pairing code: $code")
                        activityScope.launch {
                            val msg = controlMessage(ControlCommand.PAIRING_RESPONSE, mapOf("code" to code))
                            client?.send(msg.toBytes())
                        }
                    },
                    onBackToMain = { finish() }
                )
            }
        }
    }

    private fun sendMacro(macroName: String) {
        val tokenManager = TokenManager.getInstance(this)
        if (tokenManager.tokenBalance.value >= BillingConstants.TOKENS_PER_MACRO_PRESS) {
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
                    var currentVerificationCode: String? = null

                    clientJob = activityScope.launch {
                        while (isActive) {
                            val msg = getMacrosRequest().toBytes()
                            client?.send(msg)
                            delay(5000)
                            if (macros.isNotEmpty()) break
                        }
                    }

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
                                                currentVerificationCode = code
                                                onUpdate("Pending Approval", null, null, code)
                                            }
                                            ControlCommand.PAIRING_CODE_MATCHED -> {
                                                onUpdate("Code Matched", null, null, currentVerificationCode)
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

@ExperimentalGetImage
@Composable
fun QrCodeScanner(
    onCodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    
    val previewView = remember { 
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val currentOnCodeScanned by rememberUpdatedState(onCodeScanned)

    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (e: Exception) {
                Log.e("QrCodeScanner", "Error unbinding on dispose", e)
            }
        }
    }

    LaunchedEffect(cameraSelector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            try {
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    barcode.rawValue?.let { code ->
                                        currentOnCodeScanned(code)
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Log.e("QrCodeScanner", "Barcode scanning failed", it)
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                Log.d("QrCodeScanner", "Camera bound successfully")
            } catch (e: Exception) {
                Log.e("QrCodeScanner", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTransformGestures { _, _, zoom, _ ->
                camera?.let { cam ->
                    val zoomState = cam.cameraInfo.zoomState.value
                    val currentZoomRatio = zoomState?.zoomRatio ?: 1f
                    val minZoomRatio = zoomState?.minZoomRatio ?: 1f
                    val maxZoomRatio = zoomState?.maxZoomRatio ?: 1f
                    
                    val newZoomRatio = (currentZoomRatio * zoom).coerceIn(minZoomRatio, maxZoomRatio)
                    cam.cameraControl.setZoomRatio(newZoomRatio)
                }
            }
        }
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        val factory = view.meteringPointFactory
                        val point = factory.createPoint(event.x, event.y)
                        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                            .addPoint(point, FocusMeteringAction.FLAG_AE)
                            .build()
                        camera?.cameraControl?.startFocusAndMetering(action)
                    }
                    true
                }
            }
        )
        
        // Overlay
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopEnd).padding(16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                }
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Switch Camera",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onClose
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalGetImage
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
    onPairingCodeEntered: (String) -> Unit,
    onBackToMain: () -> Unit,
    showQrScanner: Boolean = false,
    onQrScannerToggle: (Boolean) -> Unit = {}
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
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (showQrScanner) {
                                Box(modifier = Modifier.fillMaxSize().aspectRatio(1f)) {
                                    QrCodeScanner(
                                        onCodeScanned = { code: String ->
                                            onPairingCodeEntered(code)
                                            onQrScannerToggle(false)
                                        },
                                        onClose = { onQrScannerToggle(false) }
                                    )
                                }
                            } else if (connectionStatus == "Pending Approval" || connectionStatus == "Code Matched") {
                                var enteredCode by remember { mutableStateOf("") }
                                val focusRequester = remember { FocusRequester() }
                                val keyboardController = LocalSoftwareKeyboardController.current
                                val focusManager = LocalFocusManager.current
                                val configuration = LocalConfiguration.current
                                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                                val isKeyboardOpen = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0

                                LaunchedEffect(Unit) {
                                    if (connectionStatus == "Pending Approval") {
                                        focusRequester.requestFocus()
                                    }
                                }

                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    if (connectionStatus == "Code Matched") {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF008000),
                                                modifier = Modifier.size(64.dp)
                                            )
                                            Spacer(Modifier.height(16.dp))
                                            Text(
                                                "Code Matched!",
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = Color(0xFF008000)
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                "Please click 'Allow' on your Desktop to finish pairing.",
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    } else {
                                        if (!isKeyboardOpen) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    "Please enter the 6-digit code shown on your Desktop:",
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                                Spacer(Modifier.height(16.dp))
                                                CircularProgressIndicator()
                                                Spacer(Modifier.height(24.dp))
                                                OutlinedButton(
                                                    onClick = { onQrScannerToggle(true) }
                                                ) {
                                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Scan QR Code")
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                        ) {
                                            IconButton(onClick = onBackToMain) {
                                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                                            }

                                            OutlinedTextField(
                                                value = enteredCode,
                                                onValueChange = {
                                                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                                        enteredCode = it
                                                    }
                                                },
                                                label = { if (!isKeyboardOpen) Text("6-Digit Code") },
                                                placeholder = { if (isKeyboardOpen) Text("Code") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier
                                                    .width(if (isLandscape && isKeyboardOpen) 120.dp else 180.dp)
                                                    .focusRequester(focusRequester)
                                            )

                                            IconButton(
                                                onClick = {
                                                    if (isKeyboardOpen) {
                                                        keyboardController?.hide()
                                                        focusManager.clearFocus()
                                                    } else {
                                                        focusRequester.requestFocus()
                                                        keyboardController?.show()
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    if (isKeyboardOpen) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                                    contentDescription = "Toggle Keyboard"
                                                )
                                            }

                                            if (!isKeyboardOpen) {
                                                IconButton(onClick = { onQrScannerToggle(true) }) {
                                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
                                                }
                                            }

                                            Button(
                                                onClick = { onPairingCodeEntered(enteredCode) },
                                                enabled = enteredCode.length == 6,
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Default.Done, contentDescription = "Submit")
                                            }
                                        }

                                        if (isKeyboardOpen) {
                                            LaunchedEffect(Unit) {
                                                focusRequester.requestFocus()
                                            }
                                        }
                                    }

                                    if (!isKeyboardOpen) {
                                        Text(
                                            "Verification required to secure the connection.",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                    }
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