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
fun ThemeSettings(
    selectedTheme: String,
    settingsViewModel: SettingsViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Column(Modifier.selectableGroup()) {
            settingsViewModel.availableThemes.forEach { theme ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (theme == selectedTheme),
                            onClick = { settingsViewModel.selectTheme(theme) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (theme == selectedTheme),
                        onClick = null
                    )
                    Text(
                        text = theme,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}
