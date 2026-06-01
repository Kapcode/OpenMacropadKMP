package com.kapcode.open.macropad.kmps.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.kapcode.open.macropad.kmps.desktop.model.ActiveProcessInfo
import com.kapcode.open.macropad.kmps.desktop.viewmodel.InspectorViewModel
import com.kapcode.open.macropad.kmps.desktop.viewmodel.MacroManagerViewModel

@Composable
fun InspectorScreen(
    viewModel: InspectorViewModel,
    macroManagerViewModel: MacroManagerViewModel,
    onOpenVariableSettings: () -> Unit
) {
    val focusHistory by viewModel.focusHistory.collectAsState()
    val screenshotOnPress by viewModel.screenshotOnPress.collectAsState()
    val selectedFKey by viewModel.selectedFKey.collectAsState()
    
    val topLeftX by viewModel.topLeftX.collectAsState()
    val topLeftY by viewModel.topLeftY.collectAsState()
    val bottomRightX by viewModel.bottomRightX.collectAsState()
    val bottomRightY by viewModel.bottomRightY.collectAsState()
    val maxScreenshots by viewModel.maxScreenshots.collectAsState()
    val screenshotCount by viewModel.screenshotCount.collectAsState()

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InspectorHeader(Icons.Default.Info, "Live Variables", modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenVariableSettings) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Settings", style = MaterialTheme.typography.labelSmall)
                }
            }
            LiveVariablesSection(macroManagerViewModel)

            HorizontalDivider()

            InspectorHeader(Icons.Default.History, "Window Focus History")
            if (focusHistory.isEmpty()) {
                Text("No focus history yet.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp))
            } else {
                focusHistory.forEach { info ->
                    FocusHistoryItem(info, onCopy = { viewModel.copyToClipboard(it) })
                }
            }

            HorizontalDivider()

            InspectorHeader(Icons.Default.Screenshot, "Automatic Screenshot Capture")
            ScreenshotConfigSection(
                screenshotOnPress = screenshotOnPress,
                onScreenshotToggled = { viewModel.onScreenshotToggled(it) },
                selectedFKey = selectedFKey,
                onFKeySelected = { viewModel.onFKeySelected(it) },
                topLeftX = topLeftX,
                onTopLeftXChanged = { viewModel.onTopLeftXChanged(it) },
                topLeftY = topLeftY,
                onTopLeftYChanged = { viewModel.onTopLeftYChanged(it) },
                bottomRightX = bottomRightX,
                onBottomRightXChanged = { viewModel.onBottomRightXChanged(it) },
                bottomRightY = bottomRightY,
                onBottomRightYChanged = { viewModel.onBottomRightYChanged(it) },
                maxScreenshots = maxScreenshots,
                onMaxScreenshotsChanged = { viewModel.onMaxScreenshotsChanged(it) },
                screenshotCount = screenshotCount,
                onResetCount = { viewModel.resetScreenshotCount() }
            )
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }
}

@Composable
private fun LiveVariablesSection(macroManagerViewModel: MacroManagerViewModel) {
    val groups = listOf(
        "Current Window" to listOf("current_window_title", "current_window_name"),
        "Previous Window" to listOf("last_window_title", "last_window_name"),
        "Mouse & Screen" to listOf("mouse_x", "mouse_y", "pixel_color_at_cursor"),
        "System" to listOf("clipboard_text", "current_time_ms")
    )

    Column(modifier = Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        groups.forEach { (category, vars) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val delayMs = when(category) {
                    "Mouse & Screen" -> macroManagerViewModel.settingsViewModel.mousePollingRate.collectAsState().value
                    "System" -> macroManagerViewModel.settingsViewModel.systemPollingRate.collectAsState().value
                    else -> macroManagerViewModel.settingsViewModel.windowPollingRate.collectAsState().value
                }

                Text(
                    text = category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold
                )
                vars.forEach { varName ->
                    var value by remember { mutableStateOf(macroManagerViewModel.getSystemVariable(varName) ?: "...") }
                    LaunchedEffect(delayMs) {
                        while(true) {
                            value = macroManagerViewModel.getSystemVariable(varName) ?: "N/A"
                            delay(delayMs)
                        }
                    }
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { macroManagerViewModel.setVariable("clipboard_text", value) },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = varName.replace("_", " ").uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectorHeader(icon: ImageVector, title: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(bottom = 4.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FocusHistoryItem(info: ActiveProcessInfo, onCopy: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(info.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { onCopy(info.name) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Process Name", modifier = Modifier.size(14.dp))
                }
            }
            Text("Title: ${info.windowTitle}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
            Text("ID: ${info.windowId} | PID: ${info.pid}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ScreenshotConfigSection(
    screenshotOnPress: Boolean,
    onScreenshotToggled: (Boolean) -> Unit,
    selectedFKey: String,
    onFKeySelected: (String) -> Unit,
    topLeftX: String,
    onTopLeftXChanged: (String) -> Unit,
    topLeftY: String,
    onTopLeftYChanged: (String) -> Unit,
    bottomRightX: String,
    onBottomRightXChanged: (String) -> Unit,
    bottomRightY: String,
    onBottomRightYChanged: (String) -> Unit,
    maxScreenshots: String,
    onMaxScreenshotsChanged: (String) -> Unit,
    screenshotCount: Int,
    onResetCount: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = screenshotOnPress, onCheckedChange = onScreenshotToggled)
            Text("Capture screenshot on trigger")
        }

        if (screenshotOnPress) {
            Text("Trigger Key:", style = MaterialTheme.typography.labelMedium)
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedFKey)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    (1..24).forEach { i ->
                        val key = "F$i"
                        DropdownMenuItem(text = { Text(key) }, onClick = { onFKeySelected(key); expanded = false })
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = topLeftX, onValueChange = onTopLeftXChanged, label = { Text("TL X") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = topLeftY, onValueChange = onTopLeftYChanged, label = { Text("TL Y") }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = bottomRightX, onValueChange = onBottomRightXChanged, label = { Text("BR X") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = bottomRightY, onValueChange = onBottomRightYChanged, label = { Text("BR Y") }, modifier = Modifier.weight(1f))
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = maxScreenshots, onValueChange = onMaxScreenshotsChanged, label = { Text("Max Files") }, modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Captured: $screenshotCount", style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = onResetCount) { Text("Reset") }
                }
            }
        }
    }
}
