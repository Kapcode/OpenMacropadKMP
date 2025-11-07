package com.kapcode.open.macropad.kmps

import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Handles the discovery of the server on the local network using UDP broadcasts.
 */
class ServerDiscovery(
    private val serverName: String,
    private val wssPort: Int,
    private val discoveryPort: Int = 9998 // The well-known port for discovery
) {
    private val discoveryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var discoveryJob: Job? = null
    private val socket = DatagramSocket()

    fun start() {
        if (discoveryJob?.isActive == true) return // Already running

        // The message to be broadcasted. A simple JSON format is a good choice.
        val message = "{\"serverName\":\"$serverName\", \"wssPort\":$wssPort}".toByteArray()

        discoveryJob = discoveryScope.launch {
            socket.broadcast = true
            val broadcastAddress = InetAddress.getByName("255.255.255.255")

            while (isActive) {
                try {
                    val packet = DatagramPacket(message, message.size, broadcastAddress, discoveryPort)
                    socket.send(packet)
                    delay(2000) // Broadcast every 2 seconds
                } catch (e: Exception) {
                    // Log or handle the exception
                    println("Discovery broadcast failed: ${e.message}")
                    delay(5000) // Wait longer before retrying on failure
                }
            }
        }
    }

    fun stop() {
        discoveryJob?.cancel()
        socket.close()
    }
}
