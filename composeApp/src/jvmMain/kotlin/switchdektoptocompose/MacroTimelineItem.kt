package switchdektoptocompose

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MacroTimelineItem(
    event: MacroEventState,
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        when (event) {
            is MacroEventState.KeyEvent -> KeyItem(event)
            is MacroEventState.MouseEvent -> MouseItem(event)
            is MacroEventState.DelayEvent -> DelayItem(event)
            is MacroEventState.SetAutoWaitEvent -> SetAutoWaitItem(event) // Added the missing branch
        }
    }
}

// ... (KeyItem, MouseItem, and DelayItem are unchanged)

@Composable
private fun KeyItem(event: MacroEventState.KeyEvent) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("KEY", fontWeight = FontWeight.Bold, color = Color(0xFF6897BB), fontSize = 12.sp)
        Spacer(Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .border(1.dp, Color.Gray, shape = MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(event.keyName)
        }
        Spacer(Modifier.width(8.dp))
        Text(event.action.name, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MouseItem(event: MacroEventState.MouseEvent) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("MOUSE", fontWeight = FontWeight.Bold, color = Color(0xFFB568BB), fontSize = 12.sp)
        Spacer(Modifier.width(16.dp))
        Text("Action: ${event.action.name}", style = MaterialTheme.typography.labelMedium)
        if (event.action == MouseAction.MOVE) {
            Spacer(Modifier.width(8.dp))
            Text("(${event.x}, ${event.y})")
        }
    }
}

@Composable
private fun DelayItem(event: MacroEventState.DelayEvent) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("DELAY", fontWeight = FontWeight.Bold, color = Color(0xFF6AAB73), fontSize = 12.sp)
        Spacer(Modifier.width(16.dp))
        Text("${event.durationMs} ms", style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * A new Composable to display the SetAutoWaitEvent.
 */
@Composable
private fun SetAutoWaitItem(event: MacroEventState.SetAutoWaitEvent) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("AUTO DELAY", fontWeight = FontWeight.Bold, color = Color(0xFFDDAA77), fontSize = 12.sp)
        Spacer(Modifier.width(16.dp))
        Text("Set automatic delay to ${event.delayMs} ms", style = MaterialTheme.typography.bodyMedium)
    }
}