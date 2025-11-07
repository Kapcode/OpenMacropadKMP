package switchdektoptocompose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

data class MacroFileState(
    val id: String,
    val file: File?,
    val name: String,
    val content: String,
    val isActive: Boolean = false,
    val isSelectedForDeletion: Boolean = false
)

class MacroManagerViewModel(
    private val settingsViewModel: SettingsViewModel,
    var onEditMacroRequested: (MacroFileState) -> Unit
) {

    private val sampleMacroContent = """
    {
        "trigger": { "keyName": "ESCAPE", "action": "RELEASE", "type": "key" },
        "events": [
            { "keyName": "WINDOWS", "action": "PRESS", "type": "key" },
            { "keyName": "WINDOWS", "action": "RELEASE", "type": "key" }
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
    
    private val macroPlayer = MacroPlayer()
    private val viewModelScope = CoroutineScope(Dispatchers.IO)

    init {
        viewModelScope.launch {
            settingsViewModel.macroDirectory.collect { directoryPath ->
                loadMacrosFromDisk(directoryPath)
            }
        }
    }
    
    private fun loadMacrosFromDisk(directoryPath: String) {
        val macroDir = File(directoryPath)
        val fileMacros = if (!macroDir.exists() || !macroDir.isDirectory) {
            emptyList()
        } else {
            macroDir.listFiles { _, name -> name.endsWith(".json", ignoreCase = true) }
                ?.map { file ->
                    val isActive = _macroFiles.value.find { it.id == file.absolutePath }?.isActive ?: false
                    MacroFileState(id = file.absolutePath, file = file, name = file.nameWithoutExtension, content = "", isActive = isActive)
                }?.sortedBy { it.name } ?: emptyList()
        }
        
        val sampleMacro = MacroFileState(
            id = "__SAMPLE_MACRO__",
            file = null,
            name = "Sample Macro",
            content = sampleMacroContent,
            isActive = _macroFiles.value.find { it.id == "__SAMPLE_MACRO__" }?.isActive ?: false
        )
        
        _macroFiles.value = listOf(sampleMacro) + fileMacros
    }

    fun onPlayMacro(macro: MacroFileState) {
        viewModelScope.launch {
            try {
                val content = macro.file?.readText() ?: macro.content
                val events = parseEventsFromJson(content)
                macroPlayer.play(events)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // --- CORRECTED FUNCTION ---
    private fun parseEventsFromJson(jsonContent: String): List<MacroEventState> {
        val events = mutableListOf<MacroEventState>()
        try {
            val json = JSONObject(jsonContent)
            json.optJSONArray("events")?.let { eventsArray ->
                for (i in 0 until eventsArray.length()) {
                    eventsArray.getJSONObject(i)?.let { eventObj ->
                        when (eventObj.getString("type").lowercase()) {
                            "key" -> {
                                events.add(MacroEventState.KeyEvent(
                                    keyName = eventObj.getString("keyName"),
                                    action = KeyAction.valueOf(eventObj.getString("action").uppercase())
                                ))
                            }
                            "delay" -> {
                                events.add(MacroEventState.DelayEvent(
                                    durationMs = eventObj.getLong("durationMs")
                                ))
                            }
                            "set_auto_wait" -> {
                                events.add(MacroEventState.SetAutoWaitEvent(
                                    delayMs = eventObj.getInt("value")
                                ))
                            }
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
            // file.delete()
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
            // it.delete()
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
    }
}