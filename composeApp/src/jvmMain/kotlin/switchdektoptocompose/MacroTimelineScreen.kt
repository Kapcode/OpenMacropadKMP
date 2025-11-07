package switchdektoptocompose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.compose.dnd.reorder.ReorderContainer
import com.mohamedrejeb.compose.dnd.reorder.ReorderableItem
import com.mohamedrejeb.compose.dnd.reorder.rememberReorderState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroTimelineScreen(
    viewModel: MacroTimelineViewModel,
    onAddEventClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val triggerEvent by viewModel.triggerEvent.collectAsState()
    val events by viewModel.events.collectAsState()
    val reorderState = rememberReorderState<MacroEventState>()
    val lazyListState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Timeline") },
            actions = {
                Button(onClick = onAddEventClicked) { Text("Add Event") }
                Button(onClick = { /* TODO */ }) { Text("Record Macro") }
            }
        )

        // --- Trigger Event Display ---
        triggerEvent?.let {
            Column(Modifier.background(MaterialTheme.colorScheme.secondaryContainer).padding(8.dp)) {
                Text("TRIGGER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                MacroTimelineItem(event = it, isDragging = false)
            }
        }

        // --- Draggable Events List ---
        ReorderContainer(state = reorderState) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                itemsIndexed(events, key = { _, event -> event.id }) { index, event ->
                    ReorderableItem(
                        state = reorderState,
                        key = event.id,
                        data = event,
                        onDragEnter = { state ->
                            val from = events.indexOfFirst { it.id == state.data.id }
                            if (from != -1) viewModel.moveEvent(from, index)
                        },
                        draggableContent = {
                            MacroTimelineItem(event = event, isDragging = true)
                        }
                    ) {
                        MacroTimelineItem(
                            event = event,
                            isDragging = isDragging,
                            modifier = Modifier.graphicsLayer { alpha = if (isDragging) 0f else 1f }
                        )
                    }
                }
            }
        }
    }
}