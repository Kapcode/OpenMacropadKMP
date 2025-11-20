package switchdektoptocompose

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

data class MacroFileState(
    val id: String,
    val file: File?,
    val name: String,
    val content: String,
    val isActive: Boolean = false,
    val isSelectedForDeletion: Boolean = false,
    val allowedClients: String = ""
)

class MacroManagerViewModel(
    private val settingsViewModel: SettingsViewModel,
    private val consoleViewModel: ConsoleViewModel,
    var onEditMacroRequested: (MacroFileState) -> Unit,
    private val onMacrosUpdated: () -> Unit
) {

    // --- CORRECTED SAMPLE MACRO CONTENT ---
    private val sampleMacroContent = """
    {
        "trigger": {
            "keyName": "ESCAPE",
            "allowedClients": "",
            "action": "RELEASE",
            "type": "key"
        },
        "events": [
            {
                "keyName": "WINDOWS",
                "action": "PRESS",
                "type": "key"
            },
            {
                "keyName": "WINDOWS",
                "action": "RELEASE",
                "type": "key"
            }
        ]
    }
    """.trimIndent()

    private val _macroFiles = MutableStateFlow<List<MacroFileState>>(emptyList())
    val macroFiles: StateFlow<List<MacroFileState>> = _macroFiles.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _filePendingDeletion = MutableStateFlow<File?>(null)
    val filePendingDeletion: StateFlow<File?> = _filePendingDeletion.asStateFlow()

    private val _filesPendingDeletion = MutableStateFlow<List<File>?>(null)
    val filesPendingDeletion: StateFlow<List<File>?> = _filesPendingDeletion.asStateFlow()

    private val _macroBeingRenamed = MutableStateFlow<MacroFileState?>(null)
    val macroBeingRenamed: StateFlow<MacroFileState?> = _macroBeingRenamed.asStateFlow()

    private val playbackJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(Dispatchers.IO + playbackJob)
    private val executionMutex = Mutex()

    private val activeMacrosFile = File(System.getProperty("user.home"), ".open-macropad-active-macros.properties")
    private val activeMacrosProps = Properties()

    init {
        loadActiveMacros()
        viewModelScope.launch {
            settingsViewModel.macroDirectory.collect { directoryPath ->
                loadMacrosFromDisk(directoryPath)
            }
        }
    }

    private fun loadActiveMacros() {
        if (activeMacrosFile.exists()) {
            FileInputStream(activeMacrosFile).use { activeMacrosProps.load(it) }
        }
    }

    private fun saveActiveMacros() {
        FileOutputStream(activeMacrosFile).use { activeMacrosProps.store(it, "Active Macros") }
    }

    private fun loadMacrosFromDisk(directoryPath: String) {
        val macroDir = File(directoryPath)
        val fileMacros = if (!macroDir.exists() || !macroDir.isDirectory) {
            emptyList()
        } else {
            macroDir.listFiles { _, name -> name.endsWith(".json", ignoreCase = true) }
                ?.mapNotNull { file ->
                    try {
                        val content = file.readText()
                        val trigger = JSONObject(content).optJSONObject("trigger")
                        val allowedClients = trigger?.optString("allowedClients", "") ?: ""
                        val isActive = activeMacrosProps.getProperty(file.absolutePath, "false").toBoolean()
                        MacroFileState(
                            id = file.absolutePath,
                            file = file,
                            name = file.nameWithoutExtension,
                            content = content,
                            isActive = isActive,
                            allowedClients = allowedClients
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }?.sortedBy { it.name } ?: emptyList()
        }

        val sampleTrigger = JSONObject(sampleMacroContent).optJSONObject("trigger")
        val sampleAllowedClients = sampleTrigger?.optString("allowedClients", "") ?: ""
        val sampleMacroIsActive = activeMacrosProps.getProperty("__SAMPLE_MACRO__", "false").toBoolean()
        val sampleMacro = MacroFileState(
            id = "__SAMPLE_MACRO__",
            file = null,
            name = "Sample Macro",
            content = sampleMacroContent,
            isActive = sampleMacroIsActive,
            allowedClients = sampleAllowedClients
        )

        _macroFiles.value = listOf(sampleMacro) + fileMacros
        onMacrosUpdated()
    }


    fun getActiveMacrosForClient(clientName: String): List<MacroFileState> {
        return _macroFiles.value.filter { macro ->
            macro.isActive && (
                macro.allowedClients.isBlank() ||
                macro.allowedClients.split(',')
                    .map { it.trim() }
                    .any { it.equals(clientName, ignoreCase = true) }
            )
        }
    }

    fun onPlayMacro(macro: MacroFileState) {
        if (!macro.isActive) return
        
        viewModelScope.launch {
            if (executionMutex.tryLock()) {
                try {
                    val logStart = ">>> MACRO STARTING: ${macro.name}"
                    println(logStart)
                    consoleViewModel.addLog(LogLevel.Info, logStart)
                    val startTime = System.currentTimeMillis()
                    val content = macro.file?.readText() ?: macro.content
                    val events = parseEventsFromJson(content)
                    MacroPlayer().play(events)
                    val duration = System.currentTimeMillis() - startTime
                    val logFinish = "<<< MACRO FINISHED: ${macro.name} (Duration: ${duration}ms)"
                    println(logFinish)
                    consoleViewModel.addLog(LogLevel.Info, logFinish)
                } catch (e: CancellationException) {
                    val logCancel = "!!! MACRO CANCELLED: ${macro.name}"
                    println(logCancel)
                    consoleViewModel.addLog(LogLevel.Warn, logCancel)
                    throw e
                } catch (e: Exception) {
                    val logError = "!!! MACRO ERROR: ${macro.name} - ${e.message}"
                    println(logError)
                    consoleViewModel.addLog(LogLevel.Error, logError)
                    e.printStackTrace()
                } finally {
                    executionMutex.unlock()
                }
            } else {
                val msg = "Macro '${macro.name}' dropped: Another macro is currently running."
                println(msg)
                consoleViewModel.addLog(LogLevel.Warn, msg) 
            }
        }
    }
    
    fun startRecording(recordMacroViewModel: RecordMacroViewModel) {
        consoleViewModel.addLog(LogLevel.Info, "Starting macro recording...")
        val recorder = MacroRecorder(recordMacroViewModel) { recordedJson ->
            viewModelScope.launch {
                val macroName = recordMacroViewModel.macroName.value
                val filename = macroName.replace(Regex("[^a-zA-Z0-9_]"), "") + ".json"
                val file = File(settingsViewModel.macroDirectory.value, filename)
                
                // Write the file
                file.writeText(recordedJson)
                
                // Refresh the macro list
                refresh()
                
                // Find the new macro state and open it in the editor
                _macroFiles.value.find { it.file == file }?.let { newMacroState ->
                     onEditMacroRequested(newMacroState)
                }
                
                consoleViewModel.addLog(LogLevel.Info, "Macro '$macroName' saved to $filename")
            }
        }
        recorder.start()
    }
    
    fun cancelAllMacros() {
        playbackJob.cancelChildren()
        val msg = "All running macros have been cancelled."
        println(msg)
        consoleViewModel.addLog(LogLevel.Warn, msg)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MacroPlayer().emergencyReleaseAll()
                consoleViewModel.addLog(LogLevel.Verbose, "Emergency release executed for modifier keys and mouse buttons.")
            } catch(e: Exception) {
                consoleViewModel.addLog(LogLevel.Error, "Error during emergency release: ${e.message}")
            }
        }
    }

    private fun parseEventsFromJson(jsonContent: String): List<MacroEventState> {
        val events = mutableListOf<MacroEventState>()
        try {
            val json = JSONObject(jsonContent)
            json.optJSONArray("events")?.let { eventsArray ->
                for (i in 0 until eventsArray.length()) {
                    eventsArray.getJSONObject(i)?.let { eventObj ->
                        when (eventObj.getString("type").lowercase()) {
                            "key" -> events.add(MacroEventState.KeyEvent(eventObj.getString("keyName"), KeyAction.valueOf(eventObj.getString("action").uppercase())))
                            "mouse" -> events.add(MacroEventState.MouseEvent(
                                eventObj.optInt("x", 0), 
                                eventObj.optInt("y", 0), 
                                MouseAction.valueOf(eventObj.getString("action").uppercase()),
                                eventObj.optBoolean("isAnimated", false)
                            ))
                            "mousebutton" -> events.add(MacroEventState.MouseButtonEvent(eventObj.getInt("buttonNumber"), KeyAction.valueOf(eventObj.getString("action").uppercase())))
                            "scroll" -> events.add(MacroEventState.ScrollEvent(eventObj.getString("scrollAmount").replace("+", "").toInt()))
                            "delay" -> events.add(MacroEventState.DelayEvent(eventObj.getLong("durationMs")))
                            "set_auto_wait" -> events.add(MacroEventState.SetAutoWaitEvent(eventObj.getInt("value")))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return events
    }

    fun refresh() {
        loadMacrosFromDisk(settingsViewModel.macroDirectory.value)
    }
    
    fun onDeleteMacro(macro: MacroFileState) {
        if (macro.file == null) return
        _filePendingDeletion.value = macro.file
    }

    fun confirmDeletion() {
        _filePendingDeletion.value?.let { file ->
            file.delete()
            refresh()
        }
        _filePendingDeletion.value = null
    }

    fun cancelDeletion() {
        _filePendingDeletion.value = null
    }

    fun deleteSelectedMacros() {
        val filesToDelete = _macroFiles.value.filter { it.isSelectedForDeletion && it.file != null }.map { it.file!! }
        if (filesToDelete.isEmpty()) return
        _filesPendingDeletion.value = filesToDelete
    }

    fun confirmMultipleDeletion() {
        _filesPendingDeletion.value?.forEach {
            it.delete()
        }
        _filesPendingDeletion.value = null
        refresh()
        _isSelectionMode.value = false
    }

    fun cancelMultipleDeletion() {
        _filesPendingDeletion.value = null
    }

    fun onEditMacro(macro: MacroFileState) {
        onEditMacroRequested(macro)
    }

    fun onRenameMacro(macro: MacroFileState) {
        _macroBeingRenamed.value = macro
    }

    fun confirmRename(newName: String) {
        _macroBeingRenamed.value?.file?.let { file ->
            val newFile = File(file.parent, "$newName.json")
            if (file.renameTo(newFile)) {
                refresh()
            }
        }
        _macroBeingRenamed.value = null
    }

    fun cancelRename() {
        _macroBeingRenamed.value = null
    }

    fun toggleSelectionMode() {
        _isSelectionMode.update { !it }
        if (!_isSelectionMode.value) {
            _macroFiles.update { list -> list.map { it.copy(isSelectedForDeletion = false) } }
        }
    }

    fun selectMacroForDeletion(macroId: String, select: Boolean) {
        _macroFiles.update { list ->
            list.map {
                if (it.id == macroId) it.copy(isSelectedForDeletion = select) else it
            }
        }
    }

    fun onToggleMacroActive(macroId: String, isActive: Boolean) {
        _macroFiles.update { list ->
            list.map {
                if (it.id == macroId) it.copy(isActive = isActive) else it
            }
        }
        activeMacrosProps.setProperty(macroId, isActive.toString())
        saveActiveMacros()
    }
}