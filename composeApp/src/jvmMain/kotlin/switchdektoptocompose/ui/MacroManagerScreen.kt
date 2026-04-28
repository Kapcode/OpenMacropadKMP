package switchdektoptocompose.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import switchdektoptocompose.viewmodel.*
import switchdektoptocompose.model.*
import com.kapcode.open.macropad.kmps.models.MacroPack
import switchdektoptocompose.ui.components.AppTooltipArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MacroManagerScreen(
    viewModel: MacroManagerViewModel,
    consoleViewModel: ConsoleViewModel,
    selectedTheme: String,
    onNewMacroClicked: () -> Unit,
    onMarketplaceClicked: () -> Unit
) {
    val macroFiles by viewModel.macroFiles.collectAsState()
    val macroPacks by viewModel.macroPacks.collectAsState()
    
    val isMacroSelectionMode by viewModel.isMacroSelectionMode.collectAsState()
    val isPackSelectionMode by viewModel.isPackSelectionMode.collectAsState()
    val macroSearchQuery by viewModel.macroSearchQuery.collectAsState()
    val packSearchQuery by viewModel.packSearchQuery.collectAsState()
    val isMacrosCollapsed by viewModel.isMacrosCollapsed.collectAsState()
    val isPacksCollapsed by viewModel.isPacksCollapsed.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    val currentActiveProcess by viewModel.currentActiveProcess.collectAsState()
    val macroBeingRenamed by viewModel.macroBeingRenamed.collectAsState()
    val packBeingEdited by viewModel.packBeingEdited.collectAsState()

    val isDark = selectedTheme == "Dark Blue"
    val headerColor = if (isDark) Color.Black else Color.White

    macroBeingRenamed?.let { macro ->
        RenameMacroDialog(
            currentName = macro.name,
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            onDismissRequest = { viewModel.cancelRename() },
            onRename = { newName -> viewModel.confirmRename(newName) }
        )
    }

    packBeingEdited?.let { pack ->
        PackEditorDialog(
            pack = pack,
            macroManagerViewModel = viewModel,
            availableMacros = macroFiles,
            onDismissRequest = { viewModel.onCancelPackEdit() },
            onSave = { updatedPack -> viewModel.onSavePack(updatedPack) },
            onOpenInJsonEditor = { packToOpen -> viewModel.onOpenPackInJsonEditor(packToOpen) }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Packs Manager Section
            item {
                SectionHeader(
                    title = "Packs Manager",
                    searchQuery = packSearchQuery,
                    onSearchQueryChange = { viewModel.onPackSearchQueryChange(it) },
                    isCollapsed = isPacksCollapsed,
                    onToggleCollapse = { viewModel.togglePacksCollapsed() },
                    isSelectionMode = isPackSelectionMode,
                    onToggleSelectionMode = { viewModel.togglePackSelectionMode() },
                    onDeleteSelected = { viewModel.deleteSelectedPacks() },
                    backgroundColor = headerColor,
                    actions = {
                        AppTooltipArea(tooltipText = "Marketplace", delayMillis = 0) {
                            IconButton(onClick = onMarketplaceClicked) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = "Marketplace")
                            }
                        }
                        AppTooltipArea(tooltipText = "New Pack", delayMillis = 0) {
                            IconButton(onClick = { viewModel.onCreatePack() }) {
                                Icon(Icons.Default.PostAdd, contentDescription = "New Pack")
                            }
                        }
                    }
                )
            }
            
            if (!isPacksCollapsed) {
                items(macroPacks, key = { "pack_${it.pack.id}_${it.file?.absolutePath}" }) { packState ->
                    val isLiveActive = packState.pack.targetProcess != null && 
                                     packState.pack.targetProcess.equals(currentActiveProcess, ignoreCase = true)
                    
                    PackItem(
                        pack = packState.pack,
                        isSelectionMode = isPackSelectionMode,
                        isSelected = uiState.selectedPackIds.contains(packState.pack.id),
                        isLiveActive = isLiveActive,
                        onToggleActive = { viewModel.onToggleMacroActive(packState.pack.id, it) },
                        onToggleSelection = { viewModel.togglePackSelection(packState.pack.id, it) },
                        onEdit = { viewModel.onEditPack(packState.pack) },
                        onDelete = { viewModel.onDeletePack(packState.pack) }
                    )
                    HorizontalDivider()
                }
            }

            // Macro Manager Section
            item {
                SectionHeader(
                    title = "Macro Manager",
                    searchQuery = macroSearchQuery,
                    onSearchQueryChange = { viewModel.onMacroSearchQueryChange(it) },
                    isCollapsed = isMacrosCollapsed,
                    onToggleCollapse = { viewModel.toggleMacrosCollapsed() },
                    isSelectionMode = isMacroSelectionMode,
                    onToggleSelectionMode = { viewModel.toggleMacroSelectionMode() },
                    onDeleteSelected = { viewModel.deleteSelectedMacros() },
                    backgroundColor = headerColor,
                    actions = {
                        AppTooltipArea(tooltipText = "New Macro", delayMillis = 0) {
                            IconButton(onClick = onNewMacroClicked) {
                                Icon(Icons.Default.Add, contentDescription = "New Macro")
                            }
                        }
                    }
                )
            }

            if (!isMacrosCollapsed) {
                items(macroFiles, key = { it.id }) { macroState ->
                    MacroItem(
                        state = macroState,
                        isSelectionMode = isMacroSelectionMode,
                        onToggleActive = { isActive -> viewModel.onToggleMacroActive(macroState.id, isActive) },
                        onSelectForDeletion = { isSelected -> viewModel.selectMacroForDeletion(macroState.id, isSelected) },
                        onPlay = { viewModel.onPlayMacro(macroState) },
                        onEdit = { viewModel.onEditMacro(macroState) },
                        onRename = { viewModel.onRenameMacro(macroState) },
                        onDelete = { viewModel.onDeleteMacro(macroState) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    isSelectionMode: Boolean,
    onToggleSelectionMode: () -> Unit,
    onDeleteSelected: (() -> Unit)? = null,
    backgroundColor: Color,
    actions: @Composable RowScope.() -> Unit
) {
    Surface(
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                IconButton(onClick = onToggleCollapse, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (isCollapsed) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isCollapsed) "Expand" else "Collapse"
                    )
                }
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    actions()
                    AppTooltipArea(tooltipText = if (isSelectionMode) "Cancel Selection" else "Select Items", delayMillis = 0) {
                        IconButton(onClick = onToggleSelectionMode) {
                            if (isSelectionMode) {
                                Icon(Icons.Default.Cancel, contentDescription = "Cancel Selection")
                            } else {
                                Icon(Icons.Default.CheckBox, contentDescription = "Select Items")
                            }
                        }
                    }
                    if (isSelectionMode && onDeleteSelected != null) {
                        AppTooltipArea(tooltipText = "Delete Selected", delayMillis = 0) {
                            IconButton(onClick = onDeleteSelected) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                placeholder = { Text("Search $title...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                } else null,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                shape = MaterialTheme.shapes.small
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PackItem(
    pack: MacroPack,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isLiveActive: Boolean,
    onToggleActive: (Boolean) -> Unit,
    onToggleSelection: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pack.name, maxLines = 1, modifier = Modifier.weight(1f))
                AppTooltipArea(
                    tooltipText = if (pack.isActive) 
                        "AUTO-SWITCH ENABLED: This pack can activate when you use '${pack.targetProcess ?: "its target app"}'." 
                        else "AUTO-SWITCH DISABLED: This pack will never activate on your phone."
                ) {
                    Switch(
                        checked = pack.isActive,
                        onCheckedChange = onToggleActive,
                        modifier = Modifier.scale(0.7f).padding(0.dp)
                    )
                }
            }
        },
        supportingContent = {
            Column {
                Text("${pack.widgets.size} Widgets", maxLines = 1)
                if (!pack.targetProcess.isNullOrBlank()) {
                    Text("Auto-switching: ${pack.targetProcess}", style = MaterialTheme.typography.bodySmall)
                }
                AppTooltipArea(
                    tooltipText = if (isLiveActive) 
                        "ACTIVE: This pack is currently displayed on your phone because the target app is focused." 
                        else "INACTIVE: This pack is hidden because its target app is not focused or auto-switching is off."
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusText = if (isLiveActive) "(Activated)" else "(Not Activated)"
                        val statusColor = if (isLiveActive) Color(0xFF00C853) else MaterialTheme.colorScheme.error
                        Text(
                            text = statusText,
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        },
        leadingContent = {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onToggleSelection
                )
            } else {
                Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Pack")
            }
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppTooltipArea(tooltipText = "Edit Pack", delayMillis = 0) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Pack")
                    }
                }
                AppTooltipArea(tooltipText = "Delete Pack", delayMillis = 0) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Pack", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MacroItem(
    state: MacroFileState,
    isSelectionMode: Boolean,
    onToggleActive: (Boolean) -> Unit,
    onSelectForDeletion: (Boolean) -> Unit,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(state.name, maxLines = 1)
        },
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = state.isSelectedForDeletion,
                        onCheckedChange = onSelectForDeletion
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Switch(
                    checked = state.isActive,
                    onCheckedChange = onToggleActive
                )
            }
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppTooltipArea(tooltipText = "Play Macro", delayMillis = 0) {
                    IconButton(onClick = onPlay) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play Macro")
                    }
                }
                AppTooltipArea(tooltipText = "Edit Macro", delayMillis = 0) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Macro")
                    }
                }
                AppTooltipArea(tooltipText = "Rename Macro", delayMillis = 0) {
                    IconButton(onClick = onRename, enabled = state.file != null) {
                        Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Rename Macro")
                    }
                }
                AppTooltipArea(tooltipText = "Delete Macro", delayMillis = 0) {
                    IconButton(onClick = onDelete, enabled = state.file != null) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Macro", tint = if (state.file != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                    }
                }
            }
        }
    )
}
