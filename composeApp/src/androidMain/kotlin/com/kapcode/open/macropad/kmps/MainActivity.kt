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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
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
        val activityCreateStart = System.currentTimeMillis() - MacroApplication.appStartTime
        Log.i(MacroApplication.TAG, "${activityCreateStart}ms: [Activity] onCreate started")
        
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        Log.i(MacroApplication.TAG, "${System.currentTimeMillis() - MacroApplication.appStartTime}ms: [Activity] super.onCreate finished")

        // Initialize TokenManager, ClientDiscovery, and IdentityManager in background
        lifecycleScope.launch(Dispatchers.IO) {
            val tmStart = System.currentTimeMillis() - MacroApplication.appStartTime
            Log.i(MacroApplication.TAG, "${tmStart}ms: [TokenManager] init started (background)")
            TokenManager.getInstance(this@MainActivity)
            val tmEnd = System.currentTimeMillis() - MacroApplication.appStartTime
            Log.i(MacroApplication.TAG, "${tmEnd}ms: [TokenManager] init finished (background)")

            val imStart = System.currentTimeMillis() - MacroApplication.appStartTime
            Log.i(MacroApplication.TAG, "${imStart}ms: [IdentityManager] init started (background)")
            IdentityManager().getIdentityPublicKey() // Triggers key generation if needed
            val imEnd = System.currentTimeMillis() - MacroApplication.appStartTime
            Log.i(MacroApplication.TAG, "${imEnd}ms: [IdentityManager] init finished (background)")

            val cdStart = System.currentTimeMillis() - MacroApplication.appStartTime
            Log.i(MacroApplication.TAG, "${cdStart}ms: [ClientDiscovery] auto-start (background)")
            clientDiscovery.start()
        }

        Log.i(MacroApplication.TAG, "${System.currentTimeMillis() - MacroApplication.appStartTime}ms: [Activity] Background tasks launched")
        
        setContent {
            val setContentStart = System.currentTimeMillis() - MacroApplication.appStartTime
            Log.i(MacroApplication.TAG, "${setContentStart}ms: [Activity] setContent started")
            
            val splashScreenVisible = remember { mutableStateOf(true) }
            val fbEnabled by settingsViewModel.analyticsEnabled.collectAsState()
            
            LaunchedEffect(Unit) {
                val effectStart = System.currentTimeMillis() - MacroApplication.appStartTime
                Log.i(MacroApplication.TAG, "${effectStart}ms: [Activity] LaunchedEffect started (UI Ready)")
                
                // Initialize MobileAds and Firebase here, AFTER the splash is gone and UI is ready
                launch(Dispatchers.IO) {
                        if (fbEnabled) {
                            val fbStart = System.currentTimeMillis() - MacroApplication.appStartTime
                            Log.i(MacroApplication.TAG, "${fbStart}ms: [Firebase] init started (background)")
                            FirebaseApp.initializeApp(this@MainActivity)
                            val fbEnd = System.currentTimeMillis() - MacroApplication.appStartTime
                            Log.i(MacroApplication.TAG, "${fbEnd}ms: [Firebase] init finished (background)")
                        } else {
                            Log.i(MacroApplication.TAG, "Firebase Analytics disabled by user.")
                        }
                        
                        val adsStart = System.currentTimeMillis() - MacroApplication.appStartTime
                        Log.i(MacroApplication.TAG, "${adsStart}ms: [MobileAds] init started (background)")
                        MobileAds.initialize(this@MainActivity) {
                            val adsEnd = System.currentTimeMillis() - MacroApplication.appStartTime
                            Log.i(MacroApplication.TAG, "${adsEnd}ms: [MobileAds] callback received")
                        }
                    }

                delay(1000)
                splashScreenVisible.value = false
                val totalTime = System.currentTimeMillis() - MacroApplication.appStartTime
                Log.i(MacroApplication.TAG, "${totalTime}ms: [Activity] Splash screen hidden. Total startup: ${totalTime}ms")
                Toast.makeText(this@MainActivity, "Total Startup: $totalTime ms", Toast.LENGTH_LONG).show()
            }

            // Dismiss system splash immediately to show our Compose animation
            splashScreen.setKeepOnScreenCondition { false }

            Crossfade(
                targetState = splashScreenVisible.value,
                animationSpec = tween(300),
                label = "splashTransition"
            ) { isVisible ->
                if (isVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(Res.drawable.macropadIcon512),
                                contentDescription = "App Icon",
                                modifier = Modifier.size(128.dp)
                            )
                            Spacer(modifier = Modifier.height(48.dp))
                            LoadingIndicator()
                        }
                    }
                } else {
                    MainContent()
                }
            }
        }
    }

    @Composable
    private fun MainContent() {
        val theme by settingsViewModel.theme.collectAsState()
        var showSettings by remember { mutableStateOf(false) }
        var showAd by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(800) // Delay ad loading to prevent hitch during transition
            showAd = true
        }

        BackHandler(enabled = showSettings) {
            showSettings = false
        }

        AppTheme(useDarkTheme = theme == SettingsAppTheme.DarkBlue) {
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
                            }
                        }
                    )
                },
                bottomBar = { 
                    if (showAd) {
                        BottomAppBar { AdmobBanner() }
                    }
                }
            ) { innerPadding ->
                if (showSettings) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        modifier = Modifier.padding(innerPadding)
                        // No specific settings needed here, the default empty lambda is used
                    )
                } else {
                    val foundServers by clientDiscovery.foundServers.collectAsState()
                    val isScanning by clientDiscovery.isScanning.collectAsState()
                    val isGlobalLoading by settingsViewModel.isGlobalLoading.collectAsState()
                    
                    if (isGlobalLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LoadingIndicator()
                        }
                    } else {
                        // The `App` composable does not need to be aware of the `DiscoveredServer` type.
                        // We map it to a simpler `ServerInfo` type that the UI can use.
                        val serverInfos = remember(foundServers) {
                            foundServers.map {
                                ServerInfo(
                                    name = it.name,
                                    address = it.address,
                                    isSecure = it.isSecure
                                )
                            }
                        }
                        App(
                            modifier = Modifier.padding(innerPadding),
                            scanServers = {
                                clientDiscovery.foundServers.value = emptyList()
                                clientDiscovery.start()
                            },
                            stopScanning = {
                                clientDiscovery.stop()
                            },
                            foundServers = serverInfos,
                            isScanning = isScanning,
                            onConnectClick = { serverInfo, deviceName ->
                                Log.d(
                                    TAG,
                                    "Launching ClientActivity for: ${serverInfo.address} with device name: $deviceName (Secure: ${serverInfo.isSecure})"
                                )
                                val intent = Intent(this, ClientActivity::class.java).apply {
                                    putExtra("SERVER_ADDRESS", serverInfo.address)
                                    putExtra("DEVICE_NAME", deviceName)
                                    putExtra("IS_SECURE", serverInfo.isSecure)
                                }
                                startActivity(intent)
                            }
                        )
                    }
                }
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