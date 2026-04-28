package switchdektoptocompose.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import switchdektoptocompose.viewmodel.SettingsViewModel

@Composable
fun ControllerSettings(
    navigationMode: String,
    settingsViewModel: SettingsViewModel
) {
    Text("Controller Navigation", style = MaterialTheme.typography.titleMedium)
    Text("Configure how your Xbox controller interacts with the application UI.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    
    Spacer(Modifier.height(8.dp))

    Column(Modifier.selectableGroup()) {
        listOf(
            "TRAVERSAL" to "Standard (D-Pad to Tab)",
            "CURSOR" to "Virtual Cursor (Stick to Mouse)"
        ).forEach { (value, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .selectable(
                        selected = (navigationMode == value),
                        onClick = { settingsViewModel.setControllerNavigationMode(value) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (navigationMode == value),
                    onClick = null // Selected by Row
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}
