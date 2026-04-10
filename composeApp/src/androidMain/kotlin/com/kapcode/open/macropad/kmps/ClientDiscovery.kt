package com.kapcode.open.macropad.kmps

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isSecure: Boolean,
    val lastSeen: Long = System.currentTimeMillis()
)

class ClientDiscovery {

    private val discoveryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var discoveryJob: Job? = null
    private var cleanupJob: Job? = null
    private var socket: DatagramSocket? = null

    val foundServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val lastProcessedFromIp = mutableMapOf<String, Long>()

    private companion object {
        const val DISCOVERY_PREFIX = "OMP_DISCOVERY_V1:"
        const val RATE_LIMIT_MS = 2000L
        const val STALE_TIMEOUT_MS = 15000L
        const val MAX_SERVERS = 20
    }

    fun isDiscovering(): Boolean {
        return discoveryJob?.isActive == true
    }

    fun start() {
        if (isDiscovering()) return
        
        _isScanning.value = true
        
        // Cleanup job to remove stale servers
        cleanupJob = discoveryScope.launch {
            while (isActive) {
                delay(5000)
                val now = System.currentTimeMillis()
                val currentList = foundServers.value
                val filteredList = currentList.filter { now - it.lastSeen < STALE_TIMEOUT_MS }
                if (filteredList.size != currentList.size) {
                    foundServers.value = filteredList
                }
            }
        }

        discoveryJob = discoveryScope.launch {
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(java.net.InetSocketAddress(9998)) // Listen on the well-known discovery port
                }

                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isActive) {
                    try {
                        socket?.receive(packet)
                        val rawData = String(packet.data, 0, packet.length)
                        
                        // 1. Prefix Validation
                        if (!rawData.startsWith(DISCOVERY_PREFIX)) continue
                        
                        val hostAddress = packet.address
                        val hostIp = hostAddress?.hostAddress ?: continue
                        val now = System.currentTimeMillis()

                        // 2. Rate Limiting (per IP)
                        val lastSeenTime = lastProcessedFromIp[hostIp] ?: 0L
                        if (now - lastSeenTime < RATE_LIMIT_MS) continue
                        lastProcessedFromIp[hostIp] = now

                        val jsonString = rawData.removePrefix(DISCOVERY_PREFIX)
                        val json = JSONObject(jsonString)

                        val serverName = json.getString("serverName")
                        val port = json.getInt("port")
                        val isSecure = json.getBoolean("isSecure")
                        val serverAddress = "$hostIp:$port"

                        val newServer = DiscoveredServer(serverName, serverAddress, hostAddress, isSecure, now)

                        // 3. Update the list of found servers
                        val currentServers = foundServers.value.toMutableList()
                        val existingIndex = currentServers.indexOfFirst { it.host == newServer.host && it.address == newServer.address }
                        
                        if (existingIndex != -1) {
                            currentServers[existingIndex] = newServer
                        } else {
                            // 4. Capacity Limit
                            if (currentServers.size < MAX_SERVERS) {
                                currentServers.add(newServer)
                            }
                        }
                        foundServers.value = currentServers
                        
                    } catch (e: Exception) {
                        if (isActive) {
                            println("Error receiving discovery packet: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    println("Failed to setup discovery socket: ${e.message}")
                }
            } finally {
                _isScanning.value = false
                socket?.close()
                socket = null
            }
        }
    }

    fun stop() {
        discoveryJob?.cancel()
        cleanupJob?.cancel()
        _isScanning.value = false
        socket?.close()
        socket = null
        discoveryJob = null
        cleanupJob = null
        lastProcessedFromIp.clear()
        foundServers.value = emptyList()
    }
}
