package com.kapcode.open.macropad.kmps

import MacroKTOR.MacroKtorClient
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.camera.core.*
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import android.hardware.camera2.CaptureRequest
import androidx.camera.core.FocusMeteringAction
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
import java.util.concurrent.TimeUnit
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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

import android.view.KeyEvent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import com.kapcode.open.macropad.kmps.settings.SlamFireTrigger

@ExperimentalGetImage
@OptIn(ExperimentalMaterial3Api::class)
class ClientActivity : ComponentActivity(), SensorEventListener {

    private var client: MacroKtorClient? = null
    private val activityScope = CoroutineScope(Dispatchers.Main.immediate)
    private var clientJob: Job? = null
    private val settingsViewModel = SettingsViewModel()
    private val clientViewModel = ClientViewModel()
    private lateinit var settingsStorage: SettingsStorage
    private val MAX_RETRIES = 5

    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var onOkayPressed: (() -> Unit)? = null
    private var onCancelPressed: (() -> Unit)? = null
    private var onSlamTriggered: ((Boolean) -> Unit)? = null
    private var lastProximityState: Boolean? = null // null = unknown, true = covered, false = uncovered
    private var lastTriggerTime = 0L
    private var triggerPending = false
    private var pendingTriggerType: String? = null // "BUTTON" or "PROXIMITY"
    private var lastToast: Toast? = null

    private fun showSlamToast(message: String) {
        lastToast?.cancel()
        lastToast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        lastToast?.show()
    }

    private fun handleSlamFire(isDouble: Boolean) {
        // If disconnected, any slam (single or double) returns to server list
        val uiState = clientViewModel.uiState.value
        if (uiState.disconnectReason != null) {
            showSlamToast("Disconnected: Returning to list")
            finish()
            return
        }

        onSlamTriggered?.invoke(isDouble)
        
        if (isDouble) {
            val doubleMacro = settingsViewModel.slamFireDoubleSelectedMacro.value
            if (doubleMacro != null) {
                sendMacro(doubleMacro)
                showSlamToast("Double Slam: $doubleMacro")
            } else {
                // Default negative action: Cancel/Back
                onCancelPressed?.invoke() ?: onBackPressedDispatcher.onBackPressed()
                showSlamToast("Double Slam: Cancel")
            }
        } else {
            val singleMacro = settingsViewModel.slamFireSelectedMacro.value
            if (singleMacro != null) {
                sendMacro(singleMacro)
                showSlamToast("Slam Fire: $singleMacro")
            } else {
                onOkayPressed?.invoke()
                showSlamToast("Slam Fire Triggered")
            }
        }
    }

