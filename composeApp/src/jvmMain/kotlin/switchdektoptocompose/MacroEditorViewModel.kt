package switchdektoptocompose

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

data class EditorTabState(
    val title: String,
    val content: String,
    val file: File? = null
)

class MacroEditorViewModel(
    private val settingsViewModel: SettingsViewModel,
    private val onSave: () -> Unit
) {

    private val _tabs = MutableStateFlow<List<EditorTabState>>(emptyList())
    val tabs = _tabs.asStateFlow()

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex = _selectedTabIndex.asStateFlow()

    init {
        addNewTab()
    }

    fun openOrSwitchToTab(macro: MacroFileState) {
        val fileToOpen = macro.file
        val idToCheck = fileToOpen?.absolutePath ?: macro.id

        val existingTabIndex = tabs.value.indexOfFirst {
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
                _tabs.update { it + newTab }
                _selectedTabIndex.value = tabs.value.lastIndex
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
        _tabs.update { it + newTab }
        _selectedTabIndex.value = tabs.value.lastIndex
    }

    fun selectTab(index: Int) {
        if (index in tabs.value.indices) {
            _selectedTabIndex.value = index
        }
    }

    fun closeTab(index: Int) {
        if (tabs.value.isEmpty()) return
        if (index !in tabs.value.indices) return

        _tabs.update { it.toMutableList().apply { removeAt(index) } }

        if (tabs.value.isEmpty()) {
            addNewTab()
        } else if (_selectedTabIndex.value >= tabs.value.size) {
            _selectedTabIndex.value = tabs.value.lastIndex
        }
    }

    fun updateSelectedTabContent(newContent: String) {
        val currentIndex = _selectedTabIndex.value
        if (currentIndex !in tabs.value.indices) return

        if (tabs.value[currentIndex].content == newContent) {
            return
        }

        _tabs.update { currentTabs ->
            currentTabs.toMutableList().also {
                it[currentIndex] = it[currentIndex].copy(content = newContent)
            }
        }
    }

    fun saveSelectedTab() {
        val currentTab = tabs.value.getOrNull(selectedTabIndex.value) ?: return
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
        val currentTab = tabs.value.getOrNull(selectedTabIndex.value) ?: return
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
            _tabs.update {
                it.toMutableList().apply { set(selectedTabIndex.value, newTabState) }
            }
            onSave()
        }
    }
}