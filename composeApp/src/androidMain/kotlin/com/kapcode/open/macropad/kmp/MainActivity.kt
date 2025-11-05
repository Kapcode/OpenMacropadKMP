package com.kapcode.open.macropad.kmp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.SocketTimeoutException

const val TAG = "MainActivity"
const val SERVER_PORT = 9999
const val TIMEOUT_MS = 5000 // Timeout for connection attempts

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Check if connected to Wi-Fi and scan for servers
        // (Implementation for Wi-Fi check and server scanning will be added later)
        // scanForAvailableServers() // No longer called directly on create

        setContent {
            App(scanServers = { scanForAvailableServers() })
        }
    }
}

private fun scanForAvailableServers() {
    Log.d(TAG, "Scanning for available servers on port $SERVER_PORT...")

    // This should ideally check for Wi-Fi connectivity first.
    // For now, we'll proceed with scanning directly.

    CoroutineScope(Dispatchers.IO).launch {
        val subnet = "192.168.1." // Example subnet, ideally determined dynamically
        val deferredServers = (1..254).map { i ->
            async {
                val address = subnet + i
                try {
                    val socket = java.net.Socket()
                    socket.connect(java.net.InetSocketAddress(address, SERVER_PORT), TIMEOUT_MS)
                    socket.close()
                    Log.d(TAG, "Found available server: $address:$SERVER_PORT")
                    "$address:$SERVER_PORT"
                } catch (e: SocketTimeoutException) {
                    Log.d(TAG, "Connection to $address:$SERVER_PORT timed out.")
                    null
                } catch (e: Exception) {
                    Log.e(TAG, "Error connecting to $address:$SERVER_PORT: ${e.message}")
                    null
                }
            }
        }

        val foundServers = deferredServers.awaitAll().filterNotNull()

        if (foundServers.isNotEmpty()) {
            Log.d(TAG, "Scan complete. Found servers: $foundServers")
        } else {
            Log.d(TAG, "Scan complete. No servers found.")
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(scanServers = {}) // Provide an empty lambda for preview
}
