package logic

import switchdektoptocompose.logic.ServerUpdater
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerUpdaterTest {
    @Test
    fun testHashCalculation() {
        val tempFile = File.createTempFile("test", ".txt")
        tempFile.writeText("Hello MacroKap")
        
        // echo -n "Hello MacroKap" | sha256sum
        // 21c969cdd537a460d5b80312589448982e3559fb8e9938ff041121a655588266
        val expectedHash = "21c969cdd537a460d5b80312589448982e3559fb8e9938ff041121a655588266"
        
        val actualHash = ServerUpdater.calculateHash(tempFile)
        assertEquals(expectedHash, actualHash)
        
        assertTrue(ServerUpdater.verifyHash(tempFile, expectedHash))
        
        tempFile.delete()
    }
}
