package com.kapcode.open.macropad.kmps.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.models.*

@Composable
fun VisualRoutineBuilder(
    routine: AutomationRoutine,
    onRoutineChange: (AutomationRoutine) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Routine: ${routine.name}", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TriggerSection(routine.trigger) { newTrigger ->
            onRoutineChange(routine.copy(trigger = newTrigger))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Logic Blocks", style = MaterialTheme.typography.titleMedium)
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(routine.logicBlocks) { block ->
                LogicBlockItem(block, 
                    onRemove = {
                        onRoutineChange(routine.copy(logicBlocks = routine.logicBlocks - block))
                    },
                    onChange = { newBlock ->
                        val newList = routine.logicBlocks.toMutableList()
                        val index = newList.indexOf(block)
                        if (index != -1) {
                            newList[index] = newBlock
                            onRoutineChange(routine.copy(logicBlocks = newList))
                        }
                    }
                )
            }
        }
        
        Button(
            onClick = {
                onRoutineChange(routine.copy(logicBlocks = routine.logicBlocks + LogicBlock()))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Add Logic Block")
        }
    }
}

@Composable
fun TriggerSection(trigger: AutomationTrigger, onTriggerChange: (AutomationTrigger) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Trigger", style = MaterialTheme.typography.titleSmall)
            // Implementation for selecting and configuring triggers
            Text(trigger.toString()) 
        }
    }
}

@Composable
fun LogicBlockItem(
    block: LogicBlock,
    onRemove: () -> Unit,
    onChange: (LogicBlock) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("If", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
            
            // Condition placeholder
            Text(block.condition?.toString() ?: "No Condition (Always Runs)")
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
            Text("Then", style = MaterialTheme.typography.labelLarge)
            // Actions placeholder
            block.actions.forEach { action ->
                Text("- ${action.toString()}")
            }
        }
    }
}
