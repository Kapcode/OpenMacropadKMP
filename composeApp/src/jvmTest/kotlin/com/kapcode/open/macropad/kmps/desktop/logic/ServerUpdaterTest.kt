package com.kapcode.open.macropad.kmps.desktop.logic

import com.kapcode.open.macropad.kmps.desktop.logic.ServerUpdater
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerUpdaterTest {
    @Test
    fun testHashCalculation() {
        val tempFile = File.createTempFile("test", ".txt")
        tempFile.writeBytes("A".toByteArray())
        
        // echo -n "A" | sha256sum
        // 559aead08264d5795d3909718cdd05abd49572e84fe55590eef31a88a08fdffd
        val expectedHash = "559aead08264d5795d3909718cdd05abd49572e84fe55590eef31a88a08fdffd"
        
        val actualHash = ServerUpdater.calculateHash(tempFile)
        assertEquals(expectedHash, actualHash)
        
        assertTrue(ServerUpdater.verifyHash(tempFile, expectedHash))
        
        tempFile.delete()
    }
}
