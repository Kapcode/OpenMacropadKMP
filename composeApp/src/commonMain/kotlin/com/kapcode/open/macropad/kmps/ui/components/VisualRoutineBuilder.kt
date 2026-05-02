package com.kapcode.open.macropad.kmps.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.models.*

/**
 * Common modifier to fix Tab focus traversal in Compose for Desktop.
 * Intercepts Tab and Shift+Tab to move focus manually instead of typing the character.
 */
@Composable
fun Modifier.tabFocus(): Modifier {
    val focusManager = LocalFocusManager.current
    return this.onPreviewKeyEvent {
        if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
            focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
            true
        } else {
            false
        }
    }
}

@Composable
fun VisualRoutineBuilder(
    routine: AutomationRoutine,
    onRoutineChange: (AutomationRoutine) -> Unit,
    getLiveValue: (String) -> String? = { null },
    isValidKey: (String) -> Boolean = { true },
    getKeySuggestions: (String) -> List<String> = { emptyList() }
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = routine.name,
            onValueChange = { onRoutineChange(routine.copy(name = it)) },
            label = { Text("Routine Name") },
            modifier = Modifier.fillMaxWidth().tabFocus()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Triggers", style = MaterialTheme.typography.titleMedium)
        Text("Any of these triggers will execute the routine.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(8.dp))

        // Trigger Stack
        routine.triggers.forEachIndexed { index, trigger ->
            TriggerSection(
                trigger = trigger,
                onTriggerChange = { newTrigger ->
                    val newList = routine.triggers.toMutableList()
                    newList[index] = newTrigger
                    onRoutineChange(routine.copy(triggers = newList))
                },
                onRemove = {
                    onRoutineChange(routine.copy(triggers = routine.triggers - trigger))
                },
                getLiveValue = getLiveValue,
                isValidKey = isValidKey,
                getKeySuggestions = getKeySuggestions
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Button(
            onClick = {
                onRoutineChange(routine.copy(triggers = routine.triggers + AutomationTrigger.KeyHold("F13", "500")))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Add Trigger")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Logic Blocks", style = MaterialTheme.typography.titleMedium)
        
        // Logic Blocks (non-lazy list for unified scrolling)
        routine.logicBlocks.forEach { block ->
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
                },
                getLiveValue = getLiveValue,
                isValidKey = isValidKey,
                getKeySuggestions = getKeySuggestions
            )
            Spacer(modifier = Modifier.height(8.dp))
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
fun TriggerSection(
    trigger: AutomationTrigger, 
    onTriggerChange: (AutomationTrigger) -> Unit, 
    onRemove: () -> Unit, 
    getLiveValue: (String) -> String?,
    isValidKey: (String) -> Boolean,
    getKeySuggestions: (String) -> List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Trigger Type", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove Trigger", modifier = Modifier.size(18.dp))
                }
            }
            
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(when(trigger) {
                        is AutomationTrigger.KeyHold -> "Key Hold"
                        is AutomationTrigger.MultiTap -> "Multi-Tap"
                        is AutomationTrigger.Sequence -> "Sequence"
                        is AutomationTrigger.OnConditionMet -> "Condition Met"
                        is AutomationTrigger.ControllerButton -> "Controller Button"
                    })
                    Icon(Icons.Default.KeyboardArrowDown, null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Key Hold") }, onClick = { onTriggerChange(AutomationTrigger.KeyHold("F13", "500")); expanded = false })
                    DropdownMenuItem(text = { Text("Multi-Tap") }, onClick = { onTriggerChange(AutomationTrigger.MultiTap("F13", "2", "300")); expanded = false })
                    DropdownMenuItem(text = { Text("Sequence") }, onClick = { onTriggerChange(AutomationTrigger.Sequence(listOf("A", "B"), "1000")); expanded = false })
                    DropdownMenuItem(text = { Text("Condition Met") }, onClick = { onTriggerChange(AutomationTrigger.OnConditionMet(AutomationCondition.ActiveWindowIs(""))); expanded = false })
                    DropdownMenuItem(text = { Text("Controller Button") }, onClick = { onTriggerChange(AutomationTrigger.ControllerButton("A", "0")); expanded = false })
                }
            }

            when (trigger) {
                is AutomationTrigger.ControllerButton -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        var buttonExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { buttonExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Btn: ${trigger.button}")
                            }
                            DropdownMenu(expanded = buttonExpanded, onDismissRequest = { buttonExpanded = false }) {
                                listOf("A", "B", "X", "Y", "LB", "RB", "START", "BACK", "L3", "R3").forEach { btn ->
                                    DropdownMenuItem(text = { Text(btn) }, onClick = { onTriggerChange(trigger.copy(button = btn)); buttonExpanded = false })
                                }
                            }
                        }
                        OutlinedTextField(value = trigger.controllerIndex, onValueChange = { onTriggerChange(trigger.copy(controllerIndex = it)) }, label = { Text("Ctrl # (0-3 or ANY)") }, modifier = Modifier.weight(1f).tabFocus())
                    }
                }
                is AutomationTrigger.KeyHold -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KeyValidationField(
                            label = "Key", 
                            value = trigger.keyName, 
                            onValueChange = { onTriggerChange(trigger.copy(keyName = it)) }, 
                            isValidKey = isValidKey,
                            getSuggestions = getKeySuggestions,
                            modifier = Modifier.weight(1f).tabFocus()
                        )
                        OutlinedTextField(value = trigger.durationMs, onValueChange = { onTriggerChange(trigger.copy(durationMs = it)) }, label = { Text("Duration (ms)") }, modifier = Modifier.weight(1f).tabFocus())
                    }
                }
                is AutomationTrigger.MultiTap -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KeyValidationField(
                            label = "Key", 
                            value = trigger.keyName, 
                            onValueChange = { onTriggerChange(trigger.copy(keyName = it)) }, 
                            isValidKey = isValidKey,
                            getSuggestions = getKeySuggestions,
                            modifier = Modifier.weight(1f).tabFocus()
                        )
                        OutlinedTextField(value = trigger.tapCount, onValueChange = { onTriggerChange(trigger.copy(tapCount = it)) }, label = { Text("Count") }, modifier = Modifier.weight(0.5f).tabFocus())
                        OutlinedTextField(value = trigger.windowMs, onValueChange = { onTriggerChange(trigger.copy(windowMs = it)) }, label = { Text("Window (ms)") }, modifier = Modifier.weight(1f).tabFocus())
                    }
                }
                is AutomationTrigger.Sequence -> {
                    KeyValidationField(
                        label = "Keys (comma separated)", 
                        value = trigger.keys.joinToString(","), 
                        onValueChange = { onTriggerChange(trigger.copy(keys = it.split(",").map { k -> k.trim() })) }, 
                        isValidKey = isValidKey,
                        getSuggestions = getKeySuggestions,
                        modifier = Modifier.fillMaxWidth().tabFocus()
                    )
                    OutlinedTextField(value = trigger.windowMs, onValueChange = { onTriggerChange(trigger.copy(windowMs = it)) }, label = { Text("Window (ms)") }, modifier = Modifier.fillMaxWidth().tabFocus())
                }
                is AutomationTrigger.OnConditionMet -> {
                    ConditionEditor(
                        condition = trigger.condition, 
                        onConditionChange = { newCond ->
                            if (newCond != null) {
                                onTriggerChange(trigger.copy(condition = newCond))
                            }
                        }, 
                        getLiveValue = getLiveValue
                    )
                }
            }
        }
    }
}

@Composable
fun LogicBlockItem(
    block: LogicBlock,
    onRemove: () -> Unit,
    onChange: (LogicBlock) -> Unit,
    getLiveValue: (String) -> String?,
    isValidKey: (String) -> Boolean,
    getKeySuggestions: (String) -> List<String>
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
            
            ConditionEditor(block.condition, onConditionChange = { onConditionChange -> onChange(block.copy(condition = onConditionChange)) }, getLiveValue = getLiveValue)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
            Text("Then", style = MaterialTheme.typography.labelLarge)
            
            block.actions.forEachIndexed { index, action ->
                ActionItem(action, 
                    onRemove = { onChange(block.copy(actions = block.actions - action)) },
                    onChange = { newAction ->
                        val newList = block.actions.toMutableList()
                        newList[index] = newAction
                        onChange(block.copy(actions = newList))
                    },
                    getLiveValue = getLiveValue,
                    isValidKey = isValidKey,
                    getKeySuggestions = getKeySuggestions
                )
            }
            
            TextButton(onClick = { onChange(block.copy(actions = block.actions + AutomationAction.KeyEvent("A", "PRESS"))) }) {
                Icon(Icons.Default.Add, null)
                Text("Add Action")
            }
        }
    }
}

@Composable
fun ConditionEditor(condition: AutomationCondition?, onConditionChange: (AutomationCondition?) -> Unit, getLiveValue: (String) -> String?) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                val label = when(condition) {
                    is AutomationCondition.ActiveWindowIs -> "Active Window Is"
                    is AutomationCondition.ActiveWindowTitleIs -> "Active Title Is"
                    is AutomationCondition.Equals -> "Variable Equals"
                    is AutomationCondition.GreaterThan -> "Variable >"
                    is AutomationCondition.LessThan -> "Variable <"
                    is AutomationCondition.Contains -> "Variable Contains"
                    else -> "Always Run"
                }
                Text(label)
                Icon(Icons.Default.KeyboardArrowDown, null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Always Run") }, onClick = { 
                    if (condition != null) onConditionChange(null)
                    expanded = false 
                })
                DropdownMenuItem(text = { Text("Active Window Is") }, onClick = { 
                    if (condition !is AutomationCondition.ActiveWindowIs) onConditionChange(AutomationCondition.ActiveWindowIs(""))
                    expanded = false 
                })
                DropdownMenuItem(text = { Text("Active Title Is") }, onClick = { 
                    if (condition !is AutomationCondition.ActiveWindowTitleIs) onConditionChange(AutomationCondition.ActiveWindowTitleIs(""))
                    expanded = false 
                })
                DropdownMenuItem(text = { Text("Variable Equals") }, onClick = { 
                    if (condition !is AutomationCondition.Equals) onConditionChange(AutomationCondition.Equals("", ""))
                    expanded = false 
                })
                DropdownMenuItem(text = { Text("Variable >") }, onClick = { 
                    if (condition !is AutomationCondition.GreaterThan) onConditionChange(AutomationCondition.GreaterThan("", "0"))
                    expanded = false 
                })
                DropdownMenuItem(text = { Text("Variable <") }, onClick = { 
                    if (condition !is AutomationCondition.LessThan) onConditionChange(AutomationCondition.LessThan("", "0"))
                    expanded = false 
                })
                DropdownMenuItem(text = { Text("Variable Contains") }, onClick = { 
                    if (condition !is AutomationCondition.Contains) onConditionChange(AutomationCondition.Contains("", ""))
                    expanded = false 
                })
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (condition) {
                is AutomationCondition.ActiveWindowIs -> {
                    OutlinedTextField(value = condition.processName, onValueChange = { onConditionChange(condition.copy(processName = it)) }, label = { Text("Process Name") }, modifier = Modifier.weight(2f).tabFocus())
                }
                is AutomationCondition.ActiveWindowTitleIs -> {
                    OutlinedTextField(value = condition.windowTitle, onValueChange = { onConditionChange(condition.copy(windowTitle = it)) }, label = { Text("Window Title") }, modifier = Modifier.weight(2f).tabFocus())
                }
                is AutomationCondition.Equals -> {
                    VariablePickerField(label = "Var", value = condition.variable, onValueChange = { onConditionChange(condition.copy(variable = it)) }, modifier = Modifier.weight(1f).tabFocus(), getLiveValue = getLiveValue)
                    OutlinedTextField(value = condition.value, onValueChange = { onConditionChange(condition.copy(value = it)) }, label = { Text("Value") }, modifier = Modifier.weight(1f).tabFocus())
                }
                is AutomationCondition.GreaterThan -> {
                    VariablePickerField(label = "Var", value = condition.variable, onValueChange = { onConditionChange(condition.copy(variable = it)) }, modifier = Modifier.weight(1f).tabFocus(), getLiveValue = getLiveValue)
                    OutlinedTextField(value = condition.value, onValueChange = { onConditionChange(condition.copy(value = it)) }, label = { Text("Value") }, modifier = Modifier.weight(1f).tabFocus())
                }
                is AutomationCondition.LessThan -> {
                    VariablePickerField(label = "Var", value = condition.variable, onValueChange = { onConditionChange(condition.copy(variable = it)) }, modifier = Modifier.weight(1f).tabFocus(), getLiveValue = getLiveValue)
                    OutlinedTextField(value = condition.value, onValueChange = { onConditionChange(condition.copy(value = it)) }, label = { Text("Value") }, modifier = Modifier.weight(1f).tabFocus())
                }
                is AutomationCondition.Contains -> {
                    VariablePickerField(label = "Var", value = condition.variable, onValueChange = { onConditionChange(condition.copy(variable = it)) }, modifier = Modifier.weight(1f).tabFocus(), getLiveValue = getLiveValue)
                    OutlinedTextField(value = condition.substring, onValueChange = { onConditionChange(condition.copy(substring = it)) }, label = { Text("Substring") }, modifier = Modifier.weight(1f).tabFocus())
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ActionItem(
    action: AutomationAction, 
    onRemove: () -> Unit, 
    onChange: (AutomationAction) -> Unit, 
    getLiveValue: (String) -> String?,
    isValidKey: (String) -> Boolean,
    getKeySuggestions: (String) -> List<String>
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(when(action) {
                        is AutomationAction.MacroAction -> "Macro"
                        is AutomationAction.ScriptAction -> "JS Script"
                        is AutomationAction.LayerShift -> "Layer Shift"
                        is AutomationAction.KeyEvent -> "Keyboard"
                        is AutomationAction.MouseEvent -> "Mouse Move"
                        is AutomationAction.MouseButtonEvent -> "Mouse Button"
                        is AutomationAction.ScrollEvent -> "Scroll"
                        is AutomationAction.DelayEvent -> "Delay"
                        is AutomationAction.SetAutoDelay -> "Auto-Delay"
                        is AutomationAction.SetVariable -> "Variable"
                        is AutomationAction.ControllerButton -> "Controller"
                        is AutomationAction.MouseKeyboard -> "Legacy"
                    })
                    Icon(Icons.Default.KeyboardArrowDown, null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Keyboard (Key)") }, onClick = { onChange(AutomationAction.KeyEvent("A", "PRESS")); expanded = false })
                    DropdownMenuItem(text = { Text("Mouse (Move)") }, onClick = { onChange(AutomationAction.MouseEvent("0", "0", "MOVE")); expanded = false })
                    DropdownMenuItem(text = { Text("Mouse (Button)") }, onClick = { onChange(AutomationAction.MouseButtonEvent("1", "CLICK")); expanded = false })
                    DropdownMenuItem(text = { Text("Mouse (Scroll)") }, onClick = { onChange(AutomationAction.ScrollEvent("1")); expanded = false })
                    DropdownMenuItem(text = { Text("Controller (Button)") }, onClick = { onChange(AutomationAction.ControllerButton("A", "0", "PRESS")); expanded = false })
                    DropdownMenuItem(text = { Text("Delay (Wait)") }, onClick = { onChange(AutomationAction.DelayEvent("500")); expanded = false })
                    DropdownMenuItem(text = { Text("Mouse (Scroll)") }, onClick = { onChange(AutomationAction.ScrollEvent("1")); expanded = false })
                    DropdownMenuItem(text = { Text("Delay (Wait)") }, onClick = { onChange(AutomationAction.DelayEvent("500")); expanded = false })
                    DropdownMenuItem(text = { Text("Set Auto-Delay") }, onClick = { onChange(AutomationAction.SetAutoDelay("50")); expanded = false })
                    DropdownMenuItem(text = { Text("Set Variable") }, onClick = { onChange(AutomationAction.SetVariable("", "")); expanded = false })
                    DropdownMenuItem(text = { Text("Macro (Execute)") }, onClick = { onChange(AutomationAction.MacroAction("")); expanded = false })
                    DropdownMenuItem(text = { Text("JS Script") }, onClick = { onChange(AutomationAction.ScriptAction("")); expanded = false })
                    DropdownMenuItem(text = { Text("Layer Shift") }, onClick = { onChange(AutomationAction.LayerShift("", false)); expanded = false })
                }
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove Action")
            }
        }

        // Action-specific editors
        Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (action) {
                is AutomationAction.KeyEvent -> {
                    KeyValidationField(
                        label = "Key(s)", 
                        value = action.keyName, 
                        onValueChange = { onChange(action.copy(keyName = it)) }, 
                        isValidKey = isValidKey,
                        getSuggestions = getKeySuggestions,
                        modifier = Modifier.weight(1f).tabFocus()
                    )
                    var typeExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { typeExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(action.actionType)
                        }
                        DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            DropdownMenuItem(text = { Text("PRESS") }, onClick = { onChange(action.copy(actionType = "PRESS")); typeExpanded = false })
                            DropdownMenuItem(text = { Text("RELEASE") }, onClick = { onChange(action.copy(actionType = "RELEASE")); typeExpanded = false })
                            DropdownMenuItem(text = { Text("TYPE (Sequence)") }, onClick = { onChange(action.copy(actionType = "TYPE")); typeExpanded = false })
                        }
                    }
                }
                is AutomationAction.MouseEvent -> {
                    VariablePickerField(label = "X", value = action.x, onValueChange = { onChange(action.copy(x = it)) }, modifier = Modifier.weight(1f).tabFocus(), getLiveValue = getLiveValue)
                    VariablePickerField(label = "Y", value = action.y, onValueChange = { onChange(action.copy(y = it)) }, modifier = Modifier.weight(1f).tabFocus(), getLiveValue = getLiveValue)
                    var typeExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { typeExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(action.actionType)
                        }
                        DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            DropdownMenuItem(text = { Text("MOVE") }, onClick = { onChange(action.copy(actionType = "MOVE")); typeExpanded = false })
                            DropdownMenuItem(text = { Text("CLICK") }, onClick = { onChange(action.copy(actionType = "CLICK")); typeExpanded = false })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = action.isAnimated, onCheckedChange = { onChange(action.copy(isAnimated = it)) })
                        Text("Anim", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is AutomationAction.MouseButtonEvent -> {
                    VariablePickerField(label = "Btn #", value = action.buttonNumber, onValueChange = { onChange(action.copy(buttonNumber = it)) }, modifier = Modifier.weight(1f).tabFocus(), getLiveValue = getLiveValue)
                    var typeExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { typeExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(action.actionType)
                        }
                        DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            DropdownMenuItem(text = { Text("PRESS") }, onClick = { onChange(action.copy(actionType = "PRESS")); typeExpanded = false })
                            DropdownMenuItem(text = { Text("RELEASE") }, onClick = { onChange(action.copy(actionType = "RELEASE")); typeExpanded = false })
                            DropdownMenuItem(text = { Text("CLICK") }, onClick = { onChange(action.copy(actionType = "CLICK")); typeExpanded = false })
                        }
                    }
                }
                is AutomationAction.ScrollEvent -> {
                    VariablePickerField(label = "Amount (+ for Up, - for Down)", value = action.amount, onValueChange = { onChange(action.copy(amount = it)) }, modifier = Modifier.fillMaxWidth().tabFocus(), getLiveValue = getLiveValue)
                }
                is AutomationAction.DelayEvent -> {
                    VariablePickerField(label = "Duration (ms)", value = action.durationMs, onValueChange = { onChange(action.copy(durationMs = it)) }, modifier = Modifier.fillMaxWidth().tabFocus(), getLiveValue = getLiveValue)
                }
                is AutomationAction.SetAutoDelay -> {
                    VariablePickerField(label = "New Auto-Delay (ms)", value = action.delayMs, onValueChange = { onChange(action.copy(delayMs = it)) }, modifier = Modifier.fillMaxWidth().tabFocus(), getLiveValue = getLiveValue)
                }
                is AutomationAction.SetVariable -> {
                    VariablePickerField(label = "Variable Name", value = action.name, onValueChange = { onChange(action.copy(name = it)) }, modifier = Modifier.weight(1f).tabFocus(), getLiveValue = getLiveValue)
                    VariablePickerField(label = "New Value", value = action.value, onValueChange = { onChange(action.copy(value = it)) }, modifier = Modifier.weight(1f).tabFocus(), getLiveValue = getLiveValue)
                }
                is AutomationAction.MacroAction -> {
                    OutlinedTextField(value = action.macroId, onValueChange = { onChange(action.copy(macroId = it)) }, label = { Text("Macro ID") }, modifier = Modifier.fillMaxWidth().tabFocus())
                }
                is AutomationAction.ControllerButton -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        var buttonExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { buttonExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(action.button)
                            }
                            DropdownMenu(expanded = buttonExpanded, onDismissRequest = { buttonExpanded = false }) {
                                listOf("A", "B", "X", "Y", "LB", "RB", "START", "BACK", "L3", "R3").forEach { btn ->
                                    DropdownMenuItem(text = { Text(btn) }, onClick = { onChange(action.copy(button = btn)); buttonExpanded = false })
                                }
                            }
                        }
                        var typeExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { typeExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(action.actionType)
                            }
                            DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                                DropdownMenuItem(text = { Text("PRESS") }, onClick = { onChange(action.copy(actionType = "PRESS")); typeExpanded = false })
                                DropdownMenuItem(text = { Text("RELEASE") }, onClick = { onChange(action.copy(actionType = "RELEASE")); typeExpanded = false })
                            }
                        }
                    }
                }
                is AutomationAction.ScriptAction -> {
                    OutlinedTextField(
                        value = action.script, 
                        onValueChange = { onChange(action.copy(script = it)) }, 
                        label = { Text("JS Script Content") }, 
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp).tabFocus(),
                        placeholder = { Text("kap.log('Hello');") }
                    )
                }
                is AutomationAction.LayerShift -> {
                    OutlinedTextField(value = action.layerId, onValueChange = { onChange(action.copy(layerId = it)) }, label = { Text("Layer ID") }, modifier = Modifier.weight(1.5f).tabFocus())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = action.isMomentary, onCheckedChange = { onChange(action.copy(isMomentary = it)) })
                        Text("Hold", style = MaterialTheme.typography.bodySmall)
                    }
                }
                else -> {}
            }
        }
    }
}
