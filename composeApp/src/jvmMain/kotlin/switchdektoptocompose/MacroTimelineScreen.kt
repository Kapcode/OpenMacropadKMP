package switchdektoptocompose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.compose.dnd.reorder.ReorderContainer
import com.mohamedrejeb.compose.dnd.reorder.ReorderableItem
import com.mohamedrejeb.compose.dnd.reorder.rememberReorderState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroTimelineScreen(
    viewModel: MacroTimelineViewModel,
    modifier: Modifier = Modifier
) {
    val events by viewModel.events.collectAsState()
    val reorderState = rememberReorderState<MacroEventState>()
    val lazyListState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Timeline") },
            actions = {
                Button(onClick = { /* TODO */ }) { Text("Add Event") }
                Button(onClick = { /* TODO */ }) { Text("Record Macro") }
            }
        )

        ReorderContainer(state = reorderState) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(events, key = { _, event -> event.id }) { index, event ->
                    ReorderableItem(
                        state = reorderState,
                        key = event.id,
                        data = event,
                        onDragEnter = { state ->
                            val from = events.indexOfFirst { it.id == state.data.id }
                            if (from != -1) {
                                viewModel.moveEvent(from, index)
                            }
                        },
                        draggableContent = {
                            MacroTimelineItem(
                                event = event,
                                isDragging = true
                            )
                        }
                    ) { // Corrected: The content lambda has no explicit parameters.
                        // 'isDragging' is a property of the receiver scope (ReorderableItemScope).
                        MacroTimelineItem(
                            event = event,
                            isDragging = isDragging, // Use the implicit 'isDragging' property
                            modifier = Modifier.graphicsLayer { alpha = if (isDragging) 0f else 1f }
                        )
                    }
                }
            }
        }
    }
}