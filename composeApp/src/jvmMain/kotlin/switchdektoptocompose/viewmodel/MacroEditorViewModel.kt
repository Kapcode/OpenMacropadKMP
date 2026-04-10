package switchdektoptocompose.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import switchdektoptocompose.model.*
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

data class EditorUiState(
    val tabs: List<EditorTabState> = emptyList(),
    val selectedTabIndex: Int = 0
)

class MacroEditorViewModel(
    private val settingsViewModel: SettingsViewModel,
    private val onSave: () -> Unit
) {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState = _uiState.asStateFlow()

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    val tabs: StateFlow<List<EditorTabState>> = _uiState.map { it.tabs }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedTabIndex: StateFlow<Int> = _uiState.map { it.selectedTabIndex }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        addNewTab()
    }

    fun openOrSwitchToTab(macro: MacroFileState) {
        val fileToOpen = macro.file
        val idToCheck = fileToOpen?.absolutePath ?: macro.id

        val existingTabIndex = _uiState.value.tabs.indexOfFirst {
            val tabId = it.file?.absolutePath ?: (if (it.title == "Sample Macro") "__SAMPLE_MACRO__" else null)
            tabId == idToCheck
        }

        if (existingTabIndex != -1) {
            selectTab(existingTabIndex)
        } else {
            try {
                val content = fileToOpen?.readText() ?: macro.content
                val newTab = EditorTabState(
                    title = macro.name,
                    content = content,
                    file = fileToOpen
                )
                _uiState.update { it.copy(tabs = it.tabs + newTab, selectedTabIndex = it.tabs.size) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addNewTab() {
        val newTab = EditorTabState(
            title = "New Macro",
            content = "{\n    \"events\": []\n}"
        )
        _uiState.update { it.copy(tabs = it.tabs + newTab, selectedTabIndex = it.tabs.size) }
    }

    fun selectTab(index: Int) {
        if (index in _uiState.value.tabs.indices) {
            _uiState.update { it.copy(selectedTabIndex = index) }
        }
    }

    fun closeTab(index: Int) {
        if (_uiState.value.tabs.isEmpty()) return
        if (index !in _uiState.value.tabs.indices) return

        _uiState.update { state ->
            val newTabs = state.tabs.toMutableList().apply { removeAt(index) }
            if (newTabs.isEmpty()) {
                val newTab = EditorTabState(
                    title = "New Macro",
                    content = "{\n    \"events\": []\n}"
                )
                state.copy(tabs = listOf(newTab), selectedTabIndex = 0)
            } else {
                val newIndex = if (state.selectedTabIndex >= newTabs.size) newTabs.lastIndex else state.selectedTabIndex
                state.copy(tabs = newTabs, selectedTabIndex = newIndex)
            }
        }
    }

    fun updateSelectedTabContent(newContent: String) {
        val currentIndex = _uiState.value.selectedTabIndex
        if (currentIndex !in _uiState.value.tabs.indices) return

        if (_uiState.value.tabs[currentIndex].content == newContent) {
            return
        }

        _uiState.update { state ->
            val newTabs = state.tabs.toMutableList().also {
                it[currentIndex] = it[currentIndex].copy(content = newContent)
            }
            state.copy(tabs = newTabs)
        }
    }

    fun saveSelectedTab() {
        val currentTab = _uiState.value.tabs.getOrNull(_uiState.value.selectedTabIndex) ?: return
        if (currentTab.file != null) {
            currentTab.file.writeText(currentTab.content)
            onSave()
        } else {
            saveSelectedTabAs()
        }
    }
    
    private fun findNextAvailableMacroFile(macroDirectory: File): File {
        var index = 1
        var nextFile: File
        do {
            nextFile = File(macroDirectory, "newMacro$index.json")
            index++
        } while (nextFile.exists())
        return nextFile
    }

    fun saveSelectedTabAs() {
        val currentTab = _uiState.value.tabs.getOrNull(_uiState.value.selectedTabIndex) ?: return
        val macroDirectory = File(settingsViewModel.macroDirectory.value)
        val defaultFile = findNextAvailableMacroFile(macroDirectory)
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Save Macro As..."
            fileFilter = FileNameExtensionFilter("JSON Files", "json")
            currentDirectory = macroDirectory
            selectedFile = defaultFile
        }
        val result = fileChooser.showSaveDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            var selectedFile = fileChooser.selectedFile
            if (!selectedFile.name.endsWith(".json", ignoreCase = true)) {
                selectedFile = File(selectedFile.absolutePath + ".json")
            }
            selectedFile.writeText(currentTab.content)
            val newTabState = currentTab.copy(
                file = selectedFile,
                title = selectedFile.nameWithoutExtension
            )
            _uiState.update { state ->
                val newTabs = state.tabs.toMutableList().apply { set(state.selectedTabIndex, newTabState) }
                state.copy(tabs = newTabs)
            }
            onSave()
        }
    }
}