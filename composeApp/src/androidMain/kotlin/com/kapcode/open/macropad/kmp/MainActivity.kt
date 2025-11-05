package com.kapcode.open.macropad.kmp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

const val TAG = "MainActivity"
const val SERVER_PORT = 9999
const val TIMEOUT_MS = 500 // Timeout for connection attempts

class MainActivity : ComponentActivity() {

    private val foundServers = mutableStateListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(
                scanServers = { scanForAvailableServers() },
                foundServers = foundServers
            )
        }
    }

    private fun scanForAvailableServers() {
        Log.d(TAG, "Scanning for available servers on port $SERVER_PORT...")
        foundServers.clear() // Clear previous results

        CoroutineScope(Dispatchers.IO).launch {
            val subnet = "192.168.1." // Example subnet, ideally determined dynamically
            val deferredServers = (1..254).map { i ->
                async {
                    val address = subnet + i
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(address, SERVER_PORT), TIMEOUT_MS)
                        socket.close()
                        Log.d(TAG, "Found available server: $address:$SERVER_PORT")
                        "$address:$SERVER_PORT"
                    } catch (e: SocketTimeoutException) {
                        null
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            val newFoundServers = deferredServers.awaitAll().filterNotNull()
            // Update the mutable state list on the main thread
            launch(Dispatchers.Main) {
                foundServers.addAll(newFoundServers)
                if (foundServers.isNotEmpty()) {
                    Log.d(TAG, "Scan complete. Found servers: $foundServers")
                } else {
                    Log.d(TAG, "Scan complete. No servers found.")
                }
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(scanServers = {}, foundServers = mutableStateListOf()) // Provide an empty lambda for preview
}
