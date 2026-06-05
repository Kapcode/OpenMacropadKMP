package com.kapcode.open.macropad.kmps.desktop.viewmodel

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
import org.json.JSONObject
import com.kapcode.open.macropad.kmps.desktop.logic.*
import com.kapcode.open.macropad.kmps.desktop.model.*
import com.kapcode.open.macropad.kmps.models.MacroPack
import com.kapcode.open.macropad.kmps.network.sockets.model.toastMessage
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
    val currentActiveProcessName: String? = null,
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
    val triggerPendingConfirmation: UnifiedTrigger? = null,
    val activeToast: String? = null
)

class MacroManagerViewModel(
    val settingsViewModel: SettingsViewModel,
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

    val currentActiveProcessName: StateFlow<String?> = _uiState.map { it.currentActiveProcessName }
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

    val triggerPendingConfirmation: StateFlow<UnifiedTrigger?> = _uiState.map { it.triggerPendingConfirmation }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, null)

    private val playbackJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(Dispatchers.IO + playbackJob)
    private val executionMutex = Mutex()

    private val robot = try { java.awt.Robot() } catch(_: Exception) { null }

    internal val macroPlayer = MacroPlayer(
        onLog = { level, msg -> consoleViewModel.addLog(level, msg) },
        getActiveProcess = { _uiState.value.currentActiveProcess },
        onPlayMacroRequested = { macroId -> 
            _uiState.value.macroFiles.find { it.id == macroId }?.let { onPlayMacro(it) }
        },
        onNotify = { msg -> showSystemNotification(msg) }
    )

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    private val prettyJson = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = true
        prettyPrintIndent = "    "
        classDiscriminator = "type"
    }
    private val json = Json { 
        ignoreUnknownKeys = true 
        classDiscriminator = "type"
    }
    private val variables = mutableMapOf<String, String>()
    private val variableCache = mutableMapOf<String, String>() // Per-pulse cache
    private val lastTriggeredRoutine = mutableMapOf<String, Boolean>()

    private val activeMacrosFile = File(System.getProperty("user.home"), ".open-macropad-active-macros.properties")
    private val activeMacrosProps = Properties()

    init {
        loadActiveMacros()
        viewModelScope.launch {
            settingsViewModel.macroDirectory.collect { directoryPath ->
                loadMacrosFromDisk(directoryPath)
            }
        }
        
        // Periodic background pulse for state triggers (e.g. catch clipboard changes while UI is open)
        viewModelScope.launch(Dispatchers.Default) {
            while(isActive) {
                delay(settingsViewModel.systemPollingRate.value)
                evaluateStateTriggers()
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
            macroDir.listFiles { _, name -> 
                name.endsWith(".json", ignoreCase = true) || name.endsWith(".js", ignoreCase = true)
            }?.toList() ?: emptyList()
        }

        val fileMacros = allFiles.filter { !it.name.endsWith("_pack.json", ignoreCase = true) }
            .mapNotNull { file ->
                try {
                    val content = file.readText()
                    val (_, allowedClients) = if (file.name.endsWith(".json", ignoreCase = true)) {
                        val json = JSONObject(content)
                        val t = json.optJSONObject("trigger")
                        t to (t?.optString("allowedClients", "") ?: "")
                    } else {
                        null to "" // JS files don't have built-in triggers yet
                    }

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
        val sampleMacroIsActive = activeMacrosProps.getProperty("__SAMPLE_MACRO__", "true").toBoolean()
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


    fun getActiveMacrosForClient(clientName: String, isTrusted: Boolean = true): List<MacroFileState> {
        return _uiState.value.macroFiles.filter { macro ->
            macro.isActive && (
                macro.allowedClients.isBlank() ||
                (isTrusted && macro.allowedClients == "ALL_TRUSTED") ||
                macro.allowedClients.split(',')
                    .map { it.trim() }
                    .any { it.equals(clientName, ignoreCase = true) }
            )
        }
    }

    fun onTriggerRoutine(routine: com.kapcode.open.macropad.kmps.models.AutomationRoutine) {
        viewModelScope.launch {
            // Check if routine belongs to a pack and if that pack's window conditions are met
            val parentPack = _uiState.value.macroPacks.find { it.pack.routines.any { r -> r.id == routine.id } }?.pack
            if (parentPack != null) {
                val isProcessMatch = parentPack.targetProcess == null || 
                    _uiState.value.currentActiveProcess?.contains(parentPack.targetProcess, ignoreCase = true) == true
                val isTitleMatch = parentPack.targetWindowTitle == null || 
                    getSystemVariable("current_window_title")?.contains(parentPack.targetWindowTitle, ignoreCase = true) == true
                
                if (!isProcessMatch || !isTitleMatch) {
                    // Routine belongs to a pack but the focus conditions are not met
                    return@launch
                }
            }

            if (executionMutex.tryLock()) {
                try {
                    consoleViewModel.addLog(LogLevel.Info, "Routine triggered: ${routine.name}")
                    var currentAutoDelay = 50L
                    routine.logicBlocks.forEach { block ->
                        if (evaluateCondition(block.condition)) {
                            block.actions.forEach { action ->
                                when (action) {
                                    is com.kapcode.open.macropad.kmps.models.AutomationAction.SetAutoDelay -> {
                                        currentAutoDelay = resolveToLong(action.delayMs)
                                    }
                                    is com.kapcode.open.macropad.kmps.models.AutomationAction.SetVariable -> {
                                        setVariable(action.name, action.value)
                                    }
                                    is com.kapcode.open.macropad.kmps.models.AutomationAction.KeyEvent -> {
                                        executeKeyEvent(action)
                                        delay(currentAutoDelay)
                                    }
                                    is com.kapcode.open.macropad.kmps.models.AutomationAction.MouseEvent -> {
                                        executeMouseEvent(action)
                                        delay(currentAutoDelay)
                                    }
                                    is com.kapcode.open.macropad.kmps.models.AutomationAction.MouseButtonEvent -> {
                                        executeMouseButtonEvent(action)
                                        delay(currentAutoDelay)
                                    }
                                    is com.kapcode.open.macropad.kmps.models.AutomationAction.ScrollEvent -> {
                                        macroPlayer.play(listOf(MacroEventState.ScrollEvent(resolveToInt(action.amount))))
                                        delay(currentAutoDelay)
                                    }
                                    is com.kapcode.open.macropad.kmps.models.AutomationAction.DelayEvent -> {
                                        delay(resolveToLong(action.durationMs))
                                    }
                                    is com.kapcode.open.macropad.kmps.models.AutomationAction.ScriptAction -> {
                                        macroPlayer.executeScript(action.script)
                                    }
                                    is com.kapcode.open.macropad.kmps.models.AutomationAction.MacroAction -> {
                                        _uiState.value.macroFiles.find { it.id == action.macroId }?.let {
                                            onPlayMacro(it)
                                        }
                                    }
                                    is com.kapcode.open.macropad.kmps.models.AutomationAction.ControllerButton -> {
                                        consoleViewModel.addLog(LogLevel.Info, "Simulated Controller Button: ${action.button}")
                                    }
                                    else -> { /* Handle others */ }
                                }
                            }
                        }
                    }
                } finally {
                    executionMutex.unlock()
                }
            }
        }
    }

    fun setVariable(name: String, value: String) {
        val resolvedValue = getSystemVariable(value) ?: value
        if (name == "clipboard_text") {
            try {
                val selection = java.awt.datatransfer.StringSelection(resolvedValue)
                Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            variables[name] = resolvedValue
        }
        evaluateStateTriggers()
    }

    fun getSystemVariable(name: String, useCache: Boolean = false): String? {
        if (useCache && variableCache.containsKey(name)) {
            return variableCache[name]
        }

        val value = when (name) {
            "current_window_name" -> _uiState.value.currentActiveProcess ?: ""
            "last_window_name" -> serverViewModel?.processWatcher?.lastProcess?.value ?: ""
            "current_process_name" -> _uiState.value.currentActiveProcessName ?: ""
            "last_process_name" -> serverViewModel?.processWatcher?.lastProcessName?.value ?: ""
            "current_window_title" -> serverViewModel?.processWatcher?.activeTitle?.value ?: ""
            "last_window_title" -> serverViewModel?.processWatcher?.lastTitle?.value ?: ""
            "mouse_x" -> try { java.awt.MouseInfo.getPointerInfo().location.x.toString() } catch(e: Exception) { "0" }
            "mouse_y" -> try { java.awt.MouseInfo.getPointerInfo().location.y.toString() } catch(e: Exception) { "0" }
            "pixel_color_at_cursor" -> {
                try {
                    val info = java.awt.MouseInfo.getPointerInfo()
                    if (info != null && robot != null) {
                        val loc = info.location
                        // Optimization: snapshot pixel color to avoid repeated heavy system calls
                        val color = robot.getPixelColor(loc.x, loc.y)
                        String.format("#%02x%02x%02x", color.red, color.green, color.blue)
                    } else "#000000"
                } catch(e: Throwable) { "#000000" }
            }
            "clipboard_text" -> {
                try {
                    // NOTE: ClassNotFoundExceptions seen in logs while running in IDE are benign side-effects 
                    // of AWT probing IntelliJ's custom clipboard formats.
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    if (clipboard.isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.stringFlavor)) {
                        clipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor) as String
                    } else ""
                } catch (e: Throwable) { "" }
            }
            "current_time_ms" -> System.currentTimeMillis().toString()
            else -> variables[name]
        }

        if (useCache && value != null) {
            variableCache[name] = value
        }
        return value
    }

    fun evaluateStateTriggers() {
        variableCache.clear() // Clear cache at start of pulse
        _uiState.value.macroPacks.filter { it.pack.isActive }.flatMap { it.pack.routines }.forEach { routine ->
            routine.triggers.forEach { trigger ->
                if (trigger is com.kapcode.open.macropad.kmps.models.AutomationTrigger.OnConditionMet) {
                    val triggerId = "${routine.id}_${trigger.hashCode()}"
                    val isMet = evaluateCondition(trigger.condition, useCache = true)
                    val wasMet = lastTriggeredRoutine[triggerId] ?: false
                    
                    if (isMet && !wasMet) {
                        onTriggerRoutine(routine)
                    }
                    lastTriggeredRoutine[triggerId] = isMet
                }
            }
        }
    }

    fun onControllerButtonTriggered(button: String, controllerIndex: Int) {
        _uiState.value.macroPacks.filter { it.pack.isActive }.flatMap { it.pack.routines }.forEach { routine ->
            routine.triggers.forEach { trigger ->
                if (trigger is com.kapcode.open.macropad.kmps.models.AutomationTrigger.ControllerButton) {
                    if (trigger.button.equals(button, ignoreCase = true) && 
                        (trigger.controllerIndex == controllerIndex.toString() || trigger.controllerIndex == "ANY")) {
                        onTriggerRoutine(routine)
                    }
                }
            }
        }
    }

    private fun resolveToLong(input: String): Long {
        return getSystemVariable(input)?.toLongOrNull() ?: input.toLongOrNull() ?: 0L
    }

    private fun resolveToInt(input: String): Int {
        return getSystemVariable(input)?.toIntOrNull() ?: input.toIntOrNull() ?: 0
    }

    private suspend fun executeKeyEvent(action: com.kapcode.open.macropad.kmps.models.AutomationAction.KeyEvent) {
        if (action.actionType == "TYPE") {
            action.keyName.forEach { char ->
                macroPlayer.play(listOf(
                    MacroEventState.KeyEvent(char.toString(), KeyAction.PRESS),
                    MacroEventState.KeyEvent(char.toString(), KeyAction.RELEASE)
                ))
            }
        } else {
            val keyAction = if (action.actionType == "PRESS") KeyAction.PRESS else KeyAction.RELEASE
            macroPlayer.play(listOf(MacroEventState.KeyEvent(action.keyName, keyAction)))
        }
    }

    private suspend fun executeMouseEvent(action: com.kapcode.open.macropad.kmps.models.AutomationAction.MouseEvent) {
        val mouseAction = if (action.actionType == "MOVE") MouseAction.MOVE else MouseAction.CLICK
        macroPlayer.play(listOf(MacroEventState.MouseEvent(resolveToInt(action.x), resolveToInt(action.y), mouseAction, action.isAnimated)))
    }

    private suspend fun executeMouseButtonEvent(action: com.kapcode.open.macropad.kmps.models.AutomationAction.MouseButtonEvent) {
        val keyAction = if (action.actionType == "PRESS") KeyAction.PRESS else KeyAction.RELEASE
        macroPlayer.play(listOf(MacroEventState.MouseButtonEvent(resolveToInt(action.buttonNumber), keyAction)))
    }

    private fun evaluateCondition(condition: com.kapcode.open.macropad.kmps.models.AutomationCondition?, useCache: Boolean = false): Boolean {
        if (condition == null) return true
        return when (condition) {
            is com.kapcode.open.macropad.kmps.models.AutomationCondition.ActiveWindowIs -> {
                _uiState.value.currentActiveProcess?.contains(condition.processName, ignoreCase = true) == true
            }
            is com.kapcode.open.macropad.kmps.models.AutomationCondition.ActiveWindowTitleIs -> {
                getSystemVariable("current_window_title", useCache)?.contains(condition.windowTitle, ignoreCase = true) == true
            }
            is com.kapcode.open.macropad.kmps.models.AutomationCondition.Equals -> {
                val current = getSystemVariable(condition.variable, useCache)
                val target = getSystemVariable(condition.value, useCache) ?: condition.value
                current == target
            }
            is com.kapcode.open.macropad.kmps.models.AutomationCondition.GreaterThan -> {
                val varVal = getSystemVariable(condition.variable, useCache)?.toDoubleOrNull() ?: 0.0
                val targetVal = getSystemVariable(condition.value, useCache)?.toDoubleOrNull() ?: condition.value.toDoubleOrNull() ?: 0.0
                varVal > targetVal
            }
            is com.kapcode.open.macropad.kmps.models.AutomationCondition.LessThan -> {
                val varVal = getSystemVariable(condition.variable, useCache)?.toDoubleOrNull() ?: 0.0
                val targetVal = getSystemVariable(condition.value, useCache)?.toDoubleOrNull() ?: condition.value.toDoubleOrNull() ?: 0.0
                varVal < targetVal
            }
            is com.kapcode.open.macropad.kmps.models.AutomationCondition.Contains -> {
                val current = getSystemVariable(condition.variable, useCache)
                val target = getSystemVariable(condition.substring, useCache) ?: condition.substring
                current?.contains(target, ignoreCase = true) == true
            }
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
                    
                    if (macro.file?.name?.endsWith(".js", ignoreCase = true) == true) {
                        macroPlayer.executeScript(content)
                    } else {
                        val events = parseEventsFromJson(content, macro.name)
                        macroPlayer.play(events)
                    }

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

    internal fun parseEventsFromJson(jsonContent: String, macroName: String = "Unknown"): List<MacroEventState> {
        val events = mutableListOf<MacroEventState>()
        try {
            val json = JSONObject(jsonContent)
            json.optJSONArray("events")?.let { eventsArray ->
                for (i in 0 until eventsArray.length()) {
                    eventsArray.getJSONObject(i)?.let { eventObj ->
                        try {
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
                        } catch (e: Exception) {
                            consoleViewModel.addLog(LogLevel.Error, "Error parsing event $i in macro '$macroName': ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            consoleViewModel.addLog(LogLevel.Error, "JSON Syntax Error in macro '$macroName': ${e.message}")
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

    fun onToggleMacroActive(id: String, isActive: Boolean) {
        if (id.startsWith("/")) {
            _uiState.update { state ->
                state.copy(
                    macroFiles = state.macroFiles.map {
                        if (it.id == id) it.copy(isActive = isActive) else it
                    }
                )
            }
            activeMacrosProps.setProperty(id, isActive.toString())
            saveActiveMacros()
        } else {
            _uiState.update { state ->
                val updatedPacks = state.macroPacks.map { packState ->
                    if (packState.pack.id == id) {
                        val updatedPack = packState.pack.copy(isActive = isActive)
                        packState.file?.let { file ->
                            try {
                                val jsonString = json.encodeToString(updatedPack)
                                file.writeText(jsonString)
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                        packState.copy(pack = updatedPack)
                    } else packState
                }
                state.copy(macroPacks = updatedPacks)
            }
        }
        onMacrosUpdated()
    }

    fun onActiveProcessChanged(info: ActiveProcessInfo?) {
        val oldActivePackId = _uiState.value.macroPacks.find { it.pack.isActiveLive(lastActiveInfo) }?.pack?.id
        
        lastActiveInfo = info
        
        _uiState.update { it.copy(
            currentActiveProcess = info?.name,
            currentActiveProcessName = info?.processName
        ) }
        
        val newActivePack = _uiState.value.macroPacks.find { it.pack.isActiveLive(info) }
        
        if (oldActivePackId != newActivePack?.pack?.id) {
            val oldPack = _uiState.value.macroPacks.find { it.pack.id == oldActivePackId }?.pack
            handlePackSwitch(oldPack, newActivePack?.pack)
        }
        
        evaluateStateTriggers()
    }

    private var lastActiveInfo: ActiveProcessInfo? = null


    private fun MacroPack.isActiveLive(info: ActiveProcessInfo?): Boolean {
        if (!isActive) return false
        
        // If we have new-style groups, use those
        if (autoSwitchGroups.isNotEmpty()) {
            return autoSwitchGroups.any { group ->
                group.rules.isNotEmpty() && group.rules.all { rule -> evaluateRule(rule, info) }
            }
        }

        // Fallback to legacy matching
        val isProcessMatch = targetProcess != null && (
            targetProcess.equals(info?.processName, ignoreCase = true) || 
            targetProcess.equals(info?.name, ignoreCase = true)
        )
        val isTitleMatch = targetWindowTitle != null && info?.windowTitle?.contains(targetWindowTitle, ignoreCase = true) == true
        
        return isProcessMatch || isTitleMatch
    }

    private fun evaluateRule(rule: com.kapcode.open.macropad.kmps.models.AutoSwitchRule, info: ActiveProcessInfo?): Boolean {
        if (info == null) return false
        
        val targetValue = when (rule.target) {
            com.kapcode.open.macropad.kmps.models.MatchTarget.PROCESS_NAME -> info.processName
            com.kapcode.open.macropad.kmps.models.MatchTarget.APP_NAME -> info.name
            com.kapcode.open.macropad.kmps.models.MatchTarget.WINDOW_TITLE -> info.windowTitle
        }

        return when (rule.operator) {
            com.kapcode.open.macropad.kmps.models.MatchOperator.EQUALS -> targetValue.equals(rule.value, ignoreCase = rule.ignoreCase)
            com.kapcode.open.macropad.kmps.models.MatchOperator.CONTAINS -> targetValue.contains(rule.value, ignoreCase = rule.ignoreCase)
            com.kapcode.open.macropad.kmps.models.MatchOperator.STARTS_WITH -> targetValue.startsWith(rule.value, ignoreCase = rule.ignoreCase)
            com.kapcode.open.macropad.kmps.models.MatchOperator.ENDS_WITH -> targetValue.endsWith(rule.value, ignoreCase = rule.ignoreCase)
            com.kapcode.open.macropad.kmps.models.MatchOperator.REGEX -> try {
                val options = if (rule.ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
                Regex(rule.value, options).containsMatchIn(targetValue)
            } catch (e: Exception) { false }
        }
    }

    private fun handlePackSwitch(oldPack: MacroPack?, newPack: MacroPack?) {
        if (!AppSettings.enablePackSwitchNotifications || !AppSettings.enableToasts) return

        val includeWindow = AppSettings.includeWindowNamesInToasts
        val message = when {
            newPack != null -> {
                val detail = if (includeWindow) {
                    val target = if (newPack.autoSwitchGroups.isNotEmpty()) {
                        newPack.autoSwitchGroups.firstOrNull()?.rules?.firstOrNull()?.value ?: ""
                    } else {
                        newPack.targetProcess ?: newPack.targetWindowTitle ?: ""
                    }
                    if (target.isNotEmpty()) " ($target)" else ""
                } else ""
                "Activated Pack: ${newPack.name}$detail"
            }
            oldPack != null -> {
                val detail = if (includeWindow) {
                    val target = if (oldPack.autoSwitchGroups.isNotEmpty()) {
                        oldPack.autoSwitchGroups.firstOrNull()?.rules?.firstOrNull()?.value ?: ""
                    } else {
                        oldPack.targetProcess ?: oldPack.targetWindowTitle ?: ""
                    }
                    if (target.isNotEmpty()) " ($target)" else ""
                } else ""
                "Deactivated Pack: ${oldPack.name}$detail"
            }
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
        consoleViewModel.addLog(LogLevel.Verbose, "Showing Global Toast: $message")
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
        println("[PackLifecycle] Creating new pack...")
        val newPack = MacroPack(
            id = UUID.randomUUID().toString(),
            name = "New Pack",
            author = "Unknown",
            version = "1.0.0",
            isActive = false
        )
        _uiState.update { 
            println("[PackLifecycle] Setting packBeingEdited for new pack: ${newPack.id}")
            it.copy(packBeingEdited = newPack) 
        }
    }

    fun onEditPack(pack: MacroPack) {
        println("[PackLifecycle] Editing existing pack: ${pack.name} (${pack.id})")
        _uiState.update { it.copy(packBeingEdited = pack) }
    }

    fun onCancelPackEdit() {
        println("[PackLifecycle] Cancelling pack edit.")
        _uiState.update { it.copy(packBeingEdited = null) }
    }

    fun onSavePack(pack: MacroPack) {
        println("[PackLifecycle] Saving pack: ${pack.name} (${pack.id})")
        val filename = getPackFilename(pack.name)
        val file = File(settingsViewModel.macroDirectory.value, filename)
        try {
            println("[PackLifecycle] Serializing pack data...")
            val content = prettyJson.encodeToString(MacroPack.serializer(), pack)
            println("[PackLifecycle] Writing to file: ${file.absolutePath}")
            file.writeText(content)
            _uiState.update { 
                println("[PackLifecycle] Resetting packBeingEdited to null after save.")
                it.copy(packBeingEdited = null) 
            }
            refresh()
            consoleViewModel.addLog(LogLevel.Info, "Pack '${pack.name}' saved to $filename")
        } catch (e: Exception) {
            println("[PackLifecycle] ERROR saving pack: ${e.message}")
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

        // Serialize with indentation for the editor
        val indentedContent = try {
            prettyJson.encodeToString(MacroPack.serializer(), pack)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (indentedContent != null) {
            val macroState = MacroFileState(
                id = file.absolutePath,
                file = file,
                name = pack.name,
                content = indentedContent,
                isActive = true
            )
            onEditMacroRequested(macroState)
            _uiState.update { it.copy(packBeingEdited = null) }
        }
    }

//    fun showTriggerConfirmation(trigger: UnifiedTrigger) {
//        _uiState.update { it.copy(triggerPendingConfirmation = trigger) }
//    }

    fun confirmTrigger() {
        val trigger = _uiState.value.triggerPendingConfirmation ?: return
        _uiState.update { it.copy(triggerPendingConfirmation = null) }
        serverViewModel?.triggerListener?.evaluator?.execute(trigger)
    }

    fun cancelTrigger() {
        _uiState.update { it.copy(triggerPendingConfirmation = null) }
    }

    fun resetAllMacros() {
        val macroDir = File(settingsViewModel.macroDirectory.value)
        if (macroDir.exists() && macroDir.isDirectory) {
            macroDir.listFiles { _, name ->
                name.endsWith(".json", ignoreCase = true) || name.endsWith(".js", ignoreCase = true)
            }?.forEach { it.delete() }
        }
        refresh()
    }

    fun resetAllPacks() {
        val macroDir = File(settingsViewModel.macroDirectory.value)
        if (macroDir.exists() && macroDir.isDirectory) {
            macroDir.listFiles { _, name ->
                name.endsWith("_pack.json", ignoreCase = true)
            }?.forEach { it.delete() }
        }
        refresh()
    }
}
