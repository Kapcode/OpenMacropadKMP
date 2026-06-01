package com.kapcode.open.macropad.kmps.desktop.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.kapcode.open.macropad.kmps.desktop.viewmodel.*
import com.kapcode.open.macropad.kmps.desktop.ui.components.AppTooltipArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MacroEditorScreen(viewModel: MacroEditorViewModel, settingsViewModel: SettingsViewModel) {
    val tabs by viewModel.tabs.collectAsState()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()
    val isDark = selectedTheme == "Dark Blue"

    Column {
        // --- Toolbar ---
        TopAppBar(
            title = { Text("Editor") },
            actions = {
                val isTabOpen = tabs.isNotEmpty()
                AppTooltipArea(tooltipText = "Save and Run", delayMillis = 0) {
                    IconButton(onClick = { viewModel.saveAndRunSelectedTab() }, enabled = isTabOpen) {
                        Box {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Save and Run")
                            Icon(
                                Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(10.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraSmall)
                            )
                        }
                    }
                }
                AppTooltipArea(tooltipText = "Save", delayMillis = 0) {
                    IconButton(onClick = { viewModel.saveSelectedTab() }, enabled = isTabOpen) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
                AppTooltipArea(tooltipText = "Save As...", delayMillis = 0) {
                    IconButton(onClick = { viewModel.saveSelectedTabAs() }, enabled = isTabOpen) {
                        Icon(Icons.Default.SaveAs, contentDescription = "Save As")
                    }
                }
                AppTooltipArea(tooltipText = "New Macro", delayMillis = 0) {
                    IconButton(onClick = { viewModel.addNewTab() }) {
                        Icon(Icons.Default.Add, contentDescription = "New Macro")
                    }
                }
            }
        )

        // --- Tab Row ---
        SecondaryTabRow(selectedTabIndex = selectedTabIndex) {
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
                            Text(tab.title + (if (tab.isModified) " •" else ""))
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
                isDark = isDark,
                syntaxStyle = currentTab.syntaxStyle,
                modifier = Modifier.fillMaxSize().padding(8.dp)
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No macros open.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}