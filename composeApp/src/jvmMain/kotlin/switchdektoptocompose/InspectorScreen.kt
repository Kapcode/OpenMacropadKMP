package switchdektoptocompose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InspectorScreen(viewModel: InspectorViewModel) {
    val selectedFKey by viewModel.selectedFKey.collectAsState()
    val screenshotOnPress by viewModel.screenshotOnPress.collectAsState()
    val topLeftX by viewModel.topLeftX.collectAsState()
    val topLeftY by viewModel.topLeftY.collectAsState()
    val bottomRightX by viewModel.bottomRightX.collectAsState()
    val bottomRightY by viewModel.bottomRightY.collectAsState()

    var fKeyMenuExpanded by remember { mutableStateOf(false) }
    val fKeys = (1..12).map { "F$it" }

    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // F-Key Selector
        Box {
            OutlinedButton(onClick = { fKeyMenuExpanded = true }) {
                Text("Inspect Key: $selectedFKey")
            }
            DropdownMenu(
                expanded = fKeyMenuExpanded,
                onDismissRequest = { fKeyMenuExpanded = false }
            ) {
                fKeys.forEach { key ->
                    DropdownMenuItem(
                        text = { Text(key) },
                        onClick = {
                            viewModel.onFKeySelected(key)
                            fKeyMenuExpanded = false
                        }
                    )
                }
            }
        }

        // Screenshot Checkbox
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = screenshotOnPress,
                onCheckedChange = { viewModel.onScreenshotToggled(it) }
            )
            Text("Screenshot on press")
        }

        // Coordinate TextFields
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = topLeftX,
                onValueChange = { viewModel.onTopLeftXChanged(it) },
                label = { Text("TL X") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = topLeftY,
                onValueChange = { viewModel.onTopLeftYChanged(it) },
                label = { Text("TL Y") },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = bottomRightX,
                onValueChange = { viewModel.onBottomRightXChanged(it) },
                label = { Text("BR X") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = bottomRightY,
                onValueChange = { viewModel.onBottomRightYChanged(it) },
                label = { Text("BR Y") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}