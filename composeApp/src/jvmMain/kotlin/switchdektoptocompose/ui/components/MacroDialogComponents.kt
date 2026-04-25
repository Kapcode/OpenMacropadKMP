package switchdektoptocompose.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import switchdektoptocompose.model.TriggerType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerTypeDropdown(
    selectedType: TriggerType,
    onTypeSelected: (TriggerType) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Trigger Type:", modifier = Modifier.width(120.dp))
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedType.name,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                TriggerType.entries.forEach { type ->
                    DropdownMenuItem(text = { Text(type.name) }, onClick = {
                        onTypeSelected(type)
                        expanded = false
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClientMultiSelect(
    selectedClients: Set<String>,
    isAllTrusted: Boolean,
    trustedDevices: Map<String, String>,
    onClientsChanged: (Set<String>) -> Unit,
    onAllTrustedChanged: (Boolean) -> Unit
) {
    Column {
        Text("Allowed Clients:", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = isAllTrusted,
                onClick = { onAllTrustedChanged(!isAllTrusted) },
                label = { Text("ALL TRUSTED") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            
            trustedDevices.forEach { (id, name) ->
                val isSelected = selectedClients.contains(id)
                FilterChip(
                    selected = isSelected && !isAllTrusted,
                    enabled = !isAllTrusted,
                    onClick = {
                        if (isSelected) onClientsChanged(selectedClients - id)
                        else onClientsChanged(selectedClients + id)
                    },
                    label = { Text(name) }
                )
            }
            if (trustedDevices.isEmpty()) {
                Text("No trusted devices found.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
