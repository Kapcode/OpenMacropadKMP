package switchdektoptocompose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MacroManagerScreen(viewModel: MacroManagerViewModel) {
    val macroFiles by viewModel.macroFiles.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Macro Manager") },
            actions = {
                TooltipArea(tooltip = { Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp){ Text("New Macro", modifier = Modifier.padding(4.dp)) } }, delayMillis = 0) {
                    IconButton(onClick = { /* TODO: Implement New Macro Action */ }) {
                        Icon(Icons.Default.Add, contentDescription = "New Macro")
                    }
                }
                TooltipArea(tooltip = { Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp){ Text(if (isSelectionMode) "Cancel Selection" else "Select Macros", modifier = Modifier.padding(4.dp)) } }, delayMillis = 0) {
                    IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                        if (isSelectionMode) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancel Selection")
                        } else {
                            Icon(Icons.Default.CheckBox, contentDescription = "Select Macros")
                        }
                    }
                }
                if (isSelectionMode) {
                    TooltipArea(tooltip = { Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp){ Text("Delete Selected", modifier = Modifier.padding(4.dp)) } }, delayMillis = 0) {
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
            items(macroFiles, key = { it.id }) { macroState ->
                MacroItem(
                    state = macroState,
                    isSelectionMode = isSelectionMode,
                    onToggleActive = { isActive -> viewModel.onToggleMacroActive(macroState.id, isActive) },
                    onSelectForDeletion = { isSelected -> viewModel.selectMacroForDeletion(macroState.id, isSelected) },
                    onPlay = { viewModel.onPlayMacro(macroState) },
                    onEdit = { viewModel.onEditMacro(macroState) },
                    onDelete = { viewModel.onDeleteMacro(macroState) }
                )
                Divider()
            }
        }
    }
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
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = state.isSelectedForDeletion,
                    onCheckedChange = onSelectForDeletion
                )
            }
            Switch(
                checked = state.isActive,
                onCheckedChange = onToggleActive
            )
            Spacer(Modifier.width(8.dp))
            Text(state.name, maxLines = 1)
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TooltipArea(tooltip = { Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp){ Text("Play Macro", modifier = Modifier.padding(4.dp)) } }, delayMillis = 0) {
                IconButton(onClick = onPlay) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play Macro")
                }
            }
            TooltipArea(tooltip = { Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp){ Text("Edit Macro", modifier = Modifier.padding(4.dp)) } }, delayMillis = 0) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Macro")
                }
            }
            TooltipArea(tooltip = { Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp){ Text("Delete Macro", modifier = Modifier.padding(4.dp)) } }, delayMillis = 0) {
                IconButton(onClick = onDelete, enabled = state.file != null) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Macro", tint = if (state.file != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                }
            }
        }
    }
}