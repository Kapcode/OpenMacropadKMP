package com.kapcode.open.macropad.kmps.network.sockets.model

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.util.concurrent.ConcurrentHashMap

class SessionEvictionTest {

    data class MockClient(val id: String, var isDisconnected: Boolean = false)

    @Test
    fun testSessionEviction_DuplicateClientId() = runTest {
        val clients = ConcurrentHashMap<String, MockClient>()
        val clientId = "test-device-id"

        // 1. First session connects
        val firstClient = MockClient(clientId)
        clients[clientId] = firstClient
        assertEquals(1, clients.size)
        assertEquals(firstClient, clients[clientId])

        // 2. Second session connects with SAME Client ID
        val secondClient = MockClient(clientId)
        
        // Simulation of the eviction logic in MacroKtorServer.kt
        clients[clientId]?.let { existingClient ->
            existingClient.isDisconnected = true
        }
        clients[clientId] = secondClient

        // 3. Verify the first session is marked as disconnected and the second is active
        assertTrue(firstClient.isDisconnected, "First session should be evicted")
        assertEquals(1, clients.size, "Should still only have one client entry for the ID")
        assertEquals(secondClient, clients[clientId], "Second session should be the active one")
    }

    @Test
    fun testSessionEviction_HighJitterRapidReconnection() = runTest {
        val clients = ConcurrentHashMap<String, MockClient>()
        val clientId = "jitter-device-id"
        val sessionHistory = mutableListOf<MockClient>()

        // Simulate 10 rapid reconnections due to network jitter
        repeat(10) {
            val newClient = MockClient(clientId)
            sessionHistory.add(newClient)
            
            // Eviction logic
            clients[clientId]?.let { it.isDisconnected = true }
            clients[clientId] = newClient
            
            delay(10) // Small delay to simulate rapid firing
        }

        // Verify all but the last session are evicted
        sessionHistory.dropLast(1).forEachIndexed { index, client ->
            assertTrue(client.isDisconnected, "Session $index should be evicted")
        }
        assertFalse(sessionHistory.last().isDisconnected, "Latest session should be active")
        assertEquals(sessionHistory.last(), clients[clientId])
    }
}
