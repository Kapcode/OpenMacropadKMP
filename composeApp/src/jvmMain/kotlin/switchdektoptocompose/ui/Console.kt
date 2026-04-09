package switchdektoptocompose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    
    var menuExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(logMessages.size) {
        if (isAutoScrollEnabled && logMessages.isNotEmpty()) {
            listState.animateScrollToItem(logMessages.size - 1)
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
        SelectionContainer {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surface)
            ) {
                items(logMessages) { message ->
                    val color = when {
                        message.contains("[Error]") -> MaterialTheme.colorScheme.error
                        message.contains("[Warn]") -> Color(0xFFFFA500) // Orange
                        message.contains("[Info]") -> MaterialTheme.colorScheme.onSurface
                        message.contains("[Debug]") -> Color.Gray
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                }
            }
        }
    }
}
