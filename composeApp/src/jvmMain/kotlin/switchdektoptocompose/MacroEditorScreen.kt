package switchdektoptocompose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun MacroEditorScreen(viewModel: MacroEditorViewModel) {
    val tabs by viewModel.tabs.collectAsState()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()

    Column {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { viewModel.selectTab(index) },
                    text = { Text(tab.title) },
                )
            }
        }

        val currentTab = tabs.getOrNull(selectedTabIndex)
        if (currentTab != null) {
            // Replace the placeholder Box with our actual SwingCodeEditor
            SwingCodeEditor(
                text = currentTab.content,
                onTextChange = { newContent ->
                    viewModel.updateSelectedTabContent(newContent)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Show a message if no tabs are open
            Box(modifier = Modifier.weight(1f)) {
                Text("No macros open.")
            }
        }
    }
}