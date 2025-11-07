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
    val host: InetAddress
)

class ClientDiscovery {

    private val discoveryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var discoveryJob: Job? = null
    private val socket = DatagramSocket(null).apply {
        reuseAddress = true
        bind(java.net.InetSocketAddress(9998)) // Listen on the well-known discovery port
    }

    val foundServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())

    fun start() {
        if (discoveryJob?.isActive == true) return

        discoveryJob = discoveryScope.launch {
            val buffer = ByteArray(1024)
            val packet = DatagramPacket(buffer, buffer.size)

            while (isActive) {
                try {
                    socket.receive(packet)
                    val jsonString = String(packet.data, 0, packet.length)
                    val json = JSONObject(jsonString)

                    val serverName = json.getString("serverName")
                    val wssPort = json.getInt("wssPort")
                    val hostAddress = packet.address
                    val serverAddress = "${hostAddress.hostAddress}:$wssPort"

                    val newServer = DiscoveredServer(serverName, serverAddress, hostAddress)

                    // Update the list of found servers
                    val currentServers = foundServers.value.toMutableList()
                    val existingServer = currentServers.find { it.host == newServer.host }
                    if (existingServer == null) {
                        currentServers.add(newServer)
                        foundServers.value = currentServers
                    }
                    // In a real app, you might want to add a timeout to remove servers that are no longer broadcasting
                } catch (e: Exception) {
                    println("Error receiving discovery packet: ${e.message}")
                }
            }
        }
    }

    fun stop() {
        discoveryJob?.cancel()
        socket.close()
    }
}
