package switchdektoptocompose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Console(
    viewModel: ConsoleViewModel
) {
    val logMessages by viewModel.logMessages.collectAsState()
    val selectedLogLevel by viewModel.logLevel.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Log level selector
        Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            OutlinedButton(onClick = { menuExpanded = true }) {
                Text("Log Level: ${selectedLogLevel.name}")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                LogLevel.values().forEach { level ->
                    DropdownMenuItem(
                        text = { Text(level.name) },
                        onClick = {
                            viewModel.setLogLevel(level)
                            menuExpanded = false
                        }
                    )
                }
            }
        }

        // Log messages
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surface)
        ) {
            items(logMessages) { message ->
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

enum class LogLevel {
    Verbose, Debug, Info, Warn, Error
}
