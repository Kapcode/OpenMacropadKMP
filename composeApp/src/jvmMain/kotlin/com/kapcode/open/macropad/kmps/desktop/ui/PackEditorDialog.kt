package com.kapcode.open.macropad.kmps.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import com.kapcode.open.macropad.kmps.desktop.ui.components.RedrawFix
import com.kapcode.open.macropad.kmps.desktop.ui.AppDialog
import com.kapcode.open.macropad.kmps.models.GridWidget
import com.kapcode.open.macropad.kmps.models.MacroPack
import com.kapcode.open.macropad.kmps.desktop.model.MacroFileState
import com.kapcode.open.macropad.kmps.desktop.ui.components.AppTooltipArea
import com.kapcode.open.macropad.kmps.desktop.viewmodel.MacroManagerViewModel
import com.kapcode.open.macropad.kmps.desktop.viewmodel.ConsoleViewModel
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme
import com.kapcode.`open`.macropad.kmps.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackEditorDialog(
    pack: MacroPack,
    macroManagerViewModel: MacroManagerViewModel,
    availableMacros: List<MacroFileState>,
    onDismissRequest: () -> Unit,
    onSave: (MacroPack) -> Unit,
    onOpenInJsonEditor: (MacroPack) -> Unit,
    selectedTheme: String,
    consoleViewModel: ConsoleViewModel,
    icon: androidx.compose.ui.graphics.painter.Painter? = null
) {
    LaunchedEffect(Unit) {
        println("[PackEditor] Dialog launched for pack: ${pack.name} (${pack.id})")
    }

    var name by remember { mutableStateOf(pack.name) }
    var author by remember { mutableStateOf(pack.author) }
    var version by remember { mutableStateOf(pack.version) }

    var autoSwitchGroups by remember { mutableStateOf(
        if (pack.autoSwitchGroups.isEmpty() && (pack.targetProcess != null || pack.targetWindowTitle != null)) {
            // Migration
            val rules = mutableListOf<com.kapcode.open.macropad.kmps.models.AutoSwitchRule>()
            pack.targetProcess?.let {
                rules.add(com.kapcode.open.macropad.kmps.models.AutoSwitchRule(
                    com.kapcode.open.macropad.kmps.models.MatchTarget.PROCESS_NAME,
                    com.kapcode.open.macropad.kmps.models.MatchOperator.EQUALS,
                    it
                ))
            }
            pack.targetWindowTitle?.let {
                rules.add(com.kapcode.open.macropad.kmps.models.AutoSwitchRule(
                    com.kapcode.open.macropad.kmps.models.MatchTarget.WINDOW_TITLE,
                    com.kapcode.open.macropad.kmps.models.MatchOperator.CONTAINS,
                    it
                ))
            }
            listOf(com.kapcode.open.macropad.kmps.models.AutoSwitchGroup(rules))
        } else {
            pack.autoSwitchGroups
        }
    ) }

    var isActive by remember { mutableStateOf(pack.isActive) }
    var widgets by remember { mutableStateOf(pack.widgets) }
    var routines by remember { mutableStateOf(pack.routines) }

    var widgetToEdit by remember { mutableStateOf<GridWidget?>(null) }
    var showWidgetEditor by remember { mutableStateOf(false) }
    
    var routineToEdit by remember { mutableStateOf<com.kapcode.open.macropad.kmps.models.AutomationRoutine?>(null) }
    var showRoutineEditor by remember { mutableStateOf(false) }

    AppDialog(
        onCloseRequest = onDismissRequest,
        state = rememberWindowState(width = 1100.dp, height = 700.dp),
        title = stringResource(Res.string.edit_pack_format, name),
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel,
        icon = icon,
        resizable = true
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.pack_editor)) },
                    actions = {
                        val currentPack = pack.copy(
                        name = name,
                        author = author,
                        version = version,
                        autoSwitchGroups = autoSwitchGroups,
                        isActive = isActive,
                        widgets = widgets,
                        routines = routines
                    )
                        TextButton(onClick = { onOpenInJsonEditor(currentPack) }) {
                            Text(stringResource(Res.string.open_json_editor))
                        }
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.close))
                        }
                    }
                )
            },
            bottomBar = {
                Surface(tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismissRequest) { Text(stringResource(Res.string.cancel)) }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                println("[PackEditor] Save button clicked.")
                                onSave(pack.copy(
                                    name = name,
                                    author = author,
                                    version = version,
                                    autoSwitchGroups = autoSwitchGroups,
                                    isActive = isActive,
                                    widgets = widgets,
                                    routines = routines
                                ))
                            },
                            enabled = name.isNotBlank()
                        ) {
                            Text(stringResource(Res.string.save_pack))
                        }
                    }
                }
            }
        ) { padding ->
            Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Left side: Metadata
                val leftScrollState = rememberScrollState()
                val focusManager = LocalFocusManager.current
                
                Column(
                    modifier = Modifier.weight(1f).padding(16.dp).verticalScroll(leftScrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val commonKeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    val commonKeyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    )

                    fun Modifier.tabFocus(): Modifier = this.onPreviewKeyEvent {
                        if (it.key == Key.Tab && it.type == KeyEventType.KeyDown) {
                            focusManager.moveFocus(if (it.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                            true
                        } else {
                            false
                        }
                    }
                    
                    Text(stringResource(Res.string.metadata), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = name, 
                        onValueChange = { name = it }, 
                        label = { Text(stringResource(Res.string.pack_name)) }, 
                        modifier = Modifier.fillMaxWidth().tabFocus(), 
                        keyboardOptions = commonKeyboardOptions,
                        keyboardActions = commonKeyboardActions
                    )
                    OutlinedTextField(
                        value = author, 
                        onValueChange = { author = it }, 
                        label = { Text(stringResource(Res.string.author)) }, 
                        modifier = Modifier.fillMaxWidth().tabFocus(), 
                        keyboardOptions = commonKeyboardOptions,
                        keyboardActions = commonKeyboardActions
                    )
                    OutlinedTextField(
                        value = version, 
                        onValueChange = { version = it }, 
                        label = { Text(stringResource(Res.string.version)) }, 
                        modifier = Modifier.fillMaxWidth().tabFocus(), 
                        keyboardOptions = commonKeyboardOptions,
                        keyboardActions = commonKeyboardActions
                    )
                    
                    AppTooltipArea(
                        tooltipText = if (isActive) 
                            "ENABLED: This pack will automatically activate on your phone when its target window is focused." 
                            else "DISABLED: This pack will never auto-switch, even if its target window is open."
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(Res.string.active_pack_autoswitch))
                            Spacer(Modifier.weight(1f))
                            Switch(checked = isActive, onCheckedChange = { isActive = it })
                        }
                    }
                    
                    if (isActive) {
                        Text(stringResource(Res.string.switch_triggers), style = MaterialTheme.typography.titleSmall)
                        Text(
                            "The pack will activate if ANY scenario below matches. Within a scenario, ALL checked rules must match.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        autoSwitchGroups.forEachIndexed { groupIndex, group ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(stringResource(Res.string.scenario_format, groupIndex + 1), style = MaterialTheme.typography.labelLarge)
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = {
                                            autoSwitchGroups = autoSwitchGroups.filterIndexed { i, _ -> i != groupIndex }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.remove_scenario), modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    com.kapcode.open.macropad.kmps.models.MatchTarget.entries.forEach { target ->
                                        val rule = group.rules.find { it.target == target }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Checkbox(
                                                checked = rule != null,
                                                onCheckedChange = { checked ->
                                                    val newRules = if (checked) {
                                                        group.rules + com.kapcode.open.macropad.kmps.models.AutoSwitchRule(
                                                            target,
                                                            com.kapcode.open.macropad.kmps.models.MatchOperator.CONTAINS,
                                                            ""
                                                        )
                                                    } else {
                                                        group.rules.filter { it.target != target }
                                                    }
                                                    autoSwitchGroups = autoSwitchGroups.mapIndexed { i, g ->
                                                        if (i == groupIndex) g.copy(rules = newRules) else g
                                                    }
                                                }
                                            )
                                            Text(target.name.replace("_", " "), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(100.dp))
                                            
                                            if (rule != null) {
                                                var expanded by remember { mutableStateOf(false) }
                                                Box {
                                                    OutlinedButton(
                                                        onClick = { expanded = true },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.width(120.dp)
                                                    ) {
                                                        Text(rule.operator.name, style = MaterialTheme.typography.labelSmall)
                                                    }
                                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                                        com.kapcode.open.macropad.kmps.models.MatchOperator.entries.forEach { op ->
                                                            DropdownMenuItem(
                                                                text = { Text(op.name, style = MaterialTheme.typography.labelSmall) },
                                                                onClick = {
                                                                    val newRules = group.rules.map { if (it.target == target) it.copy(operator = op) else it }
                                                                    autoSwitchGroups = autoSwitchGroups.mapIndexed { i, g ->
                                                                        if (i == groupIndex) g.copy(rules = newRules) else g
                                                                    }
                                                                    expanded = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }

                                                OutlinedTextField(
                                                    value = rule.value,
                                                    onValueChange = { newValue ->
                                                        val newRules = group.rules.map { if (it.target == target) it.copy(value = newValue) else it }
                                                        autoSwitchGroups = autoSwitchGroups.mapIndexed { i, g ->
                                                            if (i == groupIndex) g.copy(rules = newRules) else g
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    textStyle = MaterialTheme.typography.bodySmall,
                                                    singleLine = true
                                                )

                                                AppTooltipArea(tooltipText = stringResource(Res.string.ignore_case)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Aa", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp))
                                                        Switch(
                                                            checked = rule.ignoreCase,
                                                            onCheckedChange = { checked ->
                                                                val newRules = group.rules.map { if (it.target == target) it.copy(ignoreCase = checked) else it }
                                                                autoSwitchGroups = autoSwitchGroups.mapIndexed { i, g ->
                                                                    if (i == groupIndex) g.copy(rules = newRules) else g
                                                                }
                                                            },
                                                            modifier = Modifier.scale(0.7f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                autoSwitchGroups = autoSwitchGroups + com.kapcode.open.macropad.kmps.models.AutoSwitchGroup()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.add_matching_scenario))
                        }
                    }
                }

                VerticalDivider()

                // Right side: Widgets and Routines
                Column(
                    modifier = Modifier.weight(1f).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Widgets Section
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(Res.string.widgets), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = {
                                println("[PackEditor] Add Widget clicked.")
                                widgetToEdit = null
                                showWidgetEditor = true
                            }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add_widget))
                            }
                        }

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(widgets) { widget ->
                                ListItem(
                                    headlineContent = { Text(widget.label) },
                                    supportingContent = { Text(stringResource(Res.string.macro_label_format, availableMacros.find { it.id == widget.macroId }?.name ?: "Unknown")) },
                                    trailingContent = {
                                        Row {
                                            IconButton(onClick = {
                                                println("[PackEditor] Edit Widget clicked for: ${widget.id}")
                                                widgetToEdit = widget
                                                showWidgetEditor = true
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.action_edit))
                                            }
                                            IconButton(onClick = {
                                                widgets = widgets.filter { it.id != widget.id }
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.action_delete))
                                            }
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }

                    HorizontalDivider()

                    // Routines Section
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(Res.string.advanced_routines), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = {
                                println("[PackEditor] Add Routine clicked.")
                                routineToEdit = null
                                showRoutineEditor = true
                            }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add_routine))
                            }
                        }

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(routines) { routine ->
                                ListItem(
                                    headlineContent = { Text(routine.name) },
                                    supportingContent = { Text(stringResource(Res.string.triggers_count_format, routine.triggers.size)) },
                                    trailingContent = {
                                        Row {
                                            IconButton(onClick = {
                                                println("[PackEditor] Edit Routine clicked for: ${routine.id}")
                                                routineToEdit = routine
                                                showRoutineEditor = true
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.action_edit))
                                            }
                                            IconButton(onClick = {
                                                routines = routines.filter { it.id != routine.id }
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.action_delete))
                                            }
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWidgetEditor) {
        println("[PackEditor] Launching WidgetEditorDialog.")
        WidgetEditorDialog(
            initialWidget = widgetToEdit,
            availableMacros = availableMacros,
            onDismissRequest = { showWidgetEditor = false },
            onSave = { newWidget ->
                widgets = if (widgetToEdit == null) {
                    widgets + newWidget
                } else {
                    widgets.map { if (it.id == newWidget.id) newWidget else it }
                }
                showWidgetEditor = false
            },
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            icon = icon
        )
    }

    if (showRoutineEditor) {
        println("[PackEditor] Launching RoutineEditorDialog.")
        RoutineEditorDialog(
            initialRoutine = routineToEdit,
            macroManagerViewModel = macroManagerViewModel,
            onDismissRequest = { showRoutineEditor = false },
            onSave = { updatedRoutine ->
                routines = if (routineToEdit == null) {
                    routines + updatedRoutine
                } else {
                    routines.map { if (it.id == updatedRoutine.id) updatedRoutine else it }
                }
                showRoutineEditor = false
            },
            selectedTheme = selectedTheme,
            consoleViewModel = consoleViewModel,
            icon = icon
        )
    }
}
