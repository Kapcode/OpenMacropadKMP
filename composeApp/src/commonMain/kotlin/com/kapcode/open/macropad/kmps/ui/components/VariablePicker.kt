package com.kapcode.open.macropad.kmps.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun VariablePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    getLiveValue: (String) -> String? = { null },
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    val systemVariables = mapOf(
        "Window" to listOf(
            "current_window_name",
            "last_window_name",
            "current_window_title",
            "last_window_title"
        ),
        "Mouse" to listOf(
            "mouse_x",
            "mouse_y",
            "pixel_color_at_cursor"
        ),
        "Clipboard" to listOf(
            "clipboard_text"
        ),
        "Time" to listOf(
            "current_time_ms"
        )
    )

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.Functions, contentDescription = "Pick Variable")
                }
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(350.dp)
        ) {
            systemVariables.forEach { (category, vars) ->
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                vars.forEach { varName ->
                    DropdownMenuItem(
                        text = { 
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(varName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                
                                // Live Value Preview
                                var livePreview by remember { mutableStateOf(getLiveValue(varName) ?: "...") }
                                if (expanded) {
                                    LaunchedEffect(Unit) {
                                        while(true) {
                                            livePreview = getLiveValue(varName) ?: "N/A"
                                            delay(1000)
                                        }
                                    }
                                }
                                Text(
                                    text = livePreview.take(20) + (if(livePreview.length > 20) ".." else ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.clickable { 
                                        onValueChange(livePreview)
                                        expanded = false
                                    }
                                )
                            }
                        },
                        onClick = {
                            onValueChange(varName)
                            expanded = false
                        }
                    )
                }
                HorizontalDivider()
            }
            
            Text(
                text = "Type any custom name for user variables",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
