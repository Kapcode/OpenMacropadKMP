package com.kapcode.open.macropad.kmps

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kapcode.open.macropad.kmps.ui.components.CommonAppBar

const val TAG = "MainActivity"

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private lateinit var clientDiscovery: ClientDiscovery

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        clientDiscovery = ClientDiscovery()

        setContent {
            val foundServers by clientDiscovery.foundServers.collectAsState()

            // Map DiscoveredServer to the String the App composable expects
            val serverAddresses = remember(foundServers) {
                foundServers.map { it.address }
            }

            Scaffold(
                topBar = {
                    CommonAppBar(
                        title = "Open Macropad",
                        onSettingsClick = {
                            Toast.makeText(this, "Settings Clicked", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            ) { innerPadding ->
                App(
                    modifier = Modifier.padding(innerPadding),
                    scanServers = {
                        clientDiscovery.foundServers.value = emptyList() // Clear previous results
                        clientDiscovery.start()
                    },
                    foundServers = serverAddresses,
                    onConnectClick = { serverAddress, deviceName ->
                        Log.d(TAG, "Launching ClientActivity for: $serverAddress with device name: $deviceName")
                        val intent = Intent(this, ClientActivity::class.java).apply {
                            putExtra("SERVER_ADDRESS", serverAddress)
                            putExtra("DEVICE_NAME", deviceName)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        clientDiscovery.start()
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
    // Preview with some sample data
    val sampleServers = listOf("192.168.1.100:8443", "Desktop-PC:8443")
    App(
        scanServers = {},
        foundServers = sampleServers,
        onConnectClick = { _, _ -> }
    )
}