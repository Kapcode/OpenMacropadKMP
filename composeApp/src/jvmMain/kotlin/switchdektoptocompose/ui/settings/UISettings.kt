package switchdektoptocompose.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import switchdektoptocompose.viewmodel.SettingsViewModel

@Composable
fun UISettings(
    tooltipXOffset: Int,
    tooltipYOffset: Int,
    settingsViewModel: SettingsViewModel
) {
    Text("Tooltip Configuration", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "Adjust the horizontal and vertical position of tooltips relative to the cursor.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("X Offset (Horizontal)", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = tooltipXOffset.toFloat(),
                    onValueChange = { settingsViewModel.setTooltipXOffset(it.toInt()) },
                    valueRange = -150f..150f,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = tooltipXOffset.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(40.dp).padding(start = 8.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Y Offset (Vertical)", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = tooltipYOffset.toFloat(),
                    onValueChange = { settingsViewModel.setTooltipYOffset(it.toInt()) },
                    valueRange = -150f..150f,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = tooltipYOffset.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(40.dp).padding(start = 8.dp)
                )
            }
        }
    }
    
    Text(
        "Negative Y moves tooltip UP, Positive moves it DOWN.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

    Text("Window Placement", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "Choose which monitor the application window should open on.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))

    val placementMode by settingsViewModel.windowPlacementMode.collectAsState()
    val monitorIndex by settingsViewModel.windowMonitorIndex.collectAsState()

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RadioButton(selected = placementMode == "PRIMARY", onClick = { settingsViewModel.setWindowPlacementMode("PRIMARY") })
        Text("Primary Monitor")
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RadioButton(selected = placementMode == "CURSOR", onClick = { settingsViewModel.setWindowPlacementMode("CURSOR") })
        Text("Monitor with Cursor")
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RadioButton(selected = placementMode == "INDEX", onClick = { settingsViewModel.setWindowPlacementMode("INDEX") })
        Text("Monitor Index:")
        OutlinedTextField(
            value = monitorIndex.toString(),
            onValueChange = { settingsViewModel.setWindowMonitorIndex(it.toIntOrNull() ?: 0) },
            modifier = Modifier.width(80.dp),
            enabled = placementMode == "INDEX"
        )
    }
}
