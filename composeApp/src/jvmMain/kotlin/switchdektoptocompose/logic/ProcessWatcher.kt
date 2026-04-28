package switchdektoptocompose.logic

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import switchdektoptocompose.model.ActiveProcessInfo
import java.io.BufferedReader
import java.io.InputStreamReader

class ProcessWatcher(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val getPollingRate: () -> Long = { 250L }
) {
    private val _activeProcess = MutableStateFlow<String?>(null)
    val activeProcess = _activeProcess.asStateFlow()

    private val _focusHistory = MutableStateFlow<List<ActiveProcessInfo>>(emptyList())
    val focusHistory = _focusHistory.asStateFlow()

    private val _lastProcess = MutableStateFlow<String?>(null)
    val lastProcess = _lastProcess.asStateFlow()

    private val _activeTitle = MutableStateFlow<String?>(null)
    val activeTitle = _activeTitle.asStateFlow()

    private val _lastTitle = MutableStateFlow<String?>(null)
    val lastTitle = _lastTitle.asStateFlow()

    private var watchJob: Job? = null

    fun startWatching() {
        if (watchJob != null) return
        watchJob = scope.launch {
            while (isActive) {
                val startTime = System.currentTimeMillis()
                val info = getActiveProcessInfo()
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed > 200) {
                    println("ProcessWatcher: Warning! Polling took ${elapsed}ms (Shell overhead)")
                }

                if (info != null && (info.name != _activeProcess.value || info.windowTitle != _activeTitle.value)) {
                    if (info.name != _activeProcess.value) {
                        _lastProcess.value = _activeProcess.value
                        _activeProcess.value = info.name
                    }
                    
                    if (info.windowTitle != _activeTitle.value) {
                        _lastTitle.value = _activeTitle.value
                        _activeTitle.value = info.windowTitle
                    }
                    
                    updateHistory(info)
                }
                delay(getPollingRate())
            }
        }
    }

    private fun updateHistory(info: ActiveProcessInfo) {
        val currentHistory = _focusHistory.value.toMutableList()
        // Remove if already exists (to move to top)
        currentHistory.removeAll { it.name == info.name && it.id == info.id }
        // Add to top
        currentHistory.add(0, info)
        // Limit size
        if (currentHistory.size > 20) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        _focusHistory.value = currentHistory
    }

    fun stopWatching() {
        watchJob?.cancel()
        watchJob = null
    }

    fun getActiveProcessInfo(): ActiveProcessInfo? {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> getWindowsActiveProcessInfo()
            os.contains("nix") || os.contains("nux") -> getLinuxActiveProcessInfo()
            os.contains("mac") -> getMacActiveProcessInfo()
            else -> null
        }
    }

    private fun getWindowsActiveProcessInfo(): ActiveProcessInfo? {
        return try {
            val script = """
                Add-Type -MemberDefinition '[DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();' -Name "Win32GetForegroundWindow" -Namespace Win32Functions -PassThru
                ${'$'}hwnd = [Win32Functions.Win32GetForegroundWindow]::GetForegroundWindow()
                Get-Process | Where-Object { ${'$'}_.MainWindowHandle -eq ${'$'}hwnd } | ForEach-Object {
                    "${'$'}(${'$'}_.ProcessName).exe[SEP]${'$'}(${'$'}_.Id)[SEP]${'$'}(${'$'}_.MainWindowHandle)[SEP]${'$'}(${'$'}_.CommandLine)[SEP]${'$'}(${'$'}_.MainWindowTitle)"
                }
            """.trimIndent()
            
            val process = ProcessBuilder("powershell.exe", "-Command", script).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()?.trim()
            if (line.isNullOrBlank()) null else {
                val parts = line.split("[SEP]")
                if (parts.size >= 5) {
                    ActiveProcessInfo(
                        name = parts[0],
                        id = parts[2], // Window ID as primary ID
                        pid = parts[1],
                        windowId = parts[2],
                        command = parts[3].ifBlank { "N/A" },
                        windowTitle = parts[4].ifBlank { "N/A" }
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getLinuxActiveProcessInfo(): ActiveProcessInfo? {
        return try {
            // Use xprop for a more standard X11 approach to get the active window ID
            val xpropProcess = ProcessBuilder("xprop", "-root", "_NET_ACTIVE_WINDOW").start()
            val xpropOutput = BufferedReader(InputStreamReader(xpropProcess.inputStream)).readLine()?.trim()
            val windowId = xpropOutput?.split(" ")?.lastOrNull()?.trim()

            if (windowId != null && windowId != "0x0") {
                val pidProcess = ProcessBuilder("xprop", "-id", windowId, "_NET_STARTUP_ID", "_NET_WM_PID").start()
                val pidOutput = BufferedReader(InputStreamReader(pidProcess.inputStream)).readText()
                val pid = pidOutput.lines().find { it.contains("_NET_WM_PID") }?.split("=")?.lastOrNull()?.trim() ?: ""
                
                val nameProcess = ProcessBuilder("ps", "-p", pid, "-o", "comm=").start()
                val name = BufferedReader(InputStreamReader(nameProcess.inputStream)).readLine()?.trim() ?: "Unknown"
                
                val commandProcess = ProcessBuilder("ps", "-p", pid, "-o", "args=").start()
                val command = BufferedReader(InputStreamReader(commandProcess.inputStream)).readLine()?.trim() ?: "N/A"
                
                val titleProcess = ProcessBuilder("xprop", "-id", windowId, "_NET_WM_NAME", "WM_NAME").start()
                val titleOutput = BufferedReader(InputStreamReader(titleProcess.inputStream)).readText()
                val title = titleOutput.lines().find { it.contains("_NET_WM_NAME") || it.contains("WM_NAME") }
                    ?.split("=")?.lastOrNull()?.trim()?.removeSurrounding("\"") ?: "N/A"
                
                ActiveProcessInfo(
                    name = name,
                    id = windowId,
                    pid = pid,
                    windowId = windowId,
                    command = command,
                    windowTitle = title
                )
            } else null
        } catch (e: Exception) {
            // Fallback to xdotool if xprop fails or windowId is 0x0
            try {
                val windowIdProcess = ProcessBuilder("xdotool", "getactivewindow").start()
                val windowId = BufferedReader(InputStreamReader(windowIdProcess.inputStream)).readLine()?.trim()
                if (windowId != null) {
                    val pidProcess = ProcessBuilder("xdotool", "getwindowpid", windowId).start()
                    val pid = BufferedReader(InputStreamReader(pidProcess.inputStream)).readLine()?.trim() ?: ""
                    
                    val nameProcess = ProcessBuilder("ps", "-p", pid, "-o", "comm=").start()
                    val name = BufferedReader(InputStreamReader(nameProcess.inputStream)).readLine()?.trim() ?: "Unknown"
                    
                    val titleProcess = ProcessBuilder("xdotool", "getwindowname", windowId).start()
                    val title = BufferedReader(InputStreamReader(titleProcess.inputStream)).readLine()?.trim() ?: "N/A"
                    
                    ActiveProcessInfo(
                        name = name,
                        id = windowId,
                        pid = pid,
                        windowId = windowId,
                        command = "N/A",
                        windowTitle = title
                    )
                } else null
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun getMacActiveProcessInfo(): ActiveProcessInfo? {
        return try {
            // Using AppleScript to get the name and id of the frontmost application
            val script = """
                tell application "System Events"
                    set frontProcess to first application process whose frontmost is true
                    set procName to name of frontProcess
                    set procId to id of frontProcess
                    try
                        set winTitle to name of window 1 of frontProcess
                    on error
                        set winTitle to "N/A"
                    end try
                    return procName & "[SEP]" & procId & "[SEP]" & winTitle
                end tell
            """.trimIndent()
            val process = ProcessBuilder("osascript", "-e", script).start()
            val line = BufferedReader(InputStreamReader(process.inputStream)).readLine()?.trim()
            if (line != null) {
                val parts = line.split("[SEP]")
                if (parts.size >= 3) {
                    val name = parts[0]
                    val pid = parts[1]
                    val title = parts[2]
                    
                    val commandProcess = ProcessBuilder("ps", "-p", pid, "-o", "command=").start()
                    val command = BufferedReader(InputStreamReader(commandProcess.inputStream)).readLine()?.trim() ?: "N/A"
                    
                    ActiveProcessInfo(
                        name = name,
                        id = pid, // On Mac, PID is often used as identifier
                        pid = pid,
                        windowId = "N/A", // Mac window IDs are trickier to get via AppleScript directly in one go
                        command = command,
                        windowTitle = title
                    )
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
