@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.kapcode.open.macropad.kmps.network.sockets.model

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.currentTime
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class HeartbeatStressTest {

    @Test
    fun testHeartbeatMechanism() = runTest {
        var lastSeenTime = currentTime
        val timeoutMillis = 15000L
        val heartbeatInterval = 5000L
        var isDisconnected = false

        val serverJob = launch {
            while (isActive) {
                delay(1000)
                if (currentTime - lastSeenTime > timeoutMillis) {
                    isDisconnected = true
                    break
                }
            }
        }

        // Simulate 5 successful heartbeats
        repeat(5) {
            delay(heartbeatInterval)
            lastSeenTime = currentTime
        }

        assertFalse(isDisconnected, "Client should NOT have been disconnected during normal operation")
        serverJob.cancel()
    }

    @Test
    fun testHeartbeatTimeout() = runTest {
        var lastSeenTime = currentTime
        val timeoutMillis = 15000L
        var isDisconnected = false

        val serverJob = launch {
            while (isActive) {
                delay(1000)
                if (currentTime - lastSeenTime > timeoutMillis) {
                    isDisconnected = true
                    break
                }
            }
        }

        // Wait longer than timeout
        delay(timeoutMillis + 2000)
        
        assertTrue(isDisconnected, "Server should have disconnected client after timeout")
        serverJob.cancel()
    }

    @Test
    fun testPoorNetworkConditions_PacketLoss() = runTest {
        var lastSeenTime = currentTime
        val timeoutMillis = 15000L
        val heartbeatInterval = 5000L
        var isDisconnected = false

        val serverJob = launch {
            while (isActive) {
                delay(1000)
                if (currentTime - lastSeenTime > timeoutMillis) {
                    isDisconnected = true
                    break
                }
            }
        }

        // Simulate heartbeats with 50% packet loss
        repeat(10) { i ->
            delay(heartbeatInterval)
            if (i % 2 == 0) { // Send every other heartbeat
                lastSeenTime = currentTime
            }
        }

        // Even with 50% loss, the gap between successful heartbeats is 10s, 
        // which is less than the 15s timeout.
        assertFalse(isDisconnected, "Client should NOT disconnect with 50% packet loss (10s gap < 15s timeout)")
        
        // Now simulate 3 consecutive lost heartbeats (15s gap)
        delay(heartbeatInterval * 3 + 1000)
        assertTrue(isDisconnected, "Client SHOULD disconnect after 3 consecutive lost heartbeats (15s)")
        
        serverJob.cancel()
    }

    @Test
    fun testPoorNetworkConditions_HighLatency() = runTest {
        var lastSeenTime = currentTime
        val timeoutMillis = 15000L
        val heartbeatInterval = 5000L
        var isDisconnected = false

        val serverJob = launch {
            while (isActive) {
                delay(1000)
                if (currentTime - lastSeenTime > timeoutMillis) {
                    isDisconnected = true
                    break
                }
            }
        }

        // Simulate heartbeats with high jitter/latency
        repeat(5) {
            // In runTest, we use fixed delays to simulate latency
            val latency = (2000L..8000L).random()
            delay(latency)
            lastSeenTime = currentTime
            // Reset delay to keep average around heartbeatInterval
            val nextDelay = (heartbeatInterval * 2 - latency).coerceAtLeast(0)
            delay(nextDelay)
        }

        assertFalse(isDisconnected, "Client should handle high latency/jitter as long as it arrives within 15s")
        serverJob.cancel()
    }
}
