package switchdektoptocompose.viewmodel

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
import org.json.JSONObject
import switchdektoptocompose.logic.*
import switchdektoptocompose.model.*
import com.kapcode.open.macropad.kmps.models.MacroPack
import com.kapcode.open.macropad.kmps.network.sockets.model.toastMessage
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.Toolkit
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties
import java.util.UUID

data class MacroManagerState(
    val macroFiles: List<MacroFileState> = emptyList(),
    val macroPacks: List<MacroPackState> = emptyList(),
    val currentActiveProcess: String? = null,
    val isMacroSelectionMode: Boolean = false,
    val isPackSelectionMode: Boolean = false,
    val selectedPackIds: Set<String> = emptySet(),
    val macroSearchQuery: String = "",
    val packSearchQuery: String = "",
    val isMacrosCollapsed: Boolean = false,
    val isPacksCollapsed: Boolean = false,
    val filePendingDeletion: File? = null,
    val filesPendingDeletion: List<File>? = null,
    val macroBeingRenamed: MacroFileState? = null,
    val packBeingEdited: MacroPack? = null,
    val activeToast: String? = null
)

class MacroManagerViewModel(
    private val settingsViewModel: SettingsViewModel,
    private val consoleViewModel: ConsoleViewModel,
    var serverViewModel: ServerViewModel? = null,
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

    val macroFiles: StateFlow<List<MacroFileState>> = _uiState
        .map { state ->
            state.macroFiles.filter { it.name.contains(state.macroSearchQuery, ignoreCase = true) }
        }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, emptyList())

    val macroPacks: StateFlow<List<MacroPackState>> = _uiState
        .map { state ->
            state.macroPacks
                .filter { it.pack.name.contains(state.packSearchQuery, ignoreCase = true) }
        }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, emptyList())

    val isMacroSelectionMode: StateFlow<Boolean> = _uiState.map { it.isMacroSelectionMode }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, false)

    val isPackSelectionMode: StateFlow<Boolean> = _uiState.map { it.isPackSelectionMode }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, false)

    val macroSearchQuery: StateFlow<String> = _uiState.map { it.macroSearchQuery }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, "")

    val packSearchQuery: StateFlow<String> = _uiState.map { it.packSearchQuery }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, "")

    val isMacrosCollapsed: StateFlow<Boolean> = _uiState.map { it.isMacrosCollapsed }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, false)

    val isPacksCollapsed: StateFlow<Boolean> = _uiState.map { it.isPacksCollapsed }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, false)

    val currentActiveProcess: StateFlow<String?> = _uiState.map { it.currentActiveProcess }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, null)

    val filePendingDeletion: StateFlow<File?> = _uiState.map { it.filePendingDeletion }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, null)

    val filesPendingDeletion: StateFlow<List<File>?> = _uiState.map { it.filesPendingDeletion }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, null)

    val macroBeingRenamed: StateFlow<MacroFileState?> = _uiState.map { it.macroBeingRenamed }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, null)

    val packBeingEdited: StateFlow<MacroPack?> = _uiState.map { it.packBeingEdited }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, null)

    val activeToast: StateFlow<String?> = _uiState.map { it.activeToast }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, null)

    private val playbackJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(Dispatchers.IO + playbackJob)
    private val executionMutex = Mutex()

    private val json = Json { ignoreUnknownKeys = true }

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
        val allFiles = if (!macroDir.exists() || !macroDir.isDirectory) {
            emptyList()
        } else {
            macroDir.listFiles { _, name -> name.endsWith(".json", ignoreCase = true) }?.toList() ?: emptyList()
        }

        val fileMacros = allFiles.filter { !it.name.endsWith("_pack.json", ignoreCase = true) }
            .mapNotNull { file ->
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
            }.sortedBy { it.name }

        val filePacks = allFiles.filter { it.name.endsWith("_pack.json", ignoreCase = true) }
            .mapNotNull { file ->
                try {
                    val content = file.readText()
                    val pack = json.decodeFromString<MacroPack>(content)
                    MacroPackState(pack = pack, file = file)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }.sortedBy { it.pack.name }

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

        _uiState.update { it.copy(macroFiles = listOf(sampleMacro) + fileMacros, macroPacks = filePacks) }
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
                    
                    if (AppSettings.enableToasts) {
                        val toastMsg = "Macro Finished: ${macro.name}"
                        if (AppSettings.enableBackgroundToasts && (AppSettings.toastTarget == "SERVER" || AppSettings.toastTarget == "BOTH")) {
                            showSystemNotification(toastMsg)
                        }
                        if (AppSettings.enableNetworkToasts && (AppSettings.toastTarget == "CLIENTS" || AppSettings.toastTarget == "BOTH")) {
                            val selectedClients = AppSettings.notificationClientIds.split(",").filter { it.isNotBlank() }.toSet()
                            if (selectedClients.isNotEmpty()) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    serverViewModel?.sendToSelected(toastMessage(toastMsg), selectedClients)
                                }
                            }
                        }
                    }

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
        _uiState.update { it.copy(filesPendingDeletion = null, isMacroSelectionMode = false) }
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

    fun onMacroSearchQueryChange(query: String) {
        _uiState.update { it.copy(macroSearchQuery = query) }
    }

    fun onPackSearchQueryChange(query: String) {
        _uiState.update { it.copy(packSearchQuery = query) }
    }

    fun toggleMacrosCollapsed() {
        _uiState.update { it.copy(isMacrosCollapsed = !it.isMacrosCollapsed) }
    }

    fun togglePacksCollapsed() {
        _uiState.update { it.copy(isPacksCollapsed = !it.isPacksCollapsed) }
    }

    fun toggleMacroSelectionMode() {
        _uiState.update { state -> 
            val newSelectionMode = !state.isMacroSelectionMode
            state.copy(
                isMacroSelectionMode = newSelectionMode,
                macroFiles = if (!newSelectionMode) state.macroFiles.map { it.copy(isSelectedForDeletion = false) } else state.macroFiles
            )
        }
    }

    fun togglePackSelectionMode() {
        _uiState.update { state ->
            val newSelectionMode = !state.isPackSelectionMode
            state.copy(
                isPackSelectionMode = newSelectionMode,
                selectedPackIds = if (!newSelectionMode) emptySet() else state.selectedPackIds
            )
        }
    }

    fun togglePackSelection(packId: String, selected: Boolean) {
        _uiState.update { state ->
            val newSelectedIds = if (selected) {
                state.selectedPackIds + packId
            } else {
                state.selectedPackIds - packId
            }
            state.copy(selectedPackIds = newSelectedIds)
        }
    }

    fun deleteSelectedPacks() {
        _uiState.update { state ->
            val packsToDelete = state.macroPacks.filter { state.selectedPackIds.contains(it.pack.id) }
            // Logic to delete pack files
            packsToDelete.forEach { packState ->
                packState.file?.delete()
            }
            state.copy(isPackSelectionMode = false, selectedPackIds = emptySet())
        }
        refresh()
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

    fun onActiveProcessChanged(processName: String?) {
        val currentState = _uiState.value
        val oldActivePack = currentState.macroPacks.find { it.pack.isActiveLive(currentState.currentActiveProcess) }
        _uiState.update { it.copy(currentActiveProcess = processName) }
        val newActivePack = _uiState.value.macroPacks.find { it.pack.isActiveLive(processName) }
        
        if (oldActivePack?.pack?.id != newActivePack?.pack?.id) {
            handlePackSwitch(oldActivePack?.pack, newActivePack?.pack)
        }
    }

    private fun MacroPack.isActiveLive(activeProcess: String?): Boolean {
        return targetProcess != null && targetProcess.equals(activeProcess, ignoreCase = true)
    }

    private fun handlePackSwitch(oldPack: MacroPack?, newPack: MacroPack?) {
        if (!AppSettings.enablePackSwitchNotifications || !AppSettings.enableToasts) return

        val includeWindow = AppSettings.includeWindowNamesInToasts
        val message = when {
            newPack != null -> "Activated Pack: ${newPack.name}${if (includeWindow) " (${newPack.targetProcess})" else ""}"
            oldPack != null -> "Deactivated Pack: ${oldPack.name}${if (includeWindow) " (${oldPack.targetProcess})" else ""}"
            else -> return
        }

        if (AppSettings.enableBackgroundToasts && (AppSettings.toastTarget == "SERVER" || AppSettings.toastTarget == "BOTH")) {
             showSystemNotification(message)
        }
        
        // Send notification to selected connected clients
        if (AppSettings.enableNetworkToasts && (AppSettings.toastTarget == "CLIENTS" || AppSettings.toastTarget == "BOTH")) {
            val selectedClients = AppSettings.notificationClientIds.split(",").filter { it.isNotBlank() }.toSet()
            if (selectedClients.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    serverViewModel?.sendToSelected(toastMessage(message), selectedClients)
                }
            }
        }

        onMacrosUpdated() // Trigger refresh
    }

    private fun showSystemNotification(message: String) {
        // Update state to show custom toast overlay
        _uiState.update { it.copy(activeToast = message) }
        
        // Auto-hide after duration
        viewModelScope.launch {
            delay(AppSettings.toastDurationMs)
            _uiState.update { 
                if (it.activeToast == message) it.copy(activeToast = null) else it 
            }
        }
    }

    fun onCreatePack() {
        val newPack = MacroPack(
            id = UUID.randomUUID().toString(),
            name = "New Pack",
            author = "Unknown",
            version = "1.0.0",
            isActive = false
        )
        _uiState.update { it.copy(packBeingEdited = newPack) }
    }

    fun onEditPack(pack: MacroPack) {
        _uiState.update { it.copy(packBeingEdited = pack) }
    }

    fun onCancelPackEdit() {
        _uiState.update { it.copy(packBeingEdited = null) }
    }

    fun onSavePack(pack: MacroPack) {
        val filename = getPackFilename(pack.name)
        val file = File(settingsViewModel.macroDirectory.value, filename)
        try {
            val content = json.encodeToString(MacroPack.serializer(), pack)
            file.writeText(content)
            _uiState.update { it.copy(packBeingEdited = null) }
            refresh()
            consoleViewModel.addLog(LogLevel.Info, "Pack '${pack.name}' saved to $filename")
        } catch (e: Exception) {
            consoleViewModel.addLog(LogLevel.Error, "Failed to save pack: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun getPackFilename(packName: String): String {
        return packName.replace(Regex("[^a-zA-Z0-9]"), "_") + "_pack.json"
    }

    fun onDeletePack(pack: MacroPack) {
        // Find the pack state to get the file
        val packState = _uiState.value.macroPacks.find { it.pack.id == pack.id }
        val file = packState?.file ?: File(settingsViewModel.macroDirectory.value, getPackFilename(pack.name))
        
        if (file.exists()) {
            _uiState.update { it.copy(filePendingDeletion = file) }
        } else {
            consoleViewModel.addLog(LogLevel.Error, "Could not find file for pack '${pack.name}' to delete.")
        }
    }

    fun onOpenPackInJsonEditor(pack: MacroPack) {
        val packState = _uiState.value.macroPacks.find { it.pack.id == pack.id }
        val file = packState?.file ?: File(settingsViewModel.macroDirectory.value, getPackFilename(pack.name))

        if (file.exists()) {
            val macroState = MacroFileState(
                id = file.absolutePath,
                file = file,
                name = pack.name,
                content = file.readText(),
                isActive = true
            )
            onEditMacroRequested(macroState)
            _uiState.update { it.copy(packBeingEdited = null) }
        }
    }
}