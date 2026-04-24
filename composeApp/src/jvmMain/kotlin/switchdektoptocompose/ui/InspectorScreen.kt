package switchdektoptocompose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import switchdektoptocompose.viewmodel.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import switchdektoptocompose.ui.components.AppTooltipArea

@Composable
fun InspectorScreen(viewModel: InspectorViewModel) {
    val selectedFKey by viewModel.selectedFKey.collectAsState()
    val screenshotOnPress by viewModel.screenshotOnPress.collectAsState()
    val topLeftX by viewModel.topLeftX.collectAsState()
    val topLeftY by viewModel.topLeftY.collectAsState()
    val bottomRightX by viewModel.bottomRightX.collectAsState()
    val bottomRightY by viewModel.bottomRightY.collectAsState()
    
    val focusHistory by viewModel.focusHistory.collectAsState()

    var fKeyMenuExpanded by remember { mutableStateOf(false) }
    val fKeys = (1..12).map { "F$it" }

    Column(
        modifier = Modifier.padding(8.dp).fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Tool Configuration", style = MaterialTheme.typography.titleMedium)
            // F-Key Selector
            Box {
                OutlinedButton(onClick = { fKeyMenuExpanded = true }) {
                    Text("Inspect Key: $selectedFKey")
                }
                DropdownMenu(
                    expanded = fKeyMenuExpanded,
                    onDismissRequest = { fKeyMenuExpanded = false },
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                     Box(modifier = Modifier.sizeIn(maxHeight = 300.dp)) {
                         Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                             fKeys.forEach { key ->
                                 DropdownMenuItem(
                                     text = { Text(key) },
                                     onClick = {
                                         viewModel.onFKeySelected(key)
                                         fKeyMenuExpanded = false
                                     }
                                 )
                             }
                         }
                     }
                }
            }

            // Screenshot Checkbox
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = screenshotOnPress,
                    onCheckedChange = { viewModel.onScreenshotToggled(it) }
                )
                Text("Screenshot on press")
            }

            // Coordinate TextFields
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = topLeftX,
                    onValueChange = { viewModel.onTopLeftXChanged(it) },
                    label = { Text("TL X") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = topLeftY,
                    onValueChange = { viewModel.onTopLeftYChanged(it) },
                    label = { Text("TL Y") },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = bottomRightX,
                    onValueChange = { viewModel.onBottomRightXChanged(it) },
                    label = { Text("BR X") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = bottomRightY,
                    onValueChange = { viewModel.onBottomRightYChanged(it) },
                    label = { Text("BR Y") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Focused Window History", style = MaterialTheme.typography.titleMedium)
        Text(
            "Recent applications focused on this PC. Copy values for Macro Pack activation.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier.weight(1f).padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(focusHistory) { process ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val displayName = if (process.name.contains("java", ignoreCase = true) && process.windowTitle != "N/A") {
                                "[Java] ${process.windowTitle}"
                            } else {
                                process.name
                            }
                            Text(displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("ID: ${process.id} • PID: ${process.pid}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (process.windowTitle != "N/A" && !displayName.contains(process.windowTitle)) {
                                Text("Title: ${process.windowTitle}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Command: ${process.command}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        
                        Row {
                            AppTooltipArea(tooltipText = "Copy Name") {
                                IconButton(onClick = { viewModel.copyToClipboard(process.name) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                }
                            }
                            
                            AppTooltipArea(tooltipText = "Copy Title") {
                                IconButton(onClick = { viewModel.copyToClipboard(process.windowTitle) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                }
                            }

                            AppTooltipArea(tooltipText = "Copy PID") {
                                IconButton(onClick = { viewModel.copyToClipboard(process.pid) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.List, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                    }
                }
            }
            
            if (focusHistory.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No history yet. Focus other windows to populate.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
