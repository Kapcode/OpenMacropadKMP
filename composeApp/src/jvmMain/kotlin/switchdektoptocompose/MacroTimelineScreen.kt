package switchdektoptocompose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroTimelineScreen(
    viewModel: MacroTimelineViewModel,
    modifier: Modifier = Modifier
) {
    val events by viewModel.events.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        // --- Toolbar ---
        TopAppBar(
            title = { Text("Timeline") },
            actions = {
                Button(onClick = { /* TODO: viewModel.addEvent(...) */ }) {
                    Text("Add Event")
                }
                Button(onClick = { /* TODO: viewModel.startRecording() */ }) {
                    Text("Record Macro")
                }
            }
        )

        // --- Timeline Content ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(events, key = { it.id }) { event ->
                MacroTimelineItem(event = event)
            }
        }
    }
}