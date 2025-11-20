package switchdektoptocompose

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import javax.swing.SwingUtilities
import javax.swing.UIManager

@OptIn(ExperimentalMaterial3Api::class)
fun main() = application {
    // Set the initial Look and Feel
    UIManager.setLookAndFeel(FlatDarkLaf())

    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
    val settingsViewModel = remember { SettingsViewModel() }
    val newEventViewModel = remember { NewEventViewModel() }
    val consoleViewModel = remember { ConsoleViewModel() }
    val inspectorViewModel = remember { InspectorViewModel(consoleViewModel) }
    val recordMacroViewModel = remember { RecordMacroViewModel() }
    val desktopViewModel = remember { DesktopViewModel(settingsViewModel, consoleViewModel) }
    lateinit var macroManagerViewModel: MacroManagerViewModel
    
    var isWindowVisible by remember { mutableStateOf(true) }
    val minimizeToTray by settingsViewModel.minimizeToTray.collectAsState()
    val icon = painterResource("macropadIcon64.png")

    val macroEditorViewModel = remember {
        MacroEditorViewModel(settingsViewModel) {
            macroManagerViewModel.refresh()
        }
    }

    macroManagerViewModel = remember {
        MacroManagerViewModel(
            settingsViewModel = settingsViewModel,
            consoleViewModel = consoleViewModel,
            onEditMacroRequested = { macroState ->
                macroEditorViewModel.openOrSwitchToTab(macroState)
            },
            onMacrosUpdated = {
                desktopViewModel.sendMacroListToAllClients()
            }
        )
    }

    desktopViewModel.macroManagerViewModel = macroManagerViewModel

    val macroTimelineViewModel = remember { MacroTimelineViewModel(macroEditorViewModel) }

    val triggerListener = remember {
        TriggerListener(desktopViewModel) { macroToPlay ->
            macroManagerViewModel.onPlayMacro(macroToPlay)
        }
    }

    val inspectorManager = remember { InspectorManager(inspectorViewModel, consoleViewModel) }

    DisposableEffect(Unit) {
        desktopViewModel.startServer()
        triggerListener.startListening()
        inspectorManager.startListening()
        onDispose {
            desktopViewModel.shutdown()
            triggerListener.shutdown()
            inspectorManager.stopListening()
        }
    }
    
    Tray(
        icon = icon,
        tooltip = "Open Macropad Server",
        menu = {
            Item("Show", onClick = { isWindowVisible = true })
            Separator()
            Item("Exit", onClick = ::exitApplication)
        }
    )

    if (isWindowVisible) {
        Window(
            onCloseRequest = {
                if (minimizeToTray) {
                    isWindowVisible = false
                } else {
                    exitApplication()
                }
            },
            state = windowState,
            title = "Open Macropad (Compose)",
            icon = icon
        ) {
            val macroFiles by macroManagerViewModel.macroFiles.collectAsState()
            val eStopKey by settingsViewModel.eStopKey.collectAsState()

            LaunchedEffect(macroFiles, eStopKey) {
                triggerListener.updateActiveTriggers(macroFiles, eStopKey)
            }

            DesktopApp(
                desktopViewModel = desktopViewModel,
                consoleViewModel = consoleViewModel,
                inspectorViewModel = inspectorViewModel,
                recordMacroViewModel = recordMacroViewModel,
                macroEditorViewModel = macroEditorViewModel,
                macroManagerViewModel = macroManagerViewModel,
                settingsViewModel = settingsViewModel,
                macroTimelineViewModel = macroTimelineViewModel,
                newEventViewModel = newEventViewModel,
                onExit = ::exitApplication
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
@Preview
fun DesktopApp(
    desktopViewModel: DesktopViewModel,
    consoleViewModel: ConsoleViewModel,
    inspectorViewModel: InspectorViewModel,
    recordMacroViewModel: RecordMacroViewModel,
    macroEditorViewModel: MacroEditorViewModel,
    macroManagerViewModel: MacroManagerViewModel,
    settingsViewModel: SettingsViewModel,
    macroTimelineViewModel: MacroTimelineViewModel,
    newEventViewModel: NewEventViewModel,
    onExit: () -> Unit = {}
) {
    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedTheme) {
        val laf = if (selectedTheme == "Dark Blue") FlatDarkLaf::class.java.name else FlatLightLaf::class.java.name
        UIManager.setLookAndFeel(laf)
        for (window in java.awt.Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window)
        }
    }
    
    val logs by consoleViewModel.logMessages.collectAsState()
    LaunchedEffect(logs) {
        if (logs.isNotEmpty()) {
            val lastLog = logs.last()
            if (lastLog.contains("MACRO FINISHED") || lastLog.contains("MACRO CANCELLED") || lastLog.contains("E-STOP")) {
                 snackbarHostState.showSnackbar(lastLog)
            }
        }
    }


    val connectedDevices by desktopViewModel.connectedDevices.collectAsState()
    val isServerRunning by desktopViewModel.isServerRunning.collectAsState()
    val serverIpAddress by desktopViewModel.serverIpAddress.collectAsState()
    val encryptionEnabled by desktopViewModel.encryptionEnabled.collectAsState()
    val isMacroExecutionEnabled by desktopViewModel.isMacroExecutionEnabled.collectAsState()
    val serverPort by settingsViewModel.serverPort.collectAsState()
    val secureServerPort by settingsViewModel.secureServerPort.collectAsState()
    val currentPort = if (encryptionEnabled) secureServerPort else serverPort
    val filePendingDeletion by macroManagerViewModel.filePendingDeletion.collectAsState()
    val filesPendingDeletion by macroManagerViewModel.filesPendingDeletion.collectAsState()
    val eStopKey by settingsViewModel.eStopKey.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNewEventDialog by remember { mutableStateOf(false) }
    var showRecordDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        SettingsDialog(
            desktopViewModel = desktopViewModel,
            settingsViewModel = settingsViewModel,
            onDismissRequest = { showSettingsDialog = false }
        )
    }
    filePendingDeletion?.let { file ->
        ConfirmDeleteDialog(file = file, onConfirm = { macroManagerViewModel.confirmDeletion() }, onDismiss = { macroManagerViewModel.cancelDeletion() })
    }
    filesPendingDeletion?.let { files ->
        ConfirmDeleteMultipleDialog(files = files, onConfirm = { macroManagerViewModel.confirmMultipleDeletion() }, onDismiss = { macroManagerViewModel.cancelMultipleDeletion() })
    }
    if (showNewEventDialog) {
        NewEventDialog(
            viewModel = newEventViewModel,
            selectedTheme = selectedTheme,
            onDismissRequest = { showNewEventDialog = false },
            onAddEvent = {
                if (newEventViewModel.isTriggerEvent.value) {
                    macroTimelineViewModel.addOrUpdateTrigger(
                        keyName = newEventViewModel.keysText.value,
                        allowedClients = newEventViewModel.allowedClientsText.value
                    )
                } else {
                    val events = newEventViewModel.createEvents()
                    macroTimelineViewModel.addEvents(events)
                }
                showNewEventDialog = false
            }
        )
    }
    
    if (showRecordDialog) {
        RecordMacroDialog(
            viewModel = recordMacroViewModel,
            selectedTheme = selectedTheme,
            onDismissRequest = { showRecordDialog = false },
            onStartRecording = {
                macroManagerViewModel.startRecording(recordMacroViewModel)
                showRecordDialog = false
            }
        )
    }

    AppTheme(useDarkTheme = selectedTheme == "Dark Blue") {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Surface(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                val rootVerticalSplitter = rememberSplitPaneState(initialPositionPercentage = 0.2f)
                val mainHorizontalSplitter = rememberSplitPaneState(initialPositionPercentage = 0.1f)

                VerticalSplitPane(splitPaneState = rootVerticalSplitter) {
                    first(minSize = 100.dp) {
                        Row(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var menuExpanded by remember { mutableStateOf(false) }
                            TooltipArea(tooltip = { Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp){ Text("Menu", modifier = Modifier.padding(4.dp)) } }, delayMillis = 0) {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Start Server") },
                                    onClick = { desktopViewModel.startServer(); menuExpanded = false },
                                    enabled = !isServerRunning,
                                    leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Stop Server") },
                                    onClick = { desktopViewModel.stopServer(); menuExpanded = false },
                                    enabled = isServerRunning,
                                    leadingIcon = { Icon(Icons.Default.Stop, null) }
                                )
                                Divider()
                                DropdownMenuItem(text = { Text("Settings") }, onClick = { showSettingsDialog = true; menuExpanded = false }, leadingIcon = { Icon(Icons.Default.Settings, null) })
                                Divider()
                                DropdownMenuItem(text = { Text("Exit") }, onClick = onExit, leadingIcon = { Icon(Icons.Default.ExitToApp, null) })
                            }
                            
                            Spacer(Modifier.width(16.dp))

                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight().padding(8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Column {
                                    Text("Status: ${if (isServerRunning) "Running" else "Stopped"}", color = if (isServerRunning) Color.Green else Color.Red)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Address: $serverIpAddress:$currentPort")
                                }
                            }
                            
                            TooltipArea(tooltip = { Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp){ Text("Emergency Stop Key", modifier = Modifier.padding(4.dp)) } }, delayMillis = 0) {
                                var eStopMenuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(onClick = { eStopMenuExpanded = true }) {
                                        Text("E-Stop: $eStopKey")
                                    }
                                    DropdownMenu(
                                        expanded = eStopMenuExpanded,
                                        onDismissRequest = { eStopMenuExpanded = false },
                                        modifier = Modifier.heightIn(max = 300.dp)
                                    ) {
                                        Box(modifier = Modifier.sizeIn(maxHeight = 300.dp)) {
                                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                                val fKeys = (1..12).map { "F$it" }
                                                fKeys.forEach { key ->
                                                    DropdownMenuItem(
                                                        text = { Text(key) },
                                                        onClick = {
                                                            settingsViewModel.setEStopKey(key)
                                                            eStopMenuExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            TooltipArea(tooltip = { Surface(shape = MaterialTheme.shapes.small, shadowElevation = 4.dp){ Text("Toggle Macro Execution", modifier = Modifier.padding(4.dp)) } }, delayMillis = 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Power, contentDescription = "Macros Enabled")
                                    Switch(
                                        checked = isMacroExecutionEnabled,
                                        onCheckedChange = { desktopViewModel.setMacroExecutionEnabled(it) }
                                    )
                                }
                            }
                            
                            Spacer(Modifier.width(16.dp))

                            Box(modifier = Modifier.weight(0.1f).fillMaxHeight()) {
                                InspectorScreen(viewModel = inspectorViewModel)
                            }
                        }
                    }
                    second(minSize = 200.dp) {
                        HorizontalSplitPane(splitPaneState = mainHorizontalSplitter) {
                            first(minSize = 250.dp) {
                               Column {
                                   Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
                                       ConnectedDevicesScreen(devices = connectedDevices)
                                   }
                                   Box(modifier = Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
                                       Console(viewModel = consoleViewModel)
                                   }
                               }
                            }
                            second(minSize = 500.dp) {
                                MacroEditingArea(
                                    macroManagerViewModel = macroManagerViewModel,
                                    macroEditorViewModel = macroEditorViewModel,
                                    macroTimelineViewModel = macroTimelineViewModel,
                                    onAddEventClicked = {
                                        newEventViewModel.reset()
                                        showNewEventDialog = true
                                    },
                                    onRecordMacroClicked = {
                                        recordMacroViewModel.reset()
                                        showRecordDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}