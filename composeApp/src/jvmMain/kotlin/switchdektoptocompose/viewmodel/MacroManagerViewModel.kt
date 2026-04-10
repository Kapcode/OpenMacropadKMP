package switchdektoptocompose.viewmodel

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import org.json.JSONObject
import switchdektoptocompose.logic.*
import switchdektoptocompose.model.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

data class MacroManagerState(
    val macroFiles: List<MacroFileState> = emptyList(),
    val isSelectionMode: Boolean = false,
    val filePendingDeletion: File? = null,
    val filesPendingDeletion: List<File>? = null,
    val macroBeingRenamed: MacroFileState? = null
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

    private val _uiState = MutableStateFlow(MacroManagerState())
    val uiState: StateFlow<MacroManagerState> = _uiState.asStateFlow()

    val macroFiles: StateFlow<List<MacroFileState>> = _uiState.map { it.macroFiles }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, emptyList())

    val isSelectionMode: StateFlow<Boolean> = _uiState.map { it.isSelectionMode }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, false)

    val filePendingDeletion: StateFlow<File?> = _uiState.map { it.filePendingDeletion }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, null)

    val filesPendingDeletion: StateFlow<List<File>?> = _uiState.map { it.filesPendingDeletion }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, null)

    val macroBeingRenamed: StateFlow<MacroFileState?> = _uiState.map { it.macroBeingRenamed }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, null)

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

        _uiState.update { it.copy(macroFiles = listOf(sampleMacro) + fileMacros) }
        onMacrosUpdated()
    }


    fun getActiveMacrosForClient(clientName: String): List<MacroFileState> {
        return _uiState.value.macroFiles.filter { macro ->
            macro.isActive && (
                macro.allowedClients.isBlank() ||
                macro.allowedClients.split(',')
                    .map { it.trim() }
                    .any { it.equals(clientName, ignoreCase = true) }
            )
        }
    }

    fun onPlayMacro(macro: MacroFileState, onStart: (() -> Unit)? = null, onComplete: (() -> Unit)? = null, onFailure: ((String) -> Unit)? = null) {
        if (!macro.isActive) {
            onFailure?.invoke("Macro is not active.")
            return
        }
        
        viewModelScope.launch {
            if (executionMutex.tryLock()) {
                try {
                    onStart?.invoke()
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
                    onComplete?.invoke()
                } catch (e: CancellationException) {
                    val logCancel = "!!! MACRO CANCELLED: ${macro.name}"
                    println(logCancel)
                    consoleViewModel.addLog(LogLevel.Warn, logCancel)
                    onFailure?.invoke("Macro cancelled.")
                    throw e
                } catch (e: Exception) {
                    val logError = "!!! MACRO ERROR: ${macro.name} - ${e.message}"
                    println(logError)
                    consoleViewModel.addLog(LogLevel.Error, logError)
                    e.printStackTrace()
                    onFailure?.invoke(e.message ?: "Unknown error.")
                } finally {
                    executionMutex.unlock()
                }
            } else {
                val msg = "Macro '${macro.name}' dropped: Another macro is currently running."
                println(msg)
                consoleViewModel.addLog(LogLevel.Warn, msg) 
                onFailure?.invoke("Another macro is currently running.")
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
                _uiState.value.macroFiles.find { it.file == file }?.let { newMacroState ->
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
        _uiState.update { it.copy(filePendingDeletion = macro.file) }
    }

    fun confirmDeletion() {
        _uiState.value.filePendingDeletion?.let { file ->
            file.delete()
            refresh()
        }
        _uiState.update { it.copy(filePendingDeletion = null) }
    }

    fun cancelDeletion() {
        _uiState.update { it.copy(filePendingDeletion = null) }
    }

    fun deleteSelectedMacros() {
        val filesToDelete = _uiState.value.macroFiles.filter { it.isSelectedForDeletion && it.file != null }.map { it.file!! }
        if (filesToDelete.isEmpty()) return
        _uiState.update { it.copy(filesPendingDeletion = filesToDelete) }
    }

    fun confirmMultipleDeletion() {
        _uiState.value.filesPendingDeletion?.forEach {
            it.delete()
        }
        _uiState.update { it.copy(filesPendingDeletion = null, isSelectionMode = false) }
        refresh()
    }

    fun cancelMultipleDeletion() {
        _uiState.update { it.copy(filesPendingDeletion = null) }
    }

    fun onEditMacro(macro: MacroFileState) {
        onEditMacroRequested(macro)
    }

    fun onRenameMacro(macro: MacroFileState) {
        _uiState.update { it.copy(macroBeingRenamed = macro) }
    }

    fun confirmRename(newName: String) {
        _uiState.value.macroBeingRenamed?.file?.let { file ->
            val newFile = File(file.parent, "$newName.json")
            if (file.renameTo(newFile)) {
                refresh()
            }
        }
        _uiState.update { it.copy(macroBeingRenamed = null) }
    }

    fun cancelRename() {
        _uiState.update { it.copy(macroBeingRenamed = null) }
    }

    fun toggleSelectionMode() {
        _uiState.update { state -> 
            val newSelectionMode = !state.isSelectionMode
            state.copy(
                isSelectionMode = newSelectionMode,
                macroFiles = if (!newSelectionMode) state.macroFiles.map { it.copy(isSelectedForDeletion = false) } else state.macroFiles
            )
        }
    }

    fun selectMacroForDeletion(macroId: String, select: Boolean) {
        _uiState.update { state ->
            state.copy(
                macroFiles = state.macroFiles.map {
                    if (it.id == macroId) it.copy(isSelectedForDeletion = select) else it
                }
            )
        }
    }

    fun onToggleMacroActive(macroId: String, isActive: Boolean) {
        _uiState.update { state ->
            state.copy(
                macroFiles = state.macroFiles.map {
                    if (it.id == macroId) it.copy(isActive = isActive) else it
                }
            )
        }
        activeMacrosProps.setProperty(macroId, isActive.toString())
        saveActiveMacros()
    }
}