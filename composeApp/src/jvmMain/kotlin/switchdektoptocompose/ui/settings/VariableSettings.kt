package switchdektoptocompose.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import switchdektoptocompose.ui.components.AppTooltipArea
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import switchdektoptocompose.viewmodel.SettingsViewModel

@Composable
fun VariableSettings(
    settingsViewModel: SettingsViewModel
) {
    val windowRate by settingsViewModel.windowPollingRate.collectAsState()
    val mouseRate by settingsViewModel.mousePollingRate.collectAsState()
    val systemRate by settingsViewModel.systemPollingRate.collectAsState()

    Text("Variable System Settings", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "Configure how often the system refreshes dynamic data. Faster rates are more responsive but increase CPU load.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))

    // --- Window Polling ---
    AppTooltipArea(
        tooltipText = "Higher speed makes the app react faster when you switch windows, but uses slightly more computer power."
    ) {
        PollingSlider(
            label = "Window Tracking (Process/Title)",
            description = "Affects how fast 'Current Window' and 'Active Window Is' react.",
            value = windowRate,
            onValueChange = { settingsViewModel.setWindowPollingRate(it) },
            range = 50f..5000f
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // --- Mouse Polling ---
    AppTooltipArea(
        tooltipText = "Essential for pixel-perfect automation. Lower values make mouse coordinate tracking much smoother."
    ) {
        PollingSlider(
            label = "Input Tracking (Mouse/Pixel)",
            description = "Affects real-time coordinate variables and pixel color checks.",
            value = mouseRate,
            onValueChange = { settingsViewModel.setMousePollingRate(it) },
            range = 10f..1000f
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // --- System Polling ---
    AppTooltipArea(
        tooltipText = "Determines how often the app checks for things like clipboard changes or background tasks."
    ) {
        PollingSlider(
            label = "System Environment (Clipboard/Time)",
            description = "Affects clipboard change detection and generic state pulses.",
            value = systemRate,
            onValueChange = { settingsViewModel.setSystemPollingRate(it) },
            range = 100f..10000f
        )
    }
}

@Composable
private fun PollingSlider(
    label: String,
    description: String,
    value: Long,
    onValueChange: (Long) -> Unit,
    range: ClosedFloatingPointRange<Float>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toLong()) },
                valueRange = range,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Text(
                    text = "${value}ms",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).widthIn(min = 50.dp)
                )
            }
        }
    }
}
