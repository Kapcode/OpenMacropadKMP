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
fun ClientSettings(
    clientTheme: String,
    clientAnalyticsEnabled: Boolean,
    clientSlamFireEnabled: Boolean,
    clientSlamFireAction: String,
    settingsViewModel: SettingsViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Connected Clients", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Default Client Theme", style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.selectableGroup()) {
            settingsViewModel.availableThemes.forEach { theme ->
                Row(
                    Modifier
                        .selectable(
                            selected = (theme == clientTheme),
                            onClick = { settingsViewModel.setClientTheme(theme) },
                            role = Role.RadioButton
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = (theme == clientTheme), onClick = null)
                    Text(text = theme, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable Client Analytics", modifier = Modifier.weight(1f))
            Switch(
                checked = clientAnalyticsEnabled,
                onCheckedChange = { settingsViewModel.setClientAnalyticsEnabled(it) }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Slam Fire Trigger", modifier = Modifier.weight(1f))
            Switch(
                checked = clientSlamFireEnabled,
                onCheckedChange = { settingsViewModel.setClientSlamFireEnabled(it) }
            )
        }
        if (clientSlamFireEnabled) {
            OutlinedTextField(
                value = clientSlamFireAction,
                onValueChange = { settingsViewModel.setClientSlamFireAction(it) },
                label = { Text("Slam Fire Action/Button") },
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
