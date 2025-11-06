package switchdektoptocompose

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * Represents the state of a single editor tab.
 * @param title The title displayed on the tab.
 * @param content The text content of the editor for this tab.
 * @param file The backing file for this tab, if it exists.
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
        // Start with one default, unsaved tab.
        addNewTab()
    }

    fun addNewTab() {
        val newTab = EditorTabState(
            title = "New Macro ${tabs.value.size + 1}",
            content = "{\n    \"events\": []\n}"
        )
        _tabs.update { it + newTab }
        // Automatically select the newly added tab.
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

        // Adjust selected index if necessary
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
    
    // Future functions to be added:
    // fun saveSelectedTab() { ... }
    // fun renameSelectedTab(newTitle: String) { ... }
}