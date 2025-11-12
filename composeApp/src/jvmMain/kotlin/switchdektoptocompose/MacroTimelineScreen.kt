package switchdektoptocompose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.compose.dnd.reorder.ReorderContainer
import com.mohamedrejeb.compose.dnd.reorder.ReorderableItem
import com.mohamedrejeb.compose.dnd.reorder.rememberReorderState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
                TooltipArea(
                    tooltip = {
                        Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp) {
                            Text("Add Event", modifier = Modifier.padding(4.dp))
                        }
                    },
                    delayMillis = 0
                ) {
                    IconButton(onClick = onAddEventClicked) {
                        Icon(Icons.Default.Add, contentDescription = "Add Event")
                    }
                }

                TooltipArea(
                    tooltip = {
                        Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp) {
                            Text("Record Macro", modifier = Modifier.padding(4.dp))
                        }
                    },
                    delayMillis = 0
                ) {
                    IconButton(onClick = { /* TODO */ }, enabled = false) {
                        Icon(Icons.Default.RadioButtonChecked, contentDescription = "Record Macro")
                    }
                }
            }
        )

        // --- Trigger Event Display ---
        triggerEvent?.let { trigger ->
            Column(Modifier.background(MaterialTheme.colorScheme.secondaryContainer).padding(8.dp)) {
                Text(
                    "TRIGGER",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                // Convert the TriggerState to a displayable KeyEvent
                val displayEvent = MacroEventState.KeyEvent(
                    keyName = trigger.keyName,
                    action = KeyAction.RELEASE // Triggers are always RELEASE actions
                )
                MacroTimelineItem(event = displayEvent, isDragging = false)
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