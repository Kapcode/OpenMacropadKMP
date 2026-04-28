package com.kapcode.open.macropad.kmps.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A shared component for keyboard input fields that provides live validation
 * and suggestions.
 */
@Composable
fun KeyValidationField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isValidKey: (String) -> Boolean,
    getSuggestions: (String) -> List<String>,
    modifier: Modifier = Modifier,
    placeholder: String = "e.g. F13, CTRL+C"
) {
    var expanded by remember { mutableStateOf(false) }
    val suggestions = remember(value) { getSuggestions(value) }
    
    // Check if the current value is valid (handle multi-key sequences)
    val keys = value.split('+', ',').map { it.trim() }.filter { it.isNotEmpty() }
    val allValid = keys.all { isValidKey(it) }
    val isError = value.isNotEmpty() && !allValid

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { 
                onValueChange(it)
                expanded = it.isNotEmpty() && suggestions.isNotEmpty()
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            isError = isError,
            modifier = Modifier.fillMaxWidth(),
            colors = if (isError) {
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.error,
                    unfocusedBorderColor = MaterialTheme.colorScheme.error,
                    focusedLabelColor = MaterialTheme.colorScheme.error,
                    unfocusedLabelColor = MaterialTheme.colorScheme.error
                )
            } else {
                OutlinedTextFieldDefaults.colors()
            },
            supportingText = {
                if (isError) {
                    Text(
                        "Invalid key name detected. Click the field for suggestions.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        )

        if (expanded && suggestions.isNotEmpty()) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(200.dp)
            ) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            // If it's a multi-key combo, replace the last part
                            val parts = value.split('+', ',').toMutableList()
                            if (parts.isNotEmpty()) {
                                parts[parts.size - 1] = suggestion
                                // Try to detect which delimiter was used
                                val delimiter = if (value.contains('+')) "+" else if (value.contains(',')) "," else ""
                                onValueChange(parts.joinToString(delimiter))
                            } else {
                                onValueChange(suggestion)
                            }
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
