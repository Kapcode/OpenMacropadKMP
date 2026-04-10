package switchdektoptocompose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import switchdektoptocompose.viewmodel.ConsoleViewModel

@Composable
fun ExitConfirmDialog(
    selectedTheme: String,
    consoleViewModel: ConsoleViewModel,
    onExitNow: () -> Unit,
    onRestart: () -> Unit,
    onDismiss: () -> Unit
) {
    AppDialog(
        onCloseRequest = onDismiss,
        title = "EXIT",
        state = rememberWindowState(width = 400.dp, height = 250.dp),
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel,
        resizable = false
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            Text(
                "Are you sure you want to exit?",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(Modifier.weight(1f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("CANCEL")
                }
                
                Button(
                    onClick = onRestart,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("RESTART")
                }
                
                Button(
                    onClick = onExitNow,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("EXIT NOW")
                }
            }
        }
    }
}
