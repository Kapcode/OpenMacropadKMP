package switchdektoptocompose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroManagerScreen(viewModel: MacroManagerViewModel) {
    val macroFiles by viewModel.macroFiles.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Macro Manager") },
            actions = {
                Button(onClick = { /* TODO */ }) { Text("Add") }
                Button(onClick = { viewModel.toggleSelectionMode() }) {
                    Text(if (isSelectionMode) "Cancel" else "Select")
                }
                if (isSelectionMode) {
                    Button(
                        onClick = { viewModel.deleteSelectedMacros() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete Selected") }
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
                HorizontalDivider() // Corrected from Divider
            }
        }
    }
}

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
            Button(onClick = onPlay, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Play") }
            Button(onClick = onEdit, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Edit") }
            Button(
                onClick = onDelete,
                enabled = state.file != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) { Text("Del") }
        }
    }
}