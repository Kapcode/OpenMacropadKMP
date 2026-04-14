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
fun BehaviorSettings(
    exitBehavior: String,
    clickTrayToToggle: Boolean,
    settingsViewModel: SettingsViewModel,
    onShowShortcutsRequest: () -> Unit
) {
    Text("App Exit Behavior", style = MaterialTheme.typography.titleMedium)
    Column(Modifier.selectableGroup()) {
        listOf(
            "ASK" to "Ask every time",
            "TRAY" to "Exit to system tray",
            "EXIT" to "Just exit the application"
        ).forEach { (value, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .selectable(
                        selected = (exitBehavior == value),
                        onClick = { settingsViewModel.setExitBehavior(value) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (exitBehavior == value),
                    onClick = null
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Click tray icon to show/hide window", modifier = Modifier.weight(1f))
        Checkbox(
            checked = clickTrayToToggle,
            onCheckedChange = { settingsViewModel.setClickTrayToToggle(it) }
        )
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    // --- Shortcuts Section ---
    Text("Shortcuts & Keymap", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = onShowShortcutsRequest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Configure Global & App Shortcuts")
    }
}
