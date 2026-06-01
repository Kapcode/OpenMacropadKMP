package com.kapcode.open.macropad.kmps.desktop.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.*
import com.kapcode.open.macropad.kmps.desktop.viewmodel.*
import com.kapcode.open.macropad.kmps.desktop.model.*
import com.kapcode.open.macropad.kmps.desktop.ui.components.AppTooltipArea
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    onRecordMacroClicked: () -> Unit,
    onEditEventClicked: (MacroEventState, Int) -> Unit,
    onEditTriggerClicked: (TriggerState) -> Unit,
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
                AppTooltipArea(
                    tooltipText = "Add Event",
                    delayMillis = 0
                ) {
                    IconButton(onClick = onAddEventClicked) {
                        Icon(Icons.Default.Add, contentDescription = "Add Event")
                    }
                }

                AppTooltipArea(
                    tooltipText = "Record Macro",
                    delayMillis = 0
                ) {
                    IconButton(onClick = onRecordMacroClicked) {
                        Icon(Icons.Default.RadioButtonChecked, contentDescription = "Record Macro")
                    }
                }
            }
        )

        // --- Trigger Event Display ---
        triggerEvent?.let { trigger ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 4.dp
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "TRIGGER",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onEditTriggerClicked(trigger) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Trigger", modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { viewModel.deleteTrigger() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Trigger", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Display the trigger details using specialized text instead of a generic KeyEvent
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(trigger.triggerType.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(trigger.keyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            }
                            
                            val details = when(trigger.triggerType) {
                                TriggerType.HOLD -> "${trigger.holdDurationMs}ms Hold"
                                TriggerType.MULTI_TAP -> "${trigger.multiTapCount}x Tap (${trigger.tapWindowMs}ms window)"
                                TriggerType.SEQUENCE -> "Sequence (${trigger.sequenceWindowMs}ms window)"
                                else -> "On Release"
                            }
                            Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            
                            if (trigger.allowedClients.isNotBlank()) {
                                Text(
                                    "Clients: ${trigger.allowedClients.replace("_", " ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Draggable Events List ---
        ReorderContainer(state = reorderState) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            modifier = Modifier.graphicsLayer { alpha = if (isDragging) 0f else 1f },
                            onEdit = { onEditEventClicked(event, index) },
                            onDelete = { viewModel.deleteEvent(index) }
                        )
                    }
                }
            }
        }
    }
}