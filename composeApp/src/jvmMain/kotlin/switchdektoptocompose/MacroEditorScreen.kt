package switchdektoptocompose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroEditorScreen(viewModel: MacroEditorViewModel) {
    val tabs by viewModel.tabs.collectAsState()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()

    Column {
        // --- Toolbar ---
        TopAppBar(
            title = { Text("Editor") },
            actions = {
                Button(onClick = { viewModel.addNewTab() }) {
                    Text("Add")
                }
                // Enable Save/Save As only if a tab is open
                val isTabOpen = tabs.isNotEmpty()
                Button(onClick = { viewModel.saveSelectedTab() }, enabled = isTabOpen) {
                    Text("Save")
                }
                Button(onClick = { viewModel.saveSelectedTabAs() }, enabled = isTabOpen) {
                    Text("Save As")
                }
            }
        )

        // --- Tab Row ---
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { viewModel.selectTab(index) },
                    text = { Text(tab.title) },
                )
            }
        }

        // --- Editor Content ---
        val currentTab = tabs.getOrNull(selectedTabIndex)
        if (currentTab != null) {
            SwingCodeEditor(
                text = currentTab.content,
                onTextChange = { newContent ->
                    viewModel.updateSelectedTabContent(newContent)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.weight(1f)) {
                Text("No macros open.")
            }
        }
    }
}