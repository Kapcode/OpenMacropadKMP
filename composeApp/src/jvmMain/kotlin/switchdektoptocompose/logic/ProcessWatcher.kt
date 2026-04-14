package switchdektoptocompose.logic

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader

class ProcessWatcher(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val _activeProcess = MutableStateFlow<String?>(null)
    val activeProcess = _activeProcess.asStateFlow()

    private var watchJob: Job? = null

    fun startWatching() {
        if (watchJob != null) return
        watchJob = scope.launch {
            while (isActive) {
                val currentProcess = getActiveProcessName()
                if (currentProcess != _activeProcess.value) {
                    _activeProcess.value = currentProcess
                }
                delay(2000) // Check every 2 seconds
            }
        }
    }

    fun stopWatching() {
        watchJob?.cancel()
        watchJob = null
    }

    private fun getActiveProcessName(): String? {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> getWindowsActiveProcess()
            os.contains("nix") || os.contains("nux") -> getLinuxActiveProcess()
            os.contains("mac") -> getMacActiveProcess()
            else -> null
        }
    }

    private fun getWindowsActiveProcess(): String? {
        return try {
            // Using PowerShell to get the name of the foreground window's process
            val script = "Get-Process | Where-Object { \$_.MainWindowHandle -eq (Add-Type -MemberDefinition '[DllImport(\"user32.dll\")] public static extern IntPtr GetForegroundWindow();' -Name \"Win32GetForegroundWindow\" -Namespace Win32Functions -PassThru)::GetForegroundWindow() } | Select-Object -ExpandProperty ProcessName"
            val process = ProcessBuilder("powershell.exe", "-Command", script).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val name = reader.readLine()?.trim()
            if (name.isNullOrBlank()) null else "$name.exe"
        } catch (e: Exception) {
            null
        }
    }

    private fun getLinuxActiveProcess(): String? {
        return try {
            // Using xdotool to get the active window's PID and then finding the process name
            val windowIdProcess = ProcessBuilder("xdotool", "getactivewindow", "getwindowpid").start()
            val pid = BufferedReader(InputStreamReader(windowIdProcess.inputStream)).readLine()?.trim()
            if (pid != null) {
                val nameProcess = ProcessBuilder("ps", "-p", pid, "-o", "comm=").start()
                BufferedReader(InputStreamReader(nameProcess.inputStream)).readLine()?.trim()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getMacActiveProcess(): String? {
        return try {
            // Using AppleScript to get the name of the frontmost application
            val script = "tell application \"System Events\" to get name of first application process whose frontmost is true"
            val process = ProcessBuilder("osascript", "-e", script).start()
            BufferedReader(InputStreamReader(process.inputStream)).readLine()?.trim()
        } catch (e: Exception) {
            null
        }
    }
}
