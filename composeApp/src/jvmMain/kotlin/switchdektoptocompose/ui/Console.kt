package switchdektoptocompose.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import switchdektoptocompose.model.LogLevel
import switchdektoptocompose.viewmodel.ConsoleViewModel

@Composable
fun Console(
    viewModel: ConsoleViewModel
) {
    val logMessages by viewModel.logMessages.collectAsState()
    val selectedLogLevel by viewModel.logLevel.collectAsState()
    val isAutoScrollEnabled by viewModel.isAutoScrollEnabled.collectAsState()
    val isLoggingToFile by viewModel.isLoggingToFile.collectAsState()
    val selectionStartId by viewModel.selectionStartId.collectAsState()
    val selectionEndId by viewModel.selectionEndId.collectAsState()
    
    var menuExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var containerHeight by remember { mutableStateOf(0) }
    var autoScrollJob by remember { mutableStateOf<Job?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(logMessages.size) {
        if (isAutoScrollEnabled && logMessages.isNotEmpty() && !isDragging) {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (lastVisibleIndex >= logMessages.size - 5 || logMessages.size < 10) {
                listState.animateScrollToItem(logMessages.size - 1)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Log level selector
            Box {
                OutlinedButton(onClick = { menuExpanded = true }) {
                    Text("Level: ${selectedLogLevel.name}")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    LogLevel.entries.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level.name) },
                            onClick = {
                                viewModel.setLogLevel(level)
                                menuExpanded = false
                            }
                        )
                    }
                }
            }

            // Auto-scroll toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isAutoScrollEnabled,
                    onCheckedChange = { viewModel.setAutoScroll(it) }
                )
                Text("Auto-scroll", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.weight(1f))

            if (selectionStartId != null) {
                IconButton(onClick = {
                    val text = viewModel.getSelectedText()
                    if (text.isNotEmpty()) {
                        clipboardManager.setText(AnnotatedString(text))
                    }
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Selection")
                }
                IconButton(onClick = { viewModel.clearSelection() }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Selection")
                }
            }

            // Log to file toggle
            FilterChip(
                selected = isLoggingToFile,
                onClick = { viewModel.toggleLoggingToFile() },
                label = { Text("Log to File") },
                leadingIcon = if (isLoggingToFile) {
                    { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null
            )
        }

        // Log messages
        val selectionRange by remember(logMessages, selectionStartId, selectionEndId) {
            derivedStateOf {
                val start = selectionStartId ?: return@derivedStateOf null
                val end = selectionEndId ?: return@derivedStateOf null
                val startIndex = logMessages.indexOfFirst { it.id == start }
                val endIndex = logMessages.indexOfFirst { it.id == end }
                if (startIndex == -1 || endIndex == -1) null
                else if (startIndex <= endIndex) startIndex..endIndex else endIndex..startIndex
            }
        }

        Box(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .onGloballyPositioned { containerHeight = it.size.height }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        isDragging = event.buttons.isPrimaryPressed
                        
                        if (isDragging) {
                            val change = event.changes.first()
                            val y = change.position.y
                            
                            // Extend selection while dragging
                            val visibleItems = listState.layoutInfo.visibleItemsInfo
                            val draggedOverItem = visibleItems.find { 
                                y >= it.offset && y <= (it.offset + it.size)
                            }
                            
                            draggedOverItem?.let { item ->
                                val entryId = (item.key as? Long)
                                entryId?.let { viewModel.extendSelectionToId(it) }
                            }

                            // Auto-scroll logic
                            val scrollAmount = when {
                                y < 0f -> (y / 2).coerceIn(-150f, -20f) 
                                containerHeight > 0 && y > containerHeight -> ((y - containerHeight) / 2).coerceIn(20f, 150f)
                                else -> 0f
                            }

                            if (scrollAmount != 0f) {
                                if (autoScrollJob == null || autoScrollJob?.isActive != true) {
                                    autoScrollJob = scope.launch {
                                        while (isActive) {
                                            listState.scrollBy(scrollAmount)
                                            
                                            // Extend selection while auto-scrolling
                                            if (scrollAmount < 0) {
                                                listState.layoutInfo.visibleItemsInfo.firstOrNull()?.key?.let {
                                                    if (it is Long) viewModel.extendSelectionToId(it)
                                                }
                                            } else {
                                                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.key?.let {
                                                    if (it is Long) viewModel.extendSelectionToId(it)
                                                }
                                            }
                                            
                                            delay(50)
                                        }
                                    }
                                }
                            } else {
                                autoScrollJob?.cancel()
                                autoScrollJob = null
                            }
                        } else {
                            autoScrollJob?.cancel()
                            autoScrollJob = null
                        }
                    }
                }
            }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                itemsIndexed(logMessages, key = { _, it -> it.id }) { index, entry ->
                    val isSelected = selectionRange?.contains(index) == true

                    val color = when {
                        entry.formatted.contains("[Error]") -> MaterialTheme.colorScheme.error
                        entry.formatted.contains("[Warn]") -> Color(0xFFFFA500)
                        entry.formatted.contains("[Info]") -> MaterialTheme.colorScheme.onSurface
                        entry.formatted.contains("[Debug]") -> Color.Gray
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    
                    Text(
                        text = entry.formatted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                            .pointerInput(entry.id) {
                                awaitPointerEventScope {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    if (event.type == PointerEventType.Press && event.buttons.isPrimaryPressed) {
                                        viewModel.updateSelection(entry.id, event.keyboardModifiers.isShiftPressed)
                                    }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                }
            }
            
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(listState)
            )
        }
    }
}
