package switchdektoptocompose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroTimelineScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        // --- Toolbar ---
        TopAppBar(
            title = { Text("Timeline") },
            actions = {
                Button(onClick = { /* TODO: Add event logic */ }) {
                    Text("Add Event")
                }
                Button(onClick = { /* TODO: Record macro logic */ }) {
                    Text("Record Macro")
                }
            }
        )

        // --- Timeline Content Placeholder ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2B2B2B)) // Dark grey
                .padding(8.dp)
        ) {
            Text("Timeline content will go here (e.g., drag-and-drop event items).", color = Color.LightGray)
        }
    }
}