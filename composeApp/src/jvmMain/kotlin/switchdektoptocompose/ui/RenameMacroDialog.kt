package switchdektoptocompose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme
import switchdektoptocompose.viewmodel.ConsoleViewModel

@Composable
fun RenameMacroDialog(
    currentName: String,
    selectedTheme: String,
    consoleViewModel: ConsoleViewModel,
    onDismissRequest: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AppDialog(
        onCloseRequest = onDismissRequest,
        state = rememberWindowState(width = 400.dp, height = 250.dp),
        title = "Rename Macro",
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Renaming '$currentName' to:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onDismissRequest) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    onRename(newName)
                    onDismissRequest() // Close dialog on confirm
                }) {
                    Text("OK")
                }
            }
        }
    }
}
