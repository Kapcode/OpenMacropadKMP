package switchdektoptocompose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: SettingsViewModel,
    onDismissRequest: () -> Unit
) {
    val macroDirectory by viewModel.macroDirectory.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val dialogState = rememberDialogState(width = 600.dp, height = 400.dp)

    DialogWindow(
        onCloseRequest = onDismissRequest,
        state = dialogState,
        title = "Settings",
        resizable = false
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Macro Directory Setting ---
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Macro Directory")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = macroDirectory,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { viewModel.chooseMacroDirectory() }) {
                            Text("Browse")
                        }
                    }
                }

                // --- Theme Setting ---
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme")
                    var isThemeDropdownExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = isThemeDropdownExpanded,
                        onExpandedChange = { isThemeDropdownExpanded = !isThemeDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedTheme,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isThemeDropdownExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isThemeDropdownExpanded,
                            onDismissRequest = { isThemeDropdownExpanded = false }
                        ) {
                            viewModel.availableThemes.forEach { theme ->
                                DropdownMenuItem(
                                    text = { Text(theme) },
                                    onClick = {
                                        viewModel.selectTheme(theme)
                                        isThemeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f)) // Pushes the close button to the bottom
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}