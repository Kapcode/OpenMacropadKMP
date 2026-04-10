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
    val fingerprint: String? = null,
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
    private val malformedPacketCount = mutableMapOf<String, Int>()
    private val temporaryBlacklist = mutableMapOf<String, Long>()
    private var globalLastProcessedTime = 0L

    private companion object {
        const val DISCOVERY_PREFIX = "OMP_DISCOVERY_V1:"
        const val RATE_LIMIT_MS = 2000L
        const val GLOBAL_RATE_LIMIT_MS = 500L
        const val STALE_TIMEOUT_MS = 15000L
        const val BLACKLIST_DURATION_MS = 300000L // 5 minutes
        const val MAX_MALFORMED_ATTEMPTS = 3
        const val MAX_SERVERS = 20
    }

    fun isDiscovering(): Boolean {
        return discoveryJob?.isActive == true
    }

    fun start() {
        if (isDiscovering()) return
        
        _isScanning.value = true
        
        // Cleanup job to remove stale servers and expired blacklist entries
        cleanupJob = discoveryScope.launch {
            while (isActive) {
                delay(5000)
                val now = System.currentTimeMillis()
                
                // 1. Cleanup Stale Servers
                val currentList = foundServers.value
                val filteredList = currentList.filter { now - it.lastSeen < STALE_TIMEOUT_MS }
                if (filteredList.size != currentList.size) {
                    foundServers.value = filteredList
                }

                // 2. Cleanup Expired Blacklist
                val expiredBlacklist = temporaryBlacklist.filter { now >= it.value }.keys
                expiredBlacklist.forEach { 
                    temporaryBlacklist.remove(it)
                    malformedPacketCount.remove(it)
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
                        val now = System.currentTimeMillis()

                        // 1. Global Rate Limit (Protect CPU from packet storms)
                        if (now - globalLastProcessedTime < GLOBAL_RATE_LIMIT_MS) continue
                        globalLastProcessedTime = now

                        val hostAddress = packet.address
                        val hostIp = hostAddress?.hostAddress ?: continue

                        // 2. Blacklist Check
                        val blacklistExpiry = temporaryBlacklist[hostIp]
                        if (blacklistExpiry != null && now < blacklistExpiry) continue

                        val rawData = String(packet.data, 0, packet.length)
                        
                        // 3. Prefix Validation
                        if (!rawData.startsWith(DISCOVERY_PREFIX)) {
                            handleMalformedPacket(hostIp)
                            continue
                        }
                        
                        // 4. Rate Limiting (per IP)
                        val lastSeenTime = lastProcessedFromIp[hostIp] ?: 0L
                        if (now - lastSeenTime < RATE_LIMIT_MS) continue
                        lastProcessedFromIp[hostIp] = now

                        try {
                            val jsonString = rawData.removePrefix(DISCOVERY_PREFIX)
                            val json = JSONObject(jsonString)

                            val serverName = json.getString("serverName")
                            val port = json.getInt("port")
                            val isSecure = json.getBoolean("isSecure")
                            val fingerprint = if (json.has("fingerprint")) json.getString("fingerprint") else null
                            val serverAddress = "$hostIp:$port"

                            val newServer = DiscoveredServer(serverName, serverAddress, hostAddress, isSecure, fingerprint, now)

                            // 5. Update the list of found servers
                            val currentServers = foundServers.value.toMutableList()
                            val existingIndex = currentServers.indexOfFirst { it.host == newServer.host && it.address == newServer.address }
                            
                            if (existingIndex != -1) {
                                currentServers[existingIndex] = newServer
                            } else {
                                // 6. Capacity Limit
                                if (currentServers.size < MAX_SERVERS) {
                                    currentServers.add(newServer)
                                }
                            }
                            foundServers.value = currentServers
                            
                            // Successful packet - Reset malformed count for this IP
                            malformedPacketCount.remove(hostIp)

                        } catch (e: Exception) {
                            handleMalformedPacket(hostIp)
                        }
                        
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

    private fun handleMalformedPacket(ip: String) {
        val count = (malformedPacketCount[ip] ?: 0) + 1
        malformedPacketCount[ip] = count
        if (count >= MAX_MALFORMED_ATTEMPTS) {
            temporaryBlacklist[ip] = System.currentTimeMillis() + BLACKLIST_DURATION_MS
            println("IP $ip blacklisted for 5 minutes due to $count malformed packets.")
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
        malformedPacketCount.clear()
        temporaryBlacklist.clear()
        foundServers.value = emptyList()
    }
}
