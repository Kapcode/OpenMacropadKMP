package com.kapcode.open.macropad.kmps.desktop.logic

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.kapcode.open.macropad.kmps.desktop.model.ActiveProcessInfo
import java.io.BufferedReader
import java.io.InputStreamReader

data class ProcessWatcherState(
    val active: ActiveProcessInfo? = null,
    val last: ActiveProcessInfo? = null,
    val history: List<ActiveProcessInfo> = emptyList()
)

open class ProcessWatcher(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val getPollingRate: () -> Long = { 250L }
) {
    private val _state = MutableStateFlow(ProcessWatcherState())
    val state = _state.asStateFlow()

    // Public flows derived from unified state for backward compatibility
    val activeProcess = _state.map { it.active?.name }.distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)
    val activeProcessName = _state.map { it.active?.processName }.distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)
    val activeTitle = _state.map { it.active?.windowTitle }.distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)
    
    val lastProcess = _state.map { it.last?.name }.distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)
    val lastProcessName = _state.map { it.last?.processName }.distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)
    val lastTitle = _state.map { it.last?.windowTitle }.distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)
    
    val focusHistory = _state.map { it.history }.distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private var watchJob: Job? = null

    open fun startWatching() {
        if (watchJob != null) return
        watchJob = scope.launch {
            while (isActive) {
                val startTime = System.currentTimeMillis()
                val info = getActiveProcessInfo()
                val elapsed = System.currentTimeMillis() - startTime
                
                if (elapsed > 300) {
                    println("ProcessWatcher: Warning! Polling took ${elapsed}ms (Shell overhead)")
                }

                if (info != null) {
                    val active = _state.value.active
                    val isDifferent = info.id != active?.id || 
                                     info.windowTitle != active.windowTitle ||
                                     info.processName != active.processName

                    if (isDifferent) {
                        _state.update { prev ->
                            val newHistory = prev.history.toMutableList()
                            // Remove if already exists (to move to top)
                            newHistory.removeAll { it.name == info.name && it.id == info.id && it.windowTitle == info.windowTitle }
                            newHistory.add(0, info)
                            if (newHistory.size > 20) newHistory.removeAt(newHistory.size - 1)

                            prev.copy(
                                active = info,
                                last = if (info.id != prev.active?.id) prev.active else prev.last,
                                history = newHistory
                            )
                        }
                    }
                }
                delay(getPollingRate())
            }
        }
    }

    open fun stopWatching() {
        watchJob?.cancel()
        watchJob = null
    }

    open fun getActiveProcessInfo(): ActiveProcessInfo? {
        val os = System.getProperty("os.name").lowercase()
        val info = try {
            when {
                os.contains("win") -> getWindowsActiveProcessInfo()
                os.contains("nix") || os.contains("nux") -> getLinuxActiveProcessInfo()
                os.contains("mac") -> getMacActiveProcessInfo()
                else -> null
            }
        } catch (e: Exception) {
            println("ProcessWatcher: Error getting process info: ${e.message}")
            null
        }
        return info?.let { ProcessNormalizer.normalize(it) }
    }

    private fun getWindowsActiveProcessInfo(): ActiveProcessInfo? {
        val script = """
            Add-Type -MemberDefinition '[DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();' -Name "Win32Functions" -Namespace Win32Functions -PassThru | Out-Null
            ${'$'}hwnd = [Win32Functions.Win32Functions]::GetForegroundWindow()
            if (${'$'}hwnd -ne 0) {
                Get-Process | Where-Object { ${'$'}_.MainWindowHandle -eq ${'$'}hwnd } | ForEach-Object {
                    "${'$'}(${'$'}_.Description)[SEP]${'$'}(${'$'}_.ProcessName).exe[SEP]${'$'}(${'$'}_.Id)[SEP]${'$'}(${'$'}_.MainWindowHandle)[SEP]${'$'}(${'$'}_.CommandLine)[SEP]${'$'}(${'$'}_.MainWindowTitle)"
                }
            }
        """.trimIndent()
        
        val process = ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script).start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val line = reader.readLine()?.trim()
        
        if (line.isNullOrBlank()) return null
        
        val parts = line.split("[SEP]")
        if (parts.size >= 6) {
            return ActiveProcessInfo(
                name = parts[0].ifBlank { parts[1].removeSuffix(".exe") },
                processName = parts[1],
                id = parts[3], // Window Handle as ID
                pid = parts[2],
                windowId = parts[3],
                command = parts[4].ifBlank { "N/A" },
                windowTitle = parts[5].ifBlank { "N/A" }
            )
        }
        return null
    }

    private fun getLinuxActiveProcessInfo(): ActiveProcessInfo? {
        // Step 1: Get Active Window ID
        val xpropActive = ProcessBuilder("xprop", "-root", "_NET_ACTIVE_WINDOW").start()
        val activeLine = BufferedReader(InputStreamReader(xpropActive.inputStream)).readLine() ?: return null
        val windowId = activeLine.split(" ").lastOrNull()?.trim() ?: return null
        if (windowId == "0x0") return null

        // Step 2: Get all properties in one go
        val xpropProps = ProcessBuilder("xprop", "-id", windowId, "_NET_WM_PID", "WM_CLASS", "_NET_WM_NAME", "WM_NAME").start()
        val propsOutput = BufferedReader(InputStreamReader(xpropProps.inputStream)).readText()
        
        val lines = propsOutput.lines()
        val pid = lines.find { it.contains("_NET_WM_PID") }?.split("=")?.lastOrNull()?.trim() ?: ""
        
        val wmClassLine = lines.find { it.contains("WM_CLASS") }
        val name = wmClassLine?.split(",")?.lastOrNull()?.trim()?.removeSurrounding("\"") ?: "Unknown"
        
        val title = lines.find { it.contains("_NET_WM_NAME") || it.contains("WM_NAME") }
            ?.split("=")?.lastOrNull()?.trim()?.removeSurrounding("\"") ?: "N/A"

        // Step 3: Get process name from PID
        var processName = "Unknown"
        var command = "N/A"
        if (pid.isNotEmpty()) {
            try {
                processName = BufferedReader(InputStreamReader(ProcessBuilder("ps", "-p", pid, "-o", "comm=").start().inputStream)).readLine()?.trim() ?: "Unknown"
                command = BufferedReader(InputStreamReader(ProcessBuilder("ps", "-p", pid, "-o", "args=").start().inputStream)).readLine()?.trim() ?: "N/A"
            } catch (e: Exception) {}
        }

        return ActiveProcessInfo(
            name = name,
            processName = processName,
            id = windowId,
            pid = pid,
            windowId = windowId,
            command = command,
            windowTitle = title
        )
    }

    private fun getMacActiveProcessInfo(): ActiveProcessInfo? {
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
        val line = BufferedReader(InputStreamReader(process.inputStream)).readLine()?.trim() ?: return null
        
        val parts = line.split("[SEP]")
        if (parts.size >= 3) {
            val name = parts[0]
            val pid = parts[1]
            val title = parts[2]
            
            var command = "N/A"
            var processName = name
            try {
                command = BufferedReader(InputStreamReader(ProcessBuilder("ps", "-p", pid, "-o", "command=").start().inputStream)).readLine()?.trim() ?: "N/A"
                processName = command.split("/").lastOrNull()?.split(" ")?.firstOrNull() ?: name
            } catch (e: Exception) {}

            return ActiveProcessInfo(
                name = name,
                processName = processName,
                id = pid,
                pid = pid,
                windowId = "N/A",
                command = command,
                windowTitle = title
            )
        }
        return null
    }
}
