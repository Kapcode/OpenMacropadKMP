package switchdektoptocompose

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * Represents the state of a single editor tab.
 */
data class EditorTabState(
    val title: String,
    val content: String,
    val file: File? = null
)

/**
 * ViewModel to manage the state of the tabbed macro editor UI.
 */
class MacroEditorViewModel {

    private val _tabs = MutableStateFlow<List<EditorTabState>>(emptyList())
    val tabs = _tabs.asStateFlow()

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex = _selectedTabIndex.asStateFlow()

    init {
        // Start with a default tab containing a sample macro.
        addSampleTab()
    }
    
    private fun addSampleTab() {
        val sampleContent = """
        {
            "events": [
                { "type": "key", "action": "PRESS", "keyName": "Ctrl" },
                { "type": "key", "action": "PRESS", "keyName": "C" },
                { "type": "key", "action": "RELEASE", "keyName": "C" },
                { "type": "key", "action": "RELEASE", "keyName": "Ctrl" },
                { "type": "delay", "durationMs": 100 },
                { "type": "mouse", "action": "MOVE", "x": 500, "y": 300 },
                { "type": "mouse", "action": "CLICK" }
            ]
        }
        """.trimIndent()

        val newTab = EditorTabState(
            title = "Sample Macro",
            content = sampleContent
        )
        _tabs.value = listOf(newTab)
    }

    fun addNewTab() {
        val newTab = EditorTabState(
            title = "New Macro ${tabs.value.size + 1}",
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
        if (index !in tabs.value.indices) return
        _tabs.update { it.toMutableList().apply { removeAt(index) } }
        if (_selectedTabIndex.value >= index && _selectedTabIndex.value > 0) {
            _selectedTabIndex.value--
        } else if (tabs.value.isEmpty()) {
            _selectedTabIndex.value = 0
        }
    }

    fun updateSelectedTabContent(newContent: String) {
        val currentIndex = _selectedTabIndex.value
        if (currentIndex in tabs.value.indices) {
            _tabs.update { currentTabs ->
                currentTabs.toMutableList().also {
                    it[currentIndex] = it[currentIndex].copy(content = newContent)
                }
            }
        }
    }

    fun saveSelectedTab() {
        val currentTab = tabs.value.getOrNull(selectedTabIndex.value)
        if (currentTab != null) {
            println("--- SAVE ACTION ---")
            println("File: ${currentTab.file?.absolutePath ?: "New File"}")
            println("Content: ${currentTab.content}")
            println("--------------------")
        }
    }
    
    fun saveSelectedTabAs() {
        val currentTab = tabs.value.getOrNull(selectedTabIndex.value)
        if (currentTab != null) {
            println("--- SAVE AS ACTION ---")
            println("Content: ${currentTab.content}")
            println("----------------------")
        }
    }
}