package com.kapcode.open.macropad.kmps

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kapcode.open.macropad.kmps.settings.AppTheme as SettingsAppTheme
import com.kapcode.open.macropad.kmps.settings.SettingsScreen
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel
import com.kapcode.open.macropad.kmps.ui.components.CommonAppBar
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme

const val TAG = "MainActivity"

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private lateinit var clientDiscovery: ClientDiscovery
    private val settingsViewModel = SettingsViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        clientDiscovery = ClientDiscovery()

        setContent {
            val theme by settingsViewModel.theme.collectAsState()
            var showSettings by remember { mutableStateOf(false) }

            // Handle the system back button
            BackHandler(enabled = showSettings) {
                showSettings = false
            }

            AppTheme(useDarkTheme = theme == SettingsAppTheme.DarkBlue) {
                Scaffold(
                    topBar = {
                        CommonAppBar(
                            title = if (showSettings) "Settings" else "Open Macropad",
                            onSettingsClick = {
                                showSettings = !showSettings
                            },
                            onBackClick = if (showSettings) { { showSettings = false } } else null
                        )
                    }
                ) { innerPadding ->
                    if (showSettings) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            modifier = Modifier.padding(innerPadding) // Apply the padding here
                        )
                    } else {
                        val foundServers by clientDiscovery.foundServers.collectAsState()
                        val serverInfos = remember(foundServers) {
                            foundServers.map {
                                ServerInfo(
                                    name = it.name,
                                    address = it.address,
                                    isSecure = true
                                )
                            }
                        }
                        App(
                            modifier = Modifier.padding(innerPadding),
                            scanServers = {
                                clientDiscovery.foundServers.value = emptyList()
                                clientDiscovery.start()
                            },
                            foundServers = serverInfos,
                            onConnectClick = { serverInfo, deviceName ->
                                Log.d(TAG, "Launching ClientActivity for: ${serverInfo.address} with device name: $deviceName (Secure: ${serverInfo.isSecure})")
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
        if (!clientDiscovery.isDiscovering()) {
            clientDiscovery.start()
        }
    }

    override fun onPause() {
        super.onPause()
        clientDiscovery.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        clientDiscovery.stop()
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
            foundServers = sampleServers,
            onConnectClick = { _, _ -> }
        )
    }
}