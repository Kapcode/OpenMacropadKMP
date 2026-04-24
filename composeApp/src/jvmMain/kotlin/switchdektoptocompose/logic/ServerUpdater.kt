package switchdektoptocompose.logic

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.system.exitProcess

object ServerUpdater {
    
    fun calculateHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        file.inputStream().use { input ->
            var bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun verifyHash(file: File, expectedHash: String): Boolean {
        val actualHash = calculateHash(file)
        return actualHash.equals(expectedHash, ignoreCase = true)
    }

    fun applyUpdate(newJar: File, isSimulation: Boolean = false): Result<String> {
        if (isSimulation) {
            return Result.success("Simulation mode: Update verified successfully.")
        }

        return try {
            val currentJar = getCurrentJarPath() ?: return Result.failure(Exception("Could not determine current JAR path"))
            val os = System.getProperty("os.name").lowercase()
            
            if (os.contains("win")) {
                applyWindowsUpdate(currentJar, newJar)
            } else {
                applyUnixUpdate(currentJar, newJar)
            }
            
            Result.success("Update script triggered. Restarting...")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getCurrentJarPath(): File? {
        val path = ServerUpdater::class.java.protectionDomain.codeSource.location.toURI().path
        val file = File(path)
        return if (file.exists() && file.extension == "jar") file else {
            // Fallback for some environments
            val classPath = System.getProperty("java.class.path")
            val firstEntry = classPath.split(File.pathSeparator).firstOrNull()
            if (firstEntry != null) {
                val f = File(firstEntry)
                if (f.exists() && f.extension == "jar") f else null
            } else null
        }
    }

    private fun applyWindowsUpdate(currentJar: File, newJar: File) {
        val batchFile = File.createTempFile("update_macropad", ".bat")
        val pid = ProcessHandle.current().pid()
        
        val script = """
            @echo off
            taskkill /F /PID $pid
            timeout /t 2 /nobreak > nul
            copy /Y "${newJar.absolutePath}" "${currentJar.absolutePath}"
            start "" javaw -jar "${currentJar.absolutePath}"
            del "%~f0"
        """.trimIndent()
        
        batchFile.writeText(script)
        
        ProcessBuilder("cmd.exe", "/c", batchFile.absolutePath).start()
        exitProcess(0)
    }

    private fun applyUnixUpdate(currentJar: File, newJar: File) {
        val scriptFile = File.createTempFile("update_macropad", ".sh")
        val pid = ProcessHandle.current().pid()
        
        val script = """
            #!/bin/bash
            kill -9 $pid
            sleep 2
            cp -f "${newJar.absolutePath}" "${currentJar.absolutePath}"
            nohup java -jar "${currentJar.absolutePath}" > /dev/null 2>&1 &
            rm -- "${'$'}0"
        """.trimIndent()
        
        scriptFile.writeText(script)
        scriptFile.setExecutable(true)
        
        ProcessBuilder("sh", scriptFile.absolutePath).start()
        exitProcess(0)
    }
}
