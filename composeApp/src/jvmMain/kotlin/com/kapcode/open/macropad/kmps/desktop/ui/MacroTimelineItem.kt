package com.kapcode.open.macropad.kmps.desktop.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.kapcode.open.macropad.kmps.desktop.model.*
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
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (event) {
                    is MacroEventState.KeyEvent -> KeyItem(event)
                    is MacroEventState.MouseEvent -> MouseItem(event)
                    is MacroEventState.MouseButtonEvent -> MouseButtonItem(event)
                    is MacroEventState.ScrollEvent -> ScrollItem(event)
                    is MacroEventState.DelayEvent -> DelayItem(event)
                    is MacroEventState.SetAutoWaitEvent -> SetAutoWaitItem(event)
                    is MacroEventState.ScriptEvent -> ScriptItem(event)
                }
            }
            
            if (onEdit != null || onDelete != null) {
                Row(
                    modifier = Modifier.padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    onEdit?.let {
                        IconButton(onClick = it, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        }
                    }
                    onDelete?.let {
                        IconButton(onClick = it, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyItem(event: MacroEventState.KeyEvent) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("KEY", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        Spacer(Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outline, shape = MaterialTheme.shapes.small)
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
        Text("MOUSE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
        Spacer(Modifier.width(16.dp))
        Text("Action: ${event.action.name}", style = MaterialTheme.typography.labelMedium)
        if (event.action == MouseAction.MOVE) {
            Spacer(Modifier.width(8.dp))
            Text("(${event.x}, ${event.y})")
        }
    }
}

@Composable
private fun MouseButtonItem(event: MacroEventState.MouseButtonEvent) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("MOUSE BTN", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
        Spacer(Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outline, shape = MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("Button ${event.buttonNumber}")
        }
        Spacer(Modifier.width(8.dp))
        Text(event.action.name, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ScrollItem(event: MacroEventState.ScrollEvent) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("SCROLL", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
        Spacer(Modifier.width(16.dp))
        val amount = if (event.scrollAmount > 0) "+${event.scrollAmount}" else "${event.scrollAmount}"
        Text("Amount: $amount", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DelayItem(event: MacroEventState.DelayEvent) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("DELAY", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        Spacer(Modifier.width(16.dp))
        Text("${event.durationMs} ms", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SetAutoWaitItem(event: MacroEventState.SetAutoWaitEvent) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("AUTO DELAY", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
        Spacer(Modifier.width(16.dp))
        Text("Set automatic delay to ${event.delayMs} ms", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ScriptItem(event: MacroEventState.ScriptEvent) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("SCRIPT", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
        Spacer(Modifier.width(16.dp))
        Text(
            text = event.script.take(50).replace("\n", " ") + (if (event.script.length > 50) "..." else ""),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
