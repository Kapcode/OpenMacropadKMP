package switchdektoptocompose

import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class ServerDiscoveryAnnouncer {
    private val announcerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var announcerJob: Job? = null
    private var socket: DatagramSocket? = null

    fun start(serverName: String, port: Int, isSecure: Boolean) {
        if (announcerJob?.isActive == true) {
            return
        }
        announcerJob = announcerScope.launch {
            try {
                socket = DatagramSocket()
                socket?.broadcast = true
                val broadcastAddress = InetAddress.getByName("255.255.255.255")

                val message = JSONObject().apply {
                    put("serverName", serverName)
                    put("port", port)
                    put("isSecure", isSecure)
                }.toString().toByteArray()

                val packet = DatagramPacket(message, message.size, broadcastAddress, 9998)

                while (isActive) {
                    try {
                        socket?.send(packet)
                        delay(5000) // Announce every 5 seconds
                    } catch (e: Exception) {
                        if (isActive) {
                            System.err.println("Error sending discovery packet: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                System.err.println("Failed to start discovery announcer: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    fun stop() {
        announcerJob?.cancel()
        socket?.close()
        announcerJob = null
        socket = null
    }
}