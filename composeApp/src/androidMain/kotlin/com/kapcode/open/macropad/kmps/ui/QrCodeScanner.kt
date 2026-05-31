package com.kapcode.open.macropad.kmps.ui

import android.hardware.camera2.CaptureRequest
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
    onCameraReady: (Camera) -> Unit = {},
    isLowPowerMode: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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

    LaunchedEffect(cameraSelector, isLowPowerMode) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            try {
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysisBuilder = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                
                if (isLowPowerMode) {
                    imageAnalysisBuilder.setTargetResolution(android.util.Size(640, 480))
                } else {
                    // Higher resolution for better distance/small code detection in performance mode
                    imageAnalysisBuilder.setTargetResolution(android.util.Size(1280, 720))
                }

                val camera2Extender = Camera2Interop.Extender(imageAnalysisBuilder)
                camera2Extender.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    if (isLowPowerMode) android.util.Range(5, 10) else android.util.Range(25, 30)
                )

                if (!isLowPowerMode) {
                    // Optimize for barcode scanning specifically
                    camera2Extender.setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE)
                    camera2Extender.setCaptureRequestOption(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_BARCODE)
                    
                    // Boost exposure for screen scanning
                    camera2Extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                    camera2Extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 2) 
                    
                    // Force continuous focus in performance mode
                    camera2Extender.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                }

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
    LaunchedEffect(camera, isAutoZoomEnabled, isAutoFocusEnabled, manualZoomRatio, manualFocusDistance, isLowPowerMode) {
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
                        .setAutoCancelDuration(if (isLowPowerMode) 5 else 2, TimeUnit.SECONDS)
                        .build()
                    
                    cam.cameraControl.startFocusAndMetering(action)
                    // Ultra-fast refocusing in performance mode to find codes quicker
                    delay(if (isLowPowerMode) 5000 else 1000)
                } else {
                    // Manual focus mode within the loop if auto-zoom is on
                    val camera2Control = Camera2CameraControl.from(cam.cameraControl)
                    camera2Control.captureRequestOptions = CaptureRequestOptions.Builder()
                        .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                        .setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, manualFocusDistance * 10f)
                        .build()
                    delay(if (isLowPowerMode) 4000 else 2000)
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
                .border(2.dp, Color.White.copy(alpha = if (isLowPowerMode) 0.3f else 0.7f), RoundedCornerShape(12.dp))
        ) {
            if (isLowPowerMode) {
                Text(
                    "Low Power Mode",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                )
            }
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
