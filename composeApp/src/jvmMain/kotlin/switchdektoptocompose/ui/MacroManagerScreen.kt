package switchdektoptocompose.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val macroBeingRenamed by viewModel.macroBeingRenamed.collectAsState()
    val packBeingEdited by viewModel.packBeingEdited.collectAsState()

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
            availableMacros = macroFiles,
            onDismissRequest = { viewModel.onCancelPackEdit() },
            onSave = { updatedPack -> viewModel.onSavePack(updatedPack) },
            onOpenInJsonEditor = { packToOpen -> viewModel.onOpenPackInJsonEditor(packToOpen) }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Macro Manager") },
            actions = {
                AppTooltipArea(tooltipText = "Marketplace", delayMillis = 0) {
                    IconButton(onClick = onMarketplaceClicked) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = "Marketplace")
                    }
                }
                AppTooltipArea(tooltipText = "New Pack", delayMillis = 0) {
                    IconButton(onClick = { viewModel.onCreatePack() }) {
                        Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "New Pack")
                    }
                }
                AppTooltipArea(tooltipText = "New Macro", delayMillis = 0) {
                    IconButton(onClick = onNewMacroClicked) {
                        Icon(Icons.Default.Add, contentDescription = "New Macro")
                    }
                }
                AppTooltipArea(tooltipText = if (isSelectionMode) "Cancel Selection" else "Select Macros", delayMillis = 0) {
                    IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                        if (isSelectionMode) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancel Selection")
                        } else {
                            Icon(Icons.Default.CheckBox, contentDescription = "Select Macros")
                        }
                    }
                }
                if (isSelectionMode) {
                    AppTooltipArea(tooltipText = "Delete Selected", delayMillis = 0) {
                        IconButton(
                            onClick = { viewModel.deleteSelectedMacros() }
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Text("Macro Packs", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(8.dp))
            }
            items(macroPacks, key = { "pack_${it.id}" }) { pack ->
                PackItem(
                    pack = pack,
                    onEdit = { viewModel.onEditPack(pack) },
                    onDelete = { viewModel.onDeletePack(pack) }
                )
                HorizontalDivider()
            }
            
            item {
                Text("Macros", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(8.dp))
            }
            items(macroFiles, key = { it.id }) { macroState ->
                MacroItem(
                    state = macroState,
                    isSelectionMode = isSelectionMode,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PackItem(
    pack: MacroPack,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(pack.name, maxLines = 1)
        },
        supportingContent = {
            Text("${pack.widgets.size} Widgets${if (pack.isActive) " • Active" else ""}", maxLines = 1)
        },
        leadingContent = {
            Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Pack")
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
