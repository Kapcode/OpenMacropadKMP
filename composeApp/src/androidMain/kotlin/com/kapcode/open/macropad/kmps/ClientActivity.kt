package com.kapcode.open.macropad.kmps

import com.kapcode.open.macropad.kmps.network.sockets.MacroKtorClient
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
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.Block
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
import com.kapcode.open.macropad.kmps.network.ClientRepository
import com.kapcode.open.macropad.kmps.models.GridWidget
import com.kapcode.open.macropad.kmps.models.WidgetType
import com.kapcode.open.macropad.kmps.network.sockets.model.*
import com.kapcode.open.macropad.kmps.settings.AppTheme as SettingsAppTheme
import com.kapcode.open.macropad.kmps.settings.ClientSettingsSection
import com.kapcode.open.macropad.kmps.settings.SettingsScreen
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel
import com.kapcode.open.macropad.kmps.ui.components.CommonAppBar
import com.kapcode.open.macropad.kmps.ui.ClientScreen
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme
import android.view.KeyEvent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import com.kapcode.open.macropad.kmps.settings.SlamFireTrigger
import kotlinx.coroutines.*

@ExperimentalGetImage
@OptIn(ExperimentalMaterial3Api::class)
class ClientActivity : ComponentActivity() {

    private val settingsViewModel = MacroApplication.settingsViewModel
    private lateinit var clientRepository: ClientRepository
    private lateinit var clientViewModel: ClientViewModel
    private lateinit var settingsStorage: SettingsStorage
    private lateinit var billingManager: BillingManager
    private lateinit var slamFireManager: SlamFireManager
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var onOkayPressed: (() -> Unit)? = null
    private var onCancelPressed: (() -> Unit)? = null
    private var onSlamTriggered: ((Boolean) -> Unit)? = null
    private var lastToast: Toast? = null
    private var isHandlingSlam = false

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

        val serverAddressFull = intent.getStringExtra("SERVER_ADDRESS")
        val serverName = intent.getStringExtra("SERVER_NAME")
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

        clientRepository = ClientRepository(this)
        clientViewModel = ClientViewModel(clientRepository)
        settingsStorage = SettingsStorage(this)
        settingsStorage.bindViewModel(settingsViewModel, clientViewModel, activityScope)

        billingManager = BillingManager.getInstance(this)
        billingManager.startConnection(settingsViewModel)

        slamFireManager = SlamFireManager(this, settingsViewModel, activityScope) { isDouble ->
            handleSlamFire(isDouble)
        }

        MacroApplication.analyticsManager.trackScreen("Client", "ClientActivity")

        setContent {
            val theme by settingsViewModel.theme.collectAsState()
            val uiState by clientViewModel.uiState.collectAsState()
            
            AppTheme(useDarkTheme = theme == SettingsAppTheme.DarkBlue) {
                CompositionLocalProvider(com.kapcode.open.macropad.kmps.utils.LocalClipboardManager provides com.kapcode.open.macropad.kmps.utils.ClipboardManager()) {
                    val tokenManager = remember { TokenManager.getInstance(this@ClientActivity) }
                val tokenBalance by tokenManager.tokenBalance.collectAsState()

                LaunchedEffect(Unit) {
                    clientViewModel.connect(
                        ipAddress = ipAddress,
                        port = port,
                        deviceName = deviceName,
                        isSecure = isSecure,
                        discoveryFingerprint = discoveryFingerprint,
                        serverName = serverName,
                        tokenManager = tokenManager,
                        settingsViewModel = settingsViewModel,
                        context = this@ClientActivity,
                        onExecutionFailedToast = { message ->
                            Toast.makeText(this@ClientActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                LaunchedEffect(tokenBalance) {
                    clientViewModel.syncCurrency(tokenBalance.toLong())
                }

                ClientScreen(
                    uiState = uiState,
                    settingsViewModel = settingsViewModel,
                    clientViewModel = clientViewModel,
                    currency = tokenBalance.toLong(),
                    billingManager = billingManager,
                    onQrScannerToggle = { show ->
                        if (show) {
                            val canShow = uiState.macros.isEmpty() && uiState.connectionStatus == "Pending Approval"
                            if (!canShow) {
                                Log.d("ClientActivity", "Ignoring QR toggle: Not in pairing state")
                                return@ClientScreen
                            }
                            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                        clientViewModel.setQrScannerVisible(show)
                    },
                    onGetMacros = { clientViewModel.requestMacros() },
                    onWidgetInteraction = { widget -> 
                        when (widget.type) {
                            WidgetType.BUTTON -> clientViewModel.sendMacro(widget.macroId)
                            WidgetType.TOGGLE -> {
                                // For toggle, we send the new state
                                clientRepository.sendData("widget_state", "${widget.id}|${!widget.state}")
                            }
                            WidgetType.SLIDER_HORIZONTAL, WidgetType.SLIDER_VERTICAL -> {
                                clientRepository.sendData("widget_value", "${widget.id}|${widget.value}")
                            }
                        }
                    },
                    onPairingCodeEntered = { clientViewModel.submitPairingCode(it) },
                    onBackToMain = { 
                        clientViewModel.disconnect()
                        finish() 
                    },
                    onOkayTriggerSet = { trigger -> onOkayPressed = trigger },
                    onCancelTriggerSet = { trigger -> onCancelPressed = trigger },
                    onSlamTriggerSet = { trigger -> onSlamTriggered = trigger },
                    tokenManager = tokenManager,
                    onExecutionFailedToast = { message ->
                        Toast.makeText(this@ClientActivity, message, Toast.LENGTH_SHORT).show()
                    }
                )
                }
            }
        }
    }

    private fun sendMacro(macroName: String) {
        clientViewModel.sendMacro(macroName)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (slamFireManager.handleKeyDown(keyCode)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        slamFireManager.start()
    }

    override fun onPause() {
        super.onPause()
        slamFireManager.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
        Log.d("ClientActivity", "ClientActivity destroyed")
    }
}

