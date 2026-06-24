package com.kapcode.`open`.macropad.kmps.ui.components

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
import com.kapcode.`open`.macropad.kmps.*
import com.kapcode.open.macropad.kmps.models.*
import org.jetbrains.compose.resources.stringResource

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
            label = { Text(stringResource(Res.string.routine_name)) },
            modifier = Modifier.fillMaxWidth().tabFocus()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(stringResource(Res.string.triggers), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.triggers_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
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
            Text(stringResource(Res.string.add_trigger))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(stringResource(Res.string.logic_blocks), style = MaterialTheme.typography.titleMedium)
        
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
            Text(stringResource(Res.string.add_logic_block))
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
                Text(stringResource(Res.string.trigger_type), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove Trigger", modifier = Modifier.size(18.dp))
                }
            }
            
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(when(trigger) {
                        is AutomationTrigger.KeyHold -> stringResource(Res.string.key_hold)
                        is AutomationTrigger.MultiTap -> stringResource(Res.string.multi_tap)
                        is AutomationTrigger.Sequence -> stringResource(Res.string.sequence)
                        is AutomationTrigger.OnConditionMet -> stringResource(Res.string.condition_met)
                        is AutomationTrigger.ControllerButton -> stringResource(Res.string.controller_button)
                    })
                    Icon(Icons.Default.KeyboardArrowDown, null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text(stringResource(Res.string.key_hold)) }, onClick = { onTriggerChange(AutomationTrigger.KeyHold("F13", "500")); expanded = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.multi_tap)) }, onClick = { onTriggerChange(AutomationTrigger.MultiTap("F13", "2", "300")); expanded = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.sequence)) }, onClick = { onTriggerChange(AutomationTrigger.Sequence(listOf("A", "B"), "1000")); expanded = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.condition_met)) }, onClick = { onTriggerChange(AutomationTrigger.OnConditionMet(AutomationCondition.ActiveWindowIs(""))); expanded = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.controller_button)) }, onClick = { onTriggerChange(AutomationTrigger.ControllerButton("A", "0")); expanded = false })
                }
            }

            when (trigger) {
                is AutomationTrigger.ControllerButton -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        var buttonExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { buttonExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(Res.string.btn_label, trigger.button))
                            }
                            DropdownMenu(expanded = buttonExpanded, onDismissRequest = { buttonExpanded = false }) {
                                listOf("A", "B", "X", "Y", "LB", "RB", "START", "BACK", "L3", "R3").forEach { btn ->
                                    DropdownMenuItem(text = { Text(btn) }, onClick = { onTriggerChange(trigger.copy(button = btn)); buttonExpanded = false })
                                }
                            }
                        }
                        OutlinedTextField(value = trigger.controllerIndex, onValueChange = { onTriggerChange(trigger.copy(controllerIndex = it)) }, label = { Text(stringResource(Res.string.ctrl_index_label)) }, modifier = Modifier.weight(1f).tabFocus())
                    }
                }
                is AutomationTrigger.KeyHold -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KeyValidationField(
                            label = stringResource(Res.string.keyboard_key), 
                            value = trigger.keyName, 
                            onValueChange = { onTriggerChange(trigger.copy(keyName = it)) }, 
                            isValidKey = isValidKey,
                            getSuggestions = getKeySuggestions,
                            modifier = Modifier.weight(1f).tabFocus()
                        )
                        OutlinedTextField(value = trigger.durationMs, onValueChange = { onTriggerChange(trigger.copy(durationMs = it)) }, label = { Text(stringResource(Res.string.hold_duration_ms)) }, modifier = Modifier.weight(1f).tabFocus())
                    }
                }
                is AutomationTrigger.MultiTap -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KeyValidationField(
                            label = stringResource(Res.string.keyboard_key), 
                            value = trigger.keyName, 
                            onValueChange = { onTriggerChange(trigger.copy(keyName = it)) }, 
                            isValidKey = isValidKey,
                            getSuggestions = getKeySuggestions,
                            modifier = Modifier.weight(1f).tabFocus()
                        )
                        OutlinedTextField(value = trigger.tapCount, onValueChange = { onTriggerChange(trigger.copy(tapCount = it)) }, label = { Text(stringResource(Res.string.tap_count)) }, modifier = Modifier.weight(0.5f).tabFocus())
                        OutlinedTextField(value = trigger.windowMs, onValueChange = { onTriggerChange(trigger.copy(windowMs = it)) }, label = { Text(stringResource(Res.string.tap_window_ms)) }, modifier = Modifier.weight(1f).tabFocus())
                    }
                }
                is AutomationTrigger.Sequence -> {
                    KeyValidationField(
                        label = stringResource(Res.string.keyboard_key), 
                        value = trigger.keys.joinToString(","), 
                        onValueChange = { onTriggerChange(trigger.copy(keys = it.split(",").map { k -> k.trim() })) }, 
                        isValidKey = isValidKey,
                        getSuggestions = getKeySuggestions,
                        modifier = Modifier.fillMaxWidth().tabFocus()
                    )
                    OutlinedTextField(value = trigger.windowMs, onValueChange = { onTriggerChange(trigger.copy(windowMs = it)) }, label = { Text(stringResource(Res.string.sequence_window_ms)) }, modifier = Modifier.fillMaxWidth().tabFocus())
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
                Text(stringResource(Res.string.press).lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.dismiss))
                }
            }
            
            ConditionEditor(block.condition, onConditionChange = { onConditionChange -> onChange(block.copy(condition = onConditionChange)) }, getLiveValue = getLiveValue)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
            Text(stringResource(Res.string.move).lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelLarge)
            
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
                Text(stringResource(Res.string.add_action))
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
                    is AutomationCondition.ActiveWindowIs -> stringResource(Res.string.active_window_is)
                    is AutomationCondition.ActiveWindowTitleIs -> stringResource(Res.string.active_title_is)
                    is AutomationCondition.Equals -> stringResource(Res.string.variable_equals)
                    is AutomationCondition.GreaterThan -> stringResource(Res.string.variable_greater_than)
                    is AutomationCondition.LessThan -> stringResource(Res.string.variable_less_than)
                    is AutomationCondition.Contains -> stringResource(Res.string.variable_contains)
                    else -> stringResource(Res.string.always_run)
                }
                Text(label)
                Icon(Icons.Default.KeyboardArrowDown, null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text(stringResource(Res.string.always_run)) }, onClick = { 
                    if (condition != null) onConditionChange(null)
                    expanded = false 
                })
                DropdownMenuItem(text = { Text(stringResource(Res.string.active_window_is)) }, onClick = { 
                    if (condition !is AutomationCondition.ActiveWindowIs) onConditionChange(AutomationCondition.ActiveWindowIs(""))
                    expanded = false 
                })
                DropdownMenuItem(text = { Text(stringResource(Res.string.active_title_is)) }, onClick = { 
                    if (condition !is AutomationCondition.ActiveWindowTitleIs) onConditionChange(AutomationCondition.ActiveWindowTitleIs(""))
                    expanded = false 
                })
                DropdownMenuItem(text = { Text(stringResource(Res.string.variable_equals)) }, onClick = { 
                    if (condition !is AutomationCondition.Equals) onConditionChange(AutomationCondition.Equals("", ""))
                    expanded = false 
                })
                DropdownMenuItem(text = { Text(stringResource(Res.string.variable_greater_than)) }, onClick = { 
                    if (condition !is AutomationCondition.GreaterThan) onConditionChange(AutomationCondition.GreaterThan("", "0"))
                    expanded = false 
                })
                DropdownMenuItem(text = { Text(stringResource(Res.string.variable_less_than)) }, onClick = { 
                    if (condition !is AutomationCondition.LessThan) onConditionChange(AutomationCondition.LessThan("", "0"))
                    expanded = false 
                })
                DropdownMenuItem(text = { Text(stringResource(Res.string.variable_contains)) }, onClick = { 
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
                    VariablePickerField(label = stringResource(Res.string.variable_short), value = condition.variable, onValueChange = { onConditionChange(condition.copy(variable = it)) }, modifier = Modifier.weight(1f).tabFocus(), getLiveValue = getLiveValue)
                    OutlinedTextField(value = condition.value, onValueChange = { onConditionChange(condition.copy(value = it)) }, label = { Text(stringResource(Res.string.buy_pro).lowercase().replaceFirstChar { it.uppercase() }) }, modifier = Modifier.weight(1f).tabFocus())
                }
                is AutomationCondition.GreaterThan -> {
                    VariablePickerField(label = stringResource(Res.string.variable_short), value = condition.variable, onValueChange = { onConditionChange(condition.copy(variable = it)) }, modifier = Modifier.weight(1f).tabFocus(), getLiveValue = getLiveValue)
                    OutlinedTextField(value = condition.value, onValueChange = { onConditionChange(condition.copy(value = it)) }, label = { Text(stringResource(Res.string.buy_pro).lowercase().replaceFirstChar { it.uppercase() }) }, modifier = Modifier.weight(1f).tabFocus())
                }
                is AutomationCondition.LessThan -> {
                    VariablePickerField(label = stringResource(Res.string.variable_short), value = condition.variable, onValueChange = { onConditionChange(condition.copy(variable = it)) }, modifier = Modifier.weight(1f).tabFocus(), getLiveValue = getLiveValue)
                    OutlinedTextField(value = condition.value, onValueChange = { onConditionChange(condition.copy(value = it)) }, label = { Text(stringResource(Res.string.buy_pro).lowercase().replaceFirstChar { it.uppercase() }) }, modifier = Modifier.weight(1f).tabFocus())
                }
                is AutomationCondition.Contains -> {
                    VariablePickerField(label = stringResource(Res.string.variable_short), value = condition.variable, onValueChange = { onConditionChange(condition.copy(variable = it)) }, modifier = Modifier.weight(1f).tabFocus(), getLiveValue = getLiveValue)
                    OutlinedTextField(value = condition.substring, onValueChange = { onConditionChange(condition.copy(substring = it)) }, label = { Text(stringResource(Res.string.substring_label)) }, modifier = Modifier.weight(1f).tabFocus())
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
                        is AutomationAction.MacroAction -> stringResource(Res.string.macro_execute)
                        is AutomationAction.ScriptAction -> stringResource(Res.string.js_script)
                        is AutomationAction.LayerShift -> stringResource(Res.string.layer_shift)
                        is AutomationAction.KeyEvent -> stringResource(Res.string.keyboard_key)
                        is AutomationAction.MouseEvent -> stringResource(Res.string.mouse_move)
                        is AutomationAction.MouseButtonEvent -> stringResource(Res.string.mouse_button)
                        is AutomationAction.ScrollEvent -> stringResource(Res.string.mouse_scroll)
                        is AutomationAction.DelayEvent -> stringResource(Res.string.delay_wait)
                        is AutomationAction.SetAutoDelay -> stringResource(Res.string.set_auto_delay)
                        is AutomationAction.SetVariable -> stringResource(Res.string.set_variable)
                        is AutomationAction.ControllerButton -> stringResource(Res.string.controller_button)
                        is AutomationAction.MouseKeyboard -> "Legacy"
                    })
                    Icon(Icons.Default.KeyboardArrowDown, null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text(stringResource(Res.string.keyboard_key)) }, onClick = { onChange(AutomationAction.KeyEvent("A", "PRESS")); expanded = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.mouse_move)) }, onClick = { onChange(AutomationAction.MouseEvent("0", "0", "MOVE")); expanded = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.mouse_button)) }, onClick = { onChange(AutomationAction.MouseButtonEvent("1", "CLICK")); expanded = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.mouse_scroll)) }, onClick = { onChange(AutomationAction.ScrollEvent("1")); expanded = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.controller_button)) }, onClick = { onChange(AutomationAction.ControllerButton("A", "0", "PRESS")); expanded = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.delay_wait)) }, onClick = { onChange(AutomationAction.DelayEvent("500")); expanded = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.mouse_scroll)) }, onClick = { onChange(AutomationAction.ScrollEvent("1")); expanded = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.delay_wait)) }, onClick = { onChange(AutomationAction.DelayEvent("500")); expanded = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.set_auto_delay)) }, onClick = { onChange(AutomationAction.SetAutoDelay("50")); expanded = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.set_variable)) }, onClick = { onChange(AutomationAction.SetVariable("", "")); expanded = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.macro_execute)) }, onClick = { onChange(AutomationAction.MacroAction("")); expanded = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.js_script)) }, onClick = { onChange(AutomationAction.ScriptAction("")); expanded = false })
                    DropdownMenuItem(text = { Text(stringResource(Res.string.layer_shift)) }, onClick = { onChange(AutomationAction.LayerShift("", false)); expanded = false })
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
                            DropdownMenuItem(text = { Text(stringResource(Res.string.press)) }, onClick = { onChange(action.copy(actionType = "PRESS")); typeExpanded = false })
                            DropdownMenuItem(text = { Text(stringResource(Res.string.release)) }, onClick = { onChange(action.copy(actionType = "RELEASE")); typeExpanded = false })
                            DropdownMenuItem(text = { Text(stringResource(Res.string.type_sequence)) }, onClick = { onChange(action.copy(actionType = "TYPE")); typeExpanded = false })
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
                            DropdownMenuItem(text = { Text(stringResource(Res.string.move)) }, onClick = { onChange(action.copy(actionType = "MOVE")); typeExpanded = false })
                            DropdownMenuItem(text = { Text(stringResource(Res.string.click)) }, onClick = { onChange(action.copy(actionType = "CLICK")); typeExpanded = false })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = action.isAnimated, onCheckedChange = { onChange(action.copy(isAnimated = it)) })
                        Text(stringResource(Res.string.animate).take(4), style = MaterialTheme.typography.bodySmall)
                    }
                }
                is AutomationAction.MouseButtonEvent -> {
                    VariablePickerField(label = stringResource(Res.string.btn_label, "").replace(": ", ""), value = action.buttonNumber, onValueChange = { onChange(action.copy(buttonNumber = it)) }, modifier = Modifier.weight(1f).tabFocus(), getLiveValue = getLiveValue)
                    var typeExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { typeExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(action.actionType)
                        }
                        DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            DropdownMenuItem(text = { Text(stringResource(Res.string.press)) }, onClick = { onChange(action.copy(actionType = "PRESS")); typeExpanded = false })
                            DropdownMenuItem(text = { Text(stringResource(Res.string.release)) }, onClick = { onChange(action.copy(actionType = "RELEASE")); typeExpanded = false })
                            DropdownMenuItem(text = { Text(stringResource(Res.string.click)) }, onClick = { onChange(action.copy(actionType = "CLICK")); typeExpanded = false })
                        }
                    }
                }
                is AutomationAction.ScrollEvent -> {
                    VariablePickerField(label = stringResource(Res.string.mouse_scroll), value = action.amount, onValueChange = { onChange(action.copy(amount = it)) }, modifier = Modifier.fillMaxWidth().tabFocus(), getLiveValue = getLiveValue)
                }
                is AutomationAction.DelayEvent -> {
                    VariablePickerField(label = stringResource(Res.string.delay_wait), value = action.durationMs, onValueChange = { onChange(action.copy(durationMs = it)) }, modifier = Modifier.fillMaxWidth().tabFocus(), getLiveValue = getLiveValue)
                }
                is AutomationAction.SetAutoDelay -> {
                    VariablePickerField(label = stringResource(Res.string.set_auto_delay), value = action.delayMs, onValueChange = { onChange(action.copy(delayMs = it)) }, modifier = Modifier.fillMaxWidth().tabFocus(), getLiveValue = getLiveValue)
                }
                is AutomationAction.SetVariable -> {
                    VariablePickerField(label = stringResource(Res.string.routine_name).replace("Routine", "Variable"), value = action.name, onValueChange = { onChange(action.copy(name = it)) }, modifier = Modifier.weight(1f).tabFocus(), getLiveValue = getLiveValue)
                    VariablePickerField(label = stringResource(Res.string.buy_pro).lowercase().replaceFirstChar { it.uppercase() }, value = action.value, onValueChange = { onChange(action.copy(value = it)) }, modifier = Modifier.weight(1f).tabFocus(), getLiveValue = getLiveValue)
                }
                is AutomationAction.MacroAction -> {
                    OutlinedTextField(value = action.macroId, onValueChange = { onChange(action.copy(macroId = it)) }, label = { Text(stringResource(Res.string.macros).replace("s", " ID")) }, modifier = Modifier.fillMaxWidth().tabFocus())
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
                                DropdownMenuItem(text = { Text(stringResource(Res.string.press)) }, onClick = { onChange(action.copy(actionType = "PRESS")); typeExpanded = false })
                                DropdownMenuItem(text = { Text(stringResource(Res.string.release)) }, onClick = { onChange(action.copy(actionType = "RELEASE")); typeExpanded = false })
                            }
                        }
                    }
                }
                is AutomationAction.ScriptAction -> {
                    OutlinedTextField(
                        value = action.script, 
                        onValueChange = { onChange(action.copy(script = it)) }, 
                        label = { Text(stringResource(Res.string.js_script_content)) }, 
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp).tabFocus(),
                        placeholder = { Text(stringResource(Res.string.js_script_placeholder)) }
                    )
                }
                is AutomationAction.LayerShift -> {
                    OutlinedTextField(value = action.layerId, onValueChange = { onChange(action.copy(layerId = it)) }, label = { Text(stringResource(Res.string.layer_shift).replace("Shift", "ID")) }, modifier = Modifier.weight(1.5f).tabFocus())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = action.isMomentary, onCheckedChange = { onChange(action.copy(isMomentary = it)) })
                        Text(stringResource(Res.string.key_hold).replace("Key ", ""), style = MaterialTheme.typography.bodySmall)
                    }
                }
                else -> {}
            }
        }
    }
}