    private fun processTrigger() {
        val now = System.currentTimeMillis()
        val threshold = settingsViewModel.slamFireDoubleThreshold.value
        
        if (now - lastTriggerTime < threshold) {
            // Double tap detected
            triggerPending = false
            handleSlamFire(true)
            lastTriggerTime = 0 // Reset to prevent triple tap as another double
        } else {
            // Potential single tap, wait to see if it's a double
            triggerPending = true
            lastTriggerTime = now
            activityScope.launch {
                delay(threshold + 10)
                if (triggerPending && System.currentTimeMillis() - lastTriggerTime >= threshold) {
                    triggerPending = false
                    handleSlamFire(false)
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            Toast.makeText(this, "Camera permission is required for QR scanning", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        val serverAddressFull = intent.getStringExtra("SERVER_ADDRESS")
        val deviceName = intent.getStringExtra("DEVICE_NAME") ?: "Android Device"
        val isSecure = intent.getBooleanExtra("IS_SECURE", false)
        val discoveryFingerprint = intent.getStringExtra("SERVER_FINGERPRINT")
        val addressParts = serverAddressFull?.split(":")
        val ipAddress = addressParts?.getOrNull(0)
        val port = addressParts?.getOrNull(1)?.toIntOrNull()

        if (ipAddress == null || port == null) {
            Log.e("ClientActivity", "Invalid server address provided. Finishing activity.")
            finish()
            return
        }

        Log.d("ClientActivity", "Targeting $ipAddress:$port (Secure: $isSecure)")

        settingsStorage = SettingsStorage(this)
        settingsStorage.bindViewModel(settingsViewModel, activityScope)

        setContent {
            val theme by settingsViewModel.theme.collectAsState()
            val uiState by clientViewModel.uiState.collectAsState()
            
            AppTheme(useDarkTheme = theme == SettingsAppTheme.DarkBlue) {
                val tokenManager = remember { TokenManager.getInstance(this@ClientActivity) }
                val tokenBalance by tokenManager.tokenBalance.collectAsState()

                LaunchedEffect(tokenBalance) {
                    client?.send(dataMessage("currency_update", tokenBalance.toLong().toString().encodeToByteArray()).toBytes())
                }

                LaunchedEffect(Unit) {
                    connectToServer(
                        ipAddress,
                        port,
                        deviceName,
                        isSecure,
                        discoveryFingerprint,
                        onUpdate = { status, name, reason, code ->
                            clientViewModel.updateConnection(status, name, reason, code)
                        },
                        onExecutionStart = { macro ->
                            clientViewModel.onMacroExecutionStart(macro)
                            // Deduct tokens on start
                            if (tokenManager.spendTokens(BillingConstants.TOKENS_PER_MACRO_PRESS)) {
                                activityScope.launch {
                                    client?.send(dataMessage("currency_spent", BillingConstants.TOKENS_PER_MACRO_PRESS.toLong().toString().encodeToByteArray()).toBytes())
                                }
                            }
                        },
                        onExecutionComplete = { macro ->
                            clientViewModel.onMacroExecutionComplete(macro)
                        },
                        onExecutionFailed = { macro, error ->
                            clientViewModel.onMacroExecutionFailed(macro)
                            // Refund tokens on failure
                            tokenManager.awardTokens(BillingConstants.TOKENS_PER_MACRO_PRESS)
                            Toast.makeText(this@ClientActivity, "Macro '$macro' failed: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                ClientScreen(
                    uiState = uiState,
                    settingsViewModel = settingsViewModel,
                    clientViewModel = clientViewModel,
                    currency = tokenBalance.toLong(),
                    onQrScannerToggle = { show ->
                        if (show) {
                            // HARD BLOCK: Never show QR if connected or if not in pairing state
                            val canShow = uiState.macros.isEmpty() && uiState.connectionStatus == "Pending Approval"
                            if (!canShow) {
                                Log.d("ClientActivity", "Ignoring QR toggle: Not in pairing state (Status: ${uiState.connectionStatus}, Macros: ${uiState.macros.size})")
                                return@ClientScreen
                            }
                            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                        clientViewModel.setQrScannerVisible(show)
                    },
                    onGetMacros = { 
                        Log.d("ClientActivity", "Requesting macros...")
                        activityScope.launch { 
                            client?.send(getMacrosRequest().toBytes()) 
                        } 
                    },
                    onMacroClick = ::sendMacro,
                    onPairingCodeEntered = { code ->
                        submitPairingCode(code)
                    },
                    onBackToMain = { finish() },
                    onOkayTriggerSet = { trigger ->
                        onOkayPressed = trigger
                    },
                    onCancelTriggerSet = { trigger ->
                        onCancelPressed = trigger
                    },
                    onSlamTriggerSet = { trigger ->
                        onSlamTriggered = trigger
                    }
                )
            }
        }
    }

    private fun submitPairingCode(code: String) {
        Log.d("ClientActivity", "Submitting pairing code: $code")
        activityScope.launch {
            val msg = controlMessage(ControlCommand.PAIRING_RESPONSE, mapOf("code" to code))
            client?.send(msg.toBytes())
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
        discoveryFingerprint: String?,
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
                        engine { 
                            if (isSecure) { 
                                val savedFingerprint = ServerStorage.getServerFingerprint(this@ClientActivity, "$ipAddress:$port")
                                    ?: discoveryFingerprint
                                
                                if (savedFingerprint != null) {
                                    preconfigured = createPinnedOkHttpClient(savedFingerprint)
                                } else {
                                    // Fallback to unsafe for first-time pairing if no fingerprint known
                                    preconfigured = createUnsafeOkHttpClient() 
                                }
                            } 
                        }
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

                    // Send initial currency balance
                    val tokenManager = TokenManager.getInstance(this@ClientActivity)
                    tempClient.send(dataMessage("currency_update", tokenManager.tokenBalance.value.toLong().toString().encodeToByteArray()).toBytes())

                    clientJob = activityScope.launch {
                        while (isActive) {
                            val msg = getMacrosRequest().toBytes()
                            client?.send(msg)
                            delay(5000)
                            if (clientViewModel.uiState.value.macros.isNotEmpty()) break
                        }
                    }

                    val watchdogJob = activityScope.launch {
                        while (isActive) {
                            delay(5000)
                            if (System.currentTimeMillis() - lastHeartbeat > 40000) {
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
                                            ControlCommand.AUTH_CHALLENGE -> {
                                                val fingerprint = params["fingerprint"]
                                                if (fingerprint != null) {
                                                    // Store the fingerprint temporarily for this session 
                                                    // It will be permanently saved on PAIRING_APPROVED
                                                    Log.d("ClientActivity", "Received server fingerprint: $fingerprint")
                                                    ServerStorage.saveServerFingerprint(this@ClientActivity, "$ipAddress:$port", fingerprint)
                                                }
                                                // Note: 'challenge' for authentication is handled internally by MacroKtorClient
                                            }
                                            ControlCommand.PAIRING_PENDING -> {
                                                clientViewModel.setMacros(emptyList())
                                                val code = params["code"]
                                                currentVerificationCode = code
                                                onUpdate("Pending Approval", null, null, code)
                                                
                                                val preferredMode = params["preferredMode"]
                                                if (preferredMode == "QR") {
                                                    clientViewModel.setQrScannerVisible(true)
                                                }
                                            }
                                            ControlCommand.PAIRING_CODE_MATCHED -> {
                                                onUpdate("Code Matched", null, null, currentVerificationCode)
                                            }
                                            ControlCommand.PAIRING_APPROVED -> {
                                                onUpdate("Connected", ipAddress, null, null)
                                                clientViewModel.setQrScannerVisible(false)
                                                activityScope.launch {
                                                    tempClient.send(textMessage("getMacros").toBytes())
                                                }
                                            }
                                            ControlCommand.PAIRING_REJECTED -> {
                                                clientViewModel.setMacros(emptyList())
                                                onUpdate("Pairing Denied", null, params["reason"] ?: "Server rejected pairing.", null)
                                                this@launch.cancel()
                                            }
                                            ControlCommand.BANNED -> {
                                                clientViewModel.setMacros(emptyList())
                                                val reason = params["reason"] ?: "Device is banned"
                                                onUpdate("Banned", null, reason, null)
                                                this@launch.cancel()
                                            }
                                            ControlCommand.DISCONNECT -> {
                                                clientViewModel.setMacros(emptyList())
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
                                            val macroNames = text.substringAfter("macros:").split(",").filter { it.isNotBlank() }
                                            clientViewModel.setMacros(macroNames)
                                            clientViewModel.updateConnection("Connected", ipAddress, null, null)
                                        }
                                    },
                                    onHeartbeat = {
                                        lastHeartbeat = System.currentTimeMillis()
                                    },
                                    onData = { key, value ->
                                        if (key == "currency_update") {
                                            try {
                                                val balance = value.decodeToString().toLong()
                                                clientViewModel.updateCurrency(balance)
                                            } catch (e: Exception) {
                                                Log.e("ClientActivity", "Failed to parse currency_update", e)
                                            }
                                        }
                                    }
                                )
                            } catch (e: Exception) {
                                Log.e("ClientActivity", "Error parsing DataModel", e)
                            }
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

    private fun createPinnedOkHttpClient(expectedFingerprint: String): OkHttpClient {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                if (chain == null || chain.isEmpty()) throw java.security.cert.CertificateException("Empty certificate chain")
                
                val cert = chain[0]
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                val fingerprint = digest.digest(cert.encoded).joinToString(":") { "%02X".format(it) }
                
                if (fingerprint != expectedFingerprint) {
                    Log.e("Security", "CERTIFICATE PINNING FAILURE!")
                    Log.e("Security", "Expected: $expectedFingerprint")
                    Log.e("Security", "Found:    $fingerprint")
                    throw java.security.cert.CertificateException("Certificate fingerprint mismatch! Potential MITM attack.")
                }
            }
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), java.security.SecureRandom())

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true } // Still use hostname verifier true because we pin the cert itself
            .build()
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (settingsViewModel.slamFireEnabled.value) {
            val trigger = settingsViewModel.slamFireTrigger.value
            val isMatch = when (trigger) {
                SlamFireTrigger.VolumeDown -> keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                SlamFireTrigger.VolumeUp -> keyCode == KeyEvent.KEYCODE_VOLUME_UP
                SlamFireTrigger.Power -> keyCode == KeyEvent.KEYCODE_POWER
                SlamFireTrigger.Bixby -> keyCode == 1082
                SlamFireTrigger.Assistant -> keyCode == KeyEvent.KEYCODE_ASSIST || keyCode == KeyEvent.KEYCODE_VOICE_ASSIST
                else -> false
            }
            if (isMatch) {
                processTrigger()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PROXIMITY && settingsViewModel.slamFireEnabled.value) {
            val distance = event.values[0]
            val maxRange = event.sensor.maximumRange
            
            val threshold = if (maxRange > 5f) 5f else maxRange / 2f
            val isCovered = distance < threshold
            
            if (isCovered != lastProximityState) {
                lastProximityState = isCovered
                val trigger = settingsViewModel.slamFireTrigger.value
                val isTriggered = (trigger == SlamFireTrigger.ProximityCovered && isCovered) || 
                                 (trigger == SlamFireTrigger.ProximityUncovered && !isCovered)
                
                if (isTriggered) {
                    processTrigger()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        proximitySensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        clientJob?.cancel()
        activityScope.cancel()
        Log.d("ClientActivity", "ClientActivity destroyed")
    }
}

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
@ExperimentalGetImage
@Composable
fun QrCodeScanner(
    onCodeScanned: (String) -> Unit,
    onClose: () -> Unit,
    isAutoZoomEnabled: Boolean = false,
    isAutoFocusEnabled: Boolean = true,
    manualZoomRatio: Float = 1f,
    manualFocusDistance: Float = 0f,
    onManualZoomChange: (Float) -> Unit = {},
    onManualFocusChange: (Float) -> Unit = {},
    onAutoZoomToggle: (Boolean) -> Unit = {},
    onAutoFocusToggle: (Boolean) -> Unit = {},
    onCameraReady: (Camera) -> Unit = {}
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

    // Toast hint for manual focus optimization
    val scannerStartTime = remember { System.currentTimeMillis() }
    var hasShownFocusToast by rememberSaveable { mutableStateOf(false) }
    
    LaunchedEffect(isAutoFocusEnabled) {
        if (!isAutoFocusEnabled && !hasShownFocusToast) {
            val elapsed = System.currentTimeMillis() - scannerStartTime
            if (elapsed < 5000) {
                delay(5000 - elapsed)
            }
            // Check again after delay
            if (!isAutoFocusEnabled && !hasShownFocusToast) {
                Toast.makeText(context, "Focus not adequate? Device on a mount? Just wave hand in front of camera.", Toast.LENGTH_LONG).show()
                hasShownFocusToast = true
            }
        }
    }

    // Pinch-to-zoom detector
    val scaleGestureDetector = remember {
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                camera?.let { cam ->
                    val zoomState = cam.cameraInfo.zoomState.value
                    val currentZoomRatio = zoomState?.zoomRatio ?: 1f
                    val newZoomRatio = currentZoomRatio * detector.scaleFactor
                    cam.cameraControl.setZoomRatio(newZoomRatio.coerceIn(
                        zoomState?.minZoomRatio ?: 1f,
                        zoomState?.maxZoomRatio ?: 1f
                    ))
                }
                return true
            }
        })
    }

    val currentOnCodeScanned by rememberUpdatedState(onCodeScanned)
    val currentOnCameraReady by rememberUpdatedState(onCameraReady)

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

                val imageAnalysisBuilder = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)

                Camera2Interop.Extender(imageAnalysisBuilder).setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    android.util.Range(20, 30)
                )

                val imageAnalysis = imageAnalysisBuilder.build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    barcode.rawValue?.let { code ->
                                        // Only accept 6-digit numeric codes to prevent accidental triggers from environmental QRs
                                        if (code.length == 6 && code.all { it.isDigit() }) {
                                            currentOnCodeScanned(code)
                                        }
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

                val boundCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                camera = boundCamera
                currentOnCameraReady(boundCamera)
                Log.d("QrCodeScanner", "Camera bound successfully")
            } catch (e: Exception) {
                Log.e("QrCodeScanner", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Auto-Focus/Zoom Controller
    LaunchedEffect(camera, isAutoZoomEnabled, isAutoFocusEnabled, manualZoomRatio, manualFocusDistance) {
        val cam = camera ?: return@LaunchedEffect
        
        // Loop for auto-focus or auto-zoom
        if (isAutoZoomEnabled || isAutoFocusEnabled) {
            val intervals = listOf(0.0f, 0.33f, 0.66f, 1.0f) // Normalized
            var currentIndex = 0

            while(isActive) {
                val zoomState = cam.cameraInfo.zoomState.value
                val minZoom = zoomState?.minZoomRatio ?: 1f
                val maxZoom = (zoomState?.maxZoomRatio ?: 3f).coerceAtMost(4f)
                
                // 1. Handle Zoom
                if (isAutoZoomEnabled) {
                    val targetLevel = intervals[currentIndex]
                    val targetRatio = minZoom + (maxZoom - minZoom) * targetLevel
                    cam.cameraControl.setZoomRatio(targetRatio)
                } else {
                    cam.cameraControl.setZoomRatio(manualZoomRatio)
                }
                
                // 2. Handle Focus
                if (isAutoFocusEnabled) {
                    val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
                    val centerPoint = factory.createPoint(0.5f, 0.5f)
                    val action = FocusMeteringAction.Builder(centerPoint, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB)
                        .setAutoCancelDuration(2, TimeUnit.SECONDS)
                        .build()
                    
                    cam.cameraControl.startFocusAndMetering(action)
                    // Frequent refocusing with AE/AWB help for screen scanning
                    delay(2500)
                } else {
                    // Manual focus mode within the loop if auto-zoom is on
                    val camera2Control = Camera2CameraControl.from(cam.cameraControl)
                    camera2Control.captureRequestOptions = CaptureRequestOptions.Builder()
                        .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                        .setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, manualFocusDistance * 10f)
                        .build()
                    delay(2000)
                }

                currentIndex = (currentIndex + 1) % intervals.size
            }
        } else {
            // Manual mode: strictly follow manualZoomRatio
            cam.cameraControl.setZoomRatio(manualZoomRatio)
            
            // Manual Focus
            val camera2Control = Camera2CameraControl.from(cam.cameraControl)
            camera2Control.captureRequestOptions = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                .setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, manualFocusDistance * 10f)
                .build()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.setOnTouchListener { _, event ->
                    scaleGestureDetector.onTouchEvent(event)
                    if (event.action == MotionEvent.ACTION_UP && !scaleGestureDetector.isInProgress) {
                        val factory = view.meteringPointFactory
                        val point = factory.createPoint(event.x, event.y)
                        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB)
                            .build()
                        camera?.cameraControl?.startFocusAndMetering(action)
                    }
                    true
                }
            }
        )

        // QR Scanner Overlay (White Square)
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.Center)
                .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
        ) {
            // Target reticle / Corner accents
            val cornerSize = 40.dp
            val cornerWidth = 4.dp
            val color = Color.White
            
            // Top Left
            Box(Modifier.size(cornerSize).align(Alignment.TopStart).border(width = cornerWidth, color = color, shape = RoundedCornerShape(topStart = 12.dp)))
            // Top Right
            Box(Modifier.size(cornerSize).align(Alignment.TopEnd).border(width = cornerWidth, color = color, shape = RoundedCornerShape(topEnd = 12.dp)))
            // Bottom Left
            Box(Modifier.size(cornerSize).align(Alignment.BottomStart).border(width = cornerWidth, color = color, shape = RoundedCornerShape(bottomStart = 12.dp)))
            // Bottom Right
            Box(Modifier.size(cornerSize).align(Alignment.BottomEnd).border(width = cornerWidth, color = color, shape = RoundedCornerShape(bottomEnd = 12.dp)))
            
            // Center Dot
            Box(Modifier.size(8.dp).align(Alignment.Center).background(color.copy(alpha = 0.5f), CircleShape))
        }

        // Focus & Zoom Controls Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isAutoFocusEnabled) {
                Text(
                    "Manual Focus",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Slider(
                    value = manualFocusDistance,
                    onValueChange = { 
                        onManualFocusChange(it)
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.width(200.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                Spacer(Modifier.height(16.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Toggle Focus Mode
                IconButton(
                    onClick = { 
                        onAutoFocusToggle(!isAutoFocusEnabled)
                    },
                    modifier = Modifier.background(
                        if (!isAutoFocusEnabled) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(
                        if (!isAutoFocusEnabled) Icons.Default.FilterCenterFocus else Icons.Default.CenterFocusWeak,
                        contentDescription = "Manual Focus",
                        tint = Color.White
                    )
                }
                
                // Toggle Auto-Zoom
                IconButton(
                    onClick = { 
                        onAutoZoomToggle(!isAutoZoomEnabled)
                    },
                    modifier = Modifier.background(
                        if (isAutoZoomEnabled) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(
                        Icons.Default.ZoomIn,
                        contentDescription = "Auto Zoom",
                        tint = Color.White
                    )
                }
            }
        }
        
        // Overlay for Switch Camera
        IconButton(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
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
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalGetImage
@Composable
fun ClientScreen(
    uiState: ClientUiState,
    settingsViewModel: SettingsViewModel,
    clientViewModel: ClientViewModel,
    currency: Long = 0,
    onGetMacros: () -> Unit,
    onMacroClick: (String) -> Unit,
    onPairingCodeEntered: (String) -> Unit,
    onBackToMain: () -> Unit,
    onOkayTriggerSet: (() -> Unit) -> Unit = {},
    onCancelTriggerSet: (() -> Unit) -> Unit = {},
    onSlamTriggerSet: ((Boolean) -> Unit) -> Unit = {},
    onQrScannerToggle: (Boolean) -> Unit = {}
) {
    val connectionStatus = uiState.connectionStatus
    val serverName = uiState.serverName
    val disconnectReason = uiState.disconnectReason
    val macros = uiState.macros
    val executingMacros = uiState.executingMacros
    val failedMacros = uiState.failedMacros
    val showQrScanner = uiState.showQrScanner
    val isAutoZoomEnabled_State = uiState.isAutoZoomEnabled
    val isAutoFocusEnabled_State = uiState.isAutoFocusEnabled
    val manualZoomRatio_State = uiState.manualZoomRatio
    val manualFocusDistance_State = uiState.manualFocusDistance

    var showSettings by remember { mutableStateOf(false) }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(onBackToMain) {
        onCancelTriggerSet(onBackToMain)
    }

    LaunchedEffect(macros.isEmpty(), connectionStatus) {
        onSlamTriggerSet { isDouble ->
            // ONLY allow slam to OPEN QR if we are explicitly in "Pending Approval"
            // This ensures it only triggers when the user is actually at the step that needs QR/PIN
            val isAtPairingStep = connectionStatus == "Pending Approval"
            
            if (isAtPairingStep && !showQrScanner) {
                if (!isDouble) {
                    onQrScannerToggle(true)
                }
            } else if (showQrScanner) {
                // If scanner is ALREADY showing, double slam can turn it off
                if (isDouble) {
                    onQrScannerToggle(false)
                }
            }
        }
    }

    val slamFireEnabled by settingsViewModel.slamFireEnabled.collectAsState()
    val slamFireSelectedMacro by settingsViewModel.slamFireSelectedMacro.collectAsState()
    val slamFireDoubleSelectedMacro by settingsViewModel.slamFireDoubleSelectedMacro.collectAsState()
    
    val onOkayAction = {
        if (showQrScanner) {
            // No obvious "Okay" for scanner, maybe do nothing or toggle?
        } else if (connectionStatus == "Pending Approval") {
            // Could auto-submit code if length is 6? 
            // For now, let's just use it as a generic "Okay"
        }
    }

    LaunchedEffect(connectionStatus, showQrScanner) {
        onOkayTriggerSet(onOkayAction)
    }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    Scaffold(
        topBar = {
            CommonAppBar(
                title = if (showSettings) "Settings" else "Open Macropad",
                onSettingsClick = { showSettings = !showSettings },
                currency = uiState.currency,
                isQrScannerActive = showQrScanner && !showSettings,
                isAutoZoomEnabled = isAutoZoomEnabled_State,
                onAutoZoomToggle = { clientViewModel.setAutoZoomEnabled(it) },
                isAutoFocusEnabled = isAutoFocusEnabled_State,
                onAutoFocusToggle = { clientViewModel.setAutoFocusEnabled(it) },
                onZoomIn = {
                    clientViewModel.setAutoZoomEnabled(false)
                    activeCamera?.let { cam ->
                        val zoomState = cam.cameraInfo.zoomState.value
                        val maxZoom = zoomState?.maxZoomRatio ?: 1f
                        val newZoom = (manualZoomRatio_State + 0.2f).coerceAtMost(maxZoom)
                        clientViewModel.setManualZoomRatio(newZoom)
                        cam.cameraControl.setZoomRatio(newZoom)
                    }
                },
                onZoomOut = {
                    clientViewModel.setAutoZoomEnabled(false)
                    activeCamera?.let { cam ->
                        val zoomState = cam.cameraInfo.zoomState.value
                        val minZoom = zoomState?.minZoomRatio ?: 1f
                        val newZoom = (manualZoomRatio_State - 0.2f).coerceAtLeast(minZoom)
                        clientViewModel.setManualZoomRatio(newZoom)
                        cam.cameraControl.setZoomRatio(newZoom)
                    }
                },
                onCloseScanner = { onQrScannerToggle(false) },
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
                },
                actions = {
                    if (!showSettings && macros.isNotEmpty() && slamFireEnabled) {
                        var expandedSingle by remember { mutableStateOf(false) }
                        var expandedDouble by remember { mutableStateOf(false) }

                        // Scrollable container for Slam Fire dropdowns
                        Row(
                            modifier = Modifier
                                .width(180.dp)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Single Slam Dropdown
                            Box {
                                TextButton(
                                    onClick = { expandedSingle = true },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.TouchApp, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text(
                                        text = slamFireSelectedMacro ?: "None",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                DropdownMenu(expanded = expandedSingle, onDismissRequest = { expandedSingle = false }) {
                                    DropdownMenuItem(
                                        text = { Text("None (OK)") },
                                        onClick = {
                                            settingsViewModel.setSlamFireSelectedMacro(null)
                                            expandedSingle = false
                                        }
                                    )
                                    macros.forEach { macro ->
                                        DropdownMenuItem(
                                            text = { Text(macro) },
                                            onClick = {
                                                settingsViewModel.setSlamFireSelectedMacro(macro)
                                                expandedSingle = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Double Slam Dropdown
                            Box {
                                TextButton(
                                    onClick = { expandedDouble = true },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.DoubleArrow, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text(
                                        text = slamFireDoubleSelectedMacro ?: "Cancel",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                DropdownMenu(expanded = expandedDouble, onDismissRequest = { expandedDouble = false }) {
                                    DropdownMenuItem(
                                        text = { Text("None (Cancel)") },
                                        onClick = {
                                            settingsViewModel.setSlamFireDoubleSelectedMacro(null)
                                            expandedDouble = false
                                        }
                                    )
                                    macros.forEach { macro ->
                                        DropdownMenuItem(
                                            text = { Text(macro) },
                                            onClick = {
                                                settingsViewModel.setSlamFireDoubleSelectedMacro(macro)
                                                expandedDouble = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val isConnected = connectionStatus == "Connected" && macros.isNotEmpty()
            if (!showSettings && !isLandscape && isConnected) {
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
                    if (macros.isNotEmpty() && !showQrScanner) {
                        MacroButtonsScreen(
                            macros = macros, 
                            executingMacros = executingMacros,
                            failedMacros = failedMacros,
                            currency = uiState.currency,
                            onMacroClick = onMacroClick
                        )
                    } else if (showQrScanner) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val configuration = LocalConfiguration.current
                            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                            Box(
                                modifier = if (isLandscape) {
                                    Modifier.fillMaxHeight().aspectRatio(1f)
                                } else {
                                    Modifier.fillMaxWidth().aspectRatio(1f)
                                }
                            ) {
                                QrCodeScanner(
                                    onCodeScanned = { code: String ->
                                        onPairingCodeEntered(code)
                                        onQrScannerToggle(false)
                                    },
                                    onClose = { onQrScannerToggle(false) },
                                    isAutoZoomEnabled = isAutoZoomEnabled_State,
                                    isAutoFocusEnabled = isAutoFocusEnabled_State,
                                    manualZoomRatio = manualZoomRatio_State,
                                    manualFocusDistance = manualFocusDistance_State,
                                    onManualZoomChange = { clientViewModel.setManualZoomRatio(it) },
                                    onManualFocusChange = { clientViewModel.setManualFocusDistance(it) },
                                    onAutoZoomToggle = { clientViewModel.setAutoZoomEnabled(it) },
                                    onAutoFocusToggle = { clientViewModel.setAutoFocusEnabled(it) },
                                    onCameraReady = { activeCamera = it }
                                )
                            }
                        }
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
                            if (connectionStatus == "Pending Approval" || connectionStatus == "Code Matched") {
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

                                            IconButton(onClick = { onQrScannerToggle(true) }) {
                                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
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
                                val configuration = LocalConfiguration.current
                                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                                
                                if (isLandscape) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowBack, 
                                                contentDescription = null, 
                                                modifier = Modifier.size(64.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Spacer(Modifier.width(32.dp))
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Disconnected", style = MaterialTheme.typography.headlineMedium)
                                            Spacer(Modifier.height(8.dp))
                                            Text(disconnectReason, style = MaterialTheme.typography.bodyLarge)
                                            Spacer(Modifier.height(16.dp))
                                            Button(onClick = onBackToMain) {
                                                Text("Back to Server List")
                                            }
                                            if (slamFireEnabled) {
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    "Tip: You can slam to go back",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                } else {
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
                                    if (slamFireEnabled) {
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            "Tip: You can slam to go back",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
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
