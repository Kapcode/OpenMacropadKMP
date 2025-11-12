package com.kapcode.open.macropad.kmps

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class DiscoveredServer(
    val name: String,
    val address: String, // e.g., "192.168.1.10:8443"
    val host: InetAddress,
    val isSecure: Boolean
)

class ClientDiscovery {

    private val discoveryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var discoveryJob: Job? = null
    private var socket: DatagramSocket? = null

    val foundServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())

    fun isDiscovering(): Boolean {
        return discoveryJob?.isActive == true
    }

    fun start() {
        if (isDiscovering()) return
        
        socket = DatagramSocket(null).apply {
            reuseAddress = true
            bind(java.net.InetSocketAddress(9998)) // Listen on the well-known discovery port
        }

        discoveryJob = discoveryScope.launch {
            val buffer = ByteArray(1024)
            val packet = DatagramPacket(buffer, buffer.size)

            while (isActive) {
                try {
                    socket?.receive(packet)
                    val jsonString = String(packet.data, 0, packet.length)
                    val json = JSONObject(jsonString)

                    val serverName = json.getString("serverName")
                    val port = json.getInt("port")
                    val isSecure = json.getBoolean("isSecure")
                    val hostAddress = packet.address
                    val serverAddress = "${hostAddress.hostAddress}:$port"

                    val newServer = DiscoveredServer(serverName, serverAddress, hostAddress, isSecure)

                    // Update the list of found servers
                    val currentServers = foundServers.value.toMutableList()
                    val existingServer = currentServers.find { it.host == newServer.host }
                    if (existingServer == null) {
                        currentServers.add(newServer)
                        foundServers.value = currentServers
                    } else if (existingServer != newServer) {
                        // Replace existing server if details have changed
                        val index = currentServers.indexOf(existingServer)
                        currentServers[index] = newServer
                        foundServers.value = currentServers
                    }
                } catch (e: Exception) {
                    if (isActive) { // Don't log errors on a planned shutdown
                        println("Error receiving discovery packet: ${e.message}")
                    }
                }
            }
        }
    }

    fun stop() {
        discoveryJob?.cancel()
        socket?.close()
        socket = null
        discoveryJob = null
    }
}