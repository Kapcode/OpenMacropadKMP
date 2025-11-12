package switchdektoptocompose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MacroEditorScreen(viewModel: MacroEditorViewModel) {
    val tabs by viewModel.tabs.collectAsState()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()

    Column {
        // --- Toolbar ---
        TopAppBar(
            title = { Text("Editor") },
            actions = {
                val isTabOpen = tabs.isNotEmpty()
                TooltipArea(tooltip = { Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp){ Text("Save", modifier = Modifier.padding(4.dp)) } }, delayMillis = 0) {
                    IconButton(onClick = { viewModel.saveSelectedTab() }, enabled = isTabOpen) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
                TooltipArea(tooltip = { Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp){ Text("Save As...", modifier = Modifier.padding(4.dp)) } }, delayMillis = 0) {
                    IconButton(onClick = { viewModel.saveSelectedTabAs() }, enabled = isTabOpen) {
                        Icon(Icons.Default.SaveAs, contentDescription = "Save As")
                    }
                }
                TooltipArea(tooltip = { Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp){ Text("New Macro", modifier = Modifier.padding(4.dp)) } }, delayMillis = 0) {
                    IconButton(onClick = { viewModel.addNewTab() }) {
                        Icon(Icons.Default.Add, contentDescription = "New Macro")
                    }
                }
            }
        )

        // --- Tab Row ---
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { viewModel.selectTab(index) },
                    content = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(tab.title)
                            // Prevent closing the last tab
                            if (tabs.size > 1) {
                                IconButton(onClick = { viewModel.closeTab(index) }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Tab")
                                }
                            }
                        }
                    }
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