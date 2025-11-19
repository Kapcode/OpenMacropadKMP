package switchdektoptocompose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        SelectionContainer {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surface)
            ) {
                items(logMessages) { message ->
                    val color = when {
                        message.startsWith("[Error]") -> MaterialTheme.colorScheme.error
                        message.startsWith("[Warn]") -> Color(0xFFFFA500) // Orange
                        message.startsWith("[Info]") -> MaterialTheme.colorScheme.onSurface
                        message.startsWith("[Debug]") -> Color.Gray
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                }
            }
        }
    }
}

enum class LogLevel {
    Verbose, Debug, Info, Warn, Error
}
