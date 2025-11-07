package switchdektoptocompose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class MacroFileState(
    val file: File,
    val name: String = file.nameWithoutExtension,
    val content: String = "",
    val isActive: Boolean = false,
    val isSelectedForDeletion: Boolean = false
)

class MacroManagerViewModel(
    private val settingsViewModel: SettingsViewModel,
    private val onEditMacroRequested: (File) -> Unit
) {

    private val _macroFiles = MutableStateFlow<List<MacroFileState>>(emptyList())
    val macroFiles: StateFlow<List<MacroFileState>> = _macroFiles.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _filePendingDeletion = MutableStateFlow<File?>(null)
    val filePendingDeletion: StateFlow<File?> = _filePendingDeletion.asStateFlow()

    // State for multiple file deletion
    private val _filesPendingDeletion = MutableStateFlow<List<File>?>(null)
    val filesPendingDeletion: StateFlow<List<File>?> = _filesPendingDeletion.asStateFlow()

    private val viewModelScope = CoroutineScope(Dispatchers.IO)

    init {
        viewModelScope.launch {
            settingsViewModel.macroDirectory.collect { directoryPath ->
                loadMacrosFromDisk(directoryPath)
            }
        }
    }

    fun refresh() {
        loadMacrosFromDisk(settingsViewModel.macroDirectory.value)
    }

    private fun loadMacrosFromDisk(directoryPath: String) {
        val macroDir = File(directoryPath)
        if (!macroDir.exists() || !macroDir.isDirectory) {
            _macroFiles.value = emptyList()
            return
        }
        val files = macroDir.listFiles { _, name -> name.endsWith(".json", ignoreCase = true) }
            ?.map { file -> MacroFileState(file = file) }
            ?: emptyList()
        _macroFiles.value = files.sortedBy { it.name }
    }
    
    // --- Single Deletion Logic ---
    fun onDeleteMacro(file: File) {
        _filePendingDeletion.value = file
    }

    fun confirmDeletion() {
        _filePendingDeletion.value?.let { file ->
            // file.delete()
            println("DELETING: ${file.name}")
            refresh()
        }
        _filePendingDeletion.value = null
    }

    fun cancelDeletion() {
        _filePendingDeletion.value = null
    }

    // --- Multiple Deletion Logic ---
    fun deleteSelectedMacros() {
        val filesToDelete = _macroFiles.value.filter { it.isSelectedForDeletion }.map { it.file }
        if (filesToDelete.isEmpty()) return
        _filesPendingDeletion.value = filesToDelete
    }

    fun confirmMultipleDeletion() {
        _filesPendingDeletion.value?.forEach { file ->
            // file.delete()
            println("DELETING (batch): ${file.name}")
        }
        _filesPendingDeletion.value = null
        refresh()
        _isSelectionMode.value = false
    }

    fun cancelMultipleDeletion() {
        _filesPendingDeletion.value = null
    }

    // --- Other actions ---
    fun onEditMacro(file: File) {
        onEditMacroRequested(file)
    }

    fun toggleSelectionMode() {
        _isSelectionMode.update { !it }
        if (!_isSelectionMode.value) {
            _macroFiles.update { list -> list.map { it.copy(isSelectedForDeletion = false) } }
        }
    }

    fun selectMacroForDeletion(file: File, select: Boolean) {
        _macroFiles.update { list ->
            list.map {
                if (it.file.absolutePath == file.absolutePath) it.copy(isSelectedForDeletion = select) else it
            }
        }
    }

    fun onPlayMacro(file: File) {
        println("PLAYING (simulated): ${file.name}")
    }

    fun onToggleMacroActive(file: File, isActive: Boolean) {
        _macroFiles.update { list ->
            list.map {
                if (it.file.absolutePath == file.absolutePath) it.copy(isActive = isActive) else it
            }
        }
        println("TOGGLE ACTIVE (simulated): ${file.name} to $isActive")
    }
}