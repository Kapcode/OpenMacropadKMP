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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmp.MacroFileState
import com.kapcode.open.macropad.kmp.MacroManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroManagerScreen(viewModel: MacroManagerViewModel) {
    val macroFiles by viewModel.macroFiles.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // --- Toolbar ---
        TopAppBar(
            title = { Text("Macro Manager") },
            actions = {
                Button(onClick = { /* TODO: viewModel.addNewMacro() */ }) {
                    Text("Add")
                }
                Button(onClick = { viewModel.toggleSelectionMode() }) {
                    Text(if (isSelectionMode) "Cancel" else "Select")
                }
                if (isSelectionMode) {
                    Button(
                        onClick = { viewModel.deleteSelectedMacros() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Selected")
                    }
                }
            }
        )

        // --- Macro List ---
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(macroFiles, key = { it.file.absolutePath }) { macroState ->
                MacroItem(
                    state = macroState,
                    isSelectionMode = isSelectionMode,
                    onToggleActive = { isActive -> viewModel.onToggleMacroActive(macroState.file, isActive) },
                    onSelectForDeletion = { isSelected -> viewModel.selectMacroForDeletion(macroState.file, isSelected) },
                    onPlay = { viewModel.onPlayMacro(macroState.file) },
                    onEdit = { viewModel.onEditMacro(macroState.file) },
                    onDelete = { viewModel.onDeleteMacro(macroState.file) }
                )
                Divider()
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
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            Text(state.name, modifier = Modifier.weight(1f))
        }

        Row {
            Button(onClick = onPlay) { Text("Play") }
            Spacer(Modifier.width(4.dp))
            Button(onClick = onEdit) { Text("Edit") }
            Spacer(Modifier.width(4.dp))
            Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Del")
            }
        }
    }
}