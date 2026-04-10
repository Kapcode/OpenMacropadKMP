package com.kapcode.open.macropad.kmps

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.kapcode.open.macropad.kmps.settings.AppTheme as SettingsAppTheme
import com.kapcode.open.macropad.kmps.settings.SettingsScreen
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel
import com.kapcode.open.macropad.kmps.ui.components.CommonAppBar
import com.kapcode.open.macropad.kmps.ui.components.LoadingIndicator
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme
import kotlinx.coroutines.delay
import openmacropadkmp.composeapp.generated.resources.Res
import openmacropadkmp.composeapp.generated.resources.macropadIcon512
import org.jetbrains.compose.resources.painterResource

const val TAG = "MainActivity"

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val clientDiscovery by lazy { ClientDiscovery() }
    private val settingsViewModel by lazy { SettingsViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val splashScreenVisible = remember { mutableStateOf(true) }
            var isBlinking by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                // Start blinking immediately as we enter Compose splash
                isBlinking = true
                
                launch(Dispatchers.IO) {
                    TokenManager.getInstance(this@MainActivity)
                    try {
                        IdentityManager().getIdentityPublicKey()
                    } catch (e: Exception) {
                        Log.e(TAG, "IdentityManager initialization failed", e)
                    }
                    if (settingsViewModel.analyticsEnabled.value) {
                        FirebaseApp.initializeApp(this@MainActivity)
                    }
                    MobileAds.initialize(this@MainActivity)
                    clientDiscovery.start()
                }

                delay(1500) // Show the blinking cursor for a moment before entering the app
                splashScreenVisible.value = false
            }

            // Dismiss system splash immediately to show our custom Compose splash
            splashScreen.setKeepOnScreenCondition { false }

            Crossfade(
                targetState = splashScreenVisible.value,
                animationSpec = tween(500),
                label = "splashTransition"
            ) { isVisible ->
                if (isVisible) {
                    SplashUI(isBlinking = isBlinking)
                } else {
                    MainUI(
                        settingsViewModel = settingsViewModel,
                        clientDiscovery = clientDiscovery,
                        onLaunchClient = { serverInfo: ServerInfo, deviceName: String ->
                            Log.d(
                                TAG,
                                "Launching ClientActivity for: ${serverInfo.address} with device name: $deviceName (Secure: ${serverInfo.isSecure})"
                            )
                            val intent = Intent(this@MainActivity, ClientActivity::class.java).apply {
                                putExtra("SERVER_ADDRESS", serverInfo.address)
                                putExtra("DEVICE_NAME", deviceName)
                                putExtra("IS_SECURE", serverInfo.isSecure)
                                putExtra("SERVER_FINGERPRINT", serverInfo.fingerprint)
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun SplashUI(isBlinking: Boolean) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Secondary splash image: exactly the same size as the initial splash safe-zone
                Image(
                    painter = painterResource(Res.drawable.macropadIcon512),
                    contentDescription = "App Icon",
                    modifier = Modifier.size(192.dp)
                )
                
                // Positioned to match the 'top="48dp"' from splash_icon_centered.xml
                // We adjust for the height of the icon (192/2 = 96dp from center)
                // In XML: item(192dp) + top(48dp) relative to center
                Spacer(modifier = Modifier.height(32.dp))
                
                LoadingIndicator(
                    isBlinking = isBlinking,
                    showText = false
                )
            }

            // Keep branding at the bottom
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 48.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = "Kapcode - Software Made Simple",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Removed eager discovery start to save resources until user clicks "Scan"
    }

    override fun onPause() {
        super.onPause()
        // clientDiscovery is lazy, only stop if it was initialized
    }

    override fun onDestroy() {
        super.onDestroy()
        // clientDiscovery is lazy, only stop if it was initialized
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val sampleServers = listOf(
        ServerInfo("Server 1", "192.168.1.100:8443", true),
        ServerInfo("Desktop-PC", "192.168.1.108:8449", true)
    )
    AppTheme {
        App(
            scanServers = {},
            stopScanning = {},
            foundServers = sampleServers,
            onConnectClick = { _, _ -> }
        )
    }
}