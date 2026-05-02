package switchdektoptocompose.ui

import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.*
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import javax.swing.SwingUtilities
import javax.swing.UIManager
import java.io.File
import androidx.compose.ui.tooling.preview.Preview
import switchdektoptocompose.logic.ProcessWatcher
import switchdektoptocompose.di.DesktopViewModels
import switchdektoptocompose.viewmodel.*
import switchdektoptocompose.ui.components.AppTooltipArea
import switchdektoptocompose.ui.components.LocalTooltipSettings
import switchdektoptocompose.ui.components.TooltipSettings
import switchdektoptocompose.viewmodel.SettingsViewModel as DesktopSettingsViewModel
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel as SharedSettingsViewModel
import switchdektoptocompose.di.ViewModelFactory

@Preview
@Composable
fun DesktopAppPreview() {
    val viewModels = ViewModelFactory.createViewModels()
    val desktopWindowState = rememberDesktopWindowState(
        settingsViewModel = viewModels.settingsViewModel,
        layoutViewModel = viewModels.layoutViewModel
    )

    DesktopApp(
        viewModels = viewModels,
        desktopWindowState = desktopWindowState
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSplitPaneApi::class)
@Composable
fun DesktopApp(
    viewModels: DesktopViewModels,
    desktopWindowState: DesktopWindowState,
    onExit: () -> Unit = {}
) {
    val desktopViewModel = viewModels.desktopViewModel
    val serverViewModel = viewModels.serverViewModel
    val clientCommunicationViewModel = viewModels.clientCommunicationViewModel
    val consoleViewModel = viewModels.consoleViewModel
    val inspectorViewModel = viewModels.inspectorViewModel
    val recordMacroViewModel = viewModels.recordMacroViewModel
    val macroEditorViewModel = viewModels.macroEditorViewModel
    val macroManagerViewModel = viewModels.macroManagerViewModel
    val settingsViewModel = viewModels.settingsViewModel
    val macroTimelineViewModel = viewModels.macroTimelineViewModel
    val newEventViewModel = viewModels.newEventViewModel
    val layoutViewModel = viewModels.layoutViewModel

    val mainTab by layoutViewModel.mainTab.collectAsState()

    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()
    val allowOnceOnly by settingsViewModel.allowOnceOnly.collectAsState()
    val allowNewConnections by settingsViewModel.allowNewConnections.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val logs by consoleViewModel.logMessages.collectAsState()
    LaunchedEffect(logs) {
        if (logs.isNotEmpty()) {
            val lastLog = logs.last()
            if (lastLog.formatted.contains("MACRO FINISHED") || lastLog.formatted.contains("MACRO CANCELLED") || lastLog.formatted.contains("E-STOP") || lastLog.formatted.contains("DIALOG CLOSED")) {
                 snackbarHostState.showSnackbar(lastLog.formatted)
            }
        }
    }


    val isServerRunning by serverViewModel.isServerRunning.collectAsState()
    val connectedDevices by clientCommunicationViewModel.connectedDevices.collectAsState()
    val serverIpAddress by serverViewModel.serverIpAddress.collectAsState()
    val encryptionEnabled by serverViewModel.encryptionEnabled.collectAsState()
    val isMacroExecutionEnabled by clientCommunicationViewModel.isMacroExecutionEnabled.collectAsState()
    val connectionHistory by clientCommunicationViewModel.connectionHistory.collectAsState()
    val trustedDevices by clientCommunicationViewModel.trustedDevices.collectAsState()
    val totalCurrencySpent by clientCommunicationViewModel.totalCurrencySpent.collectAsState()

    val serverPort by settingsViewModel.serverPort.collectAsState()
    val secureServerPort by settingsViewModel.secureServerPort.collectAsState()
    val currentPort = if (encryptionEnabled) secureServerPort else serverPort
    val eStopKey by settingsViewModel.eStopKey.collectAsState()
    
    val tooltipXOffset by settingsViewModel.tooltipXOffset.collectAsState()
    val tooltipYOffset by settingsViewModel.tooltipYOffset.collectAsState()

    val exitBehavior by settingsViewModel.exitBehavior.collectAsState()

    AppTheme(useDarkTheme = selectedTheme == "Dark Blue") {
        CompositionLocalProvider(
            LocalTooltipSettings provides TooltipSettings(xOffset = tooltipXOffset, yOffset = tooltipYOffset)
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                modifier = Modifier.fillMaxSize()
            ) { paddingValues ->
            Surface(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                val rootVerticalSplitter = rememberSplitPaneState(
                    initialPositionPercentage = layoutViewModel.getSplitterPosition("Root Layout", 0.0254f)
                )
                val mainHorizontalSplitter = rememberSplitPaneState(
                    initialPositionPercentage = layoutViewModel.getSplitterPosition("Main Horizontal", 0.2965f)
                )
                val secondaryPanelSplitter = rememberSplitPaneState(
                    initialPositionPercentage = layoutViewModel.getSplitterPosition("Secondary Panel", 0.1564f)
                )
                val sidebarSplitter = rememberSplitPaneState(
                    initialPositionPercentage = layoutViewModel.getSplitterPosition("Sidebar Split", 0.7073f)
                )

                MoveableVerticalSplitPane(
                    name = "Root Layout",
                    firstName = "Header",
                    secondName = "Main Content",
                    splitPaneState = rootVerticalSplitter,
                    consoleViewModel = consoleViewModel,
                    settingsViewModel = settingsViewModel,
                    firstMinSize = 48.dp,
                    secondMinSize = 200.dp,
                    first = {
                        DesktopTopBar(
                            desktopViewModel = desktopViewModel,
                            settingsViewModel = settingsViewModel,
                            isServerRunning = isServerRunning,
                            connectedDevicesCount = connectedDevices.size,
                            isMacroExecutionEnabled = isMacroExecutionEnabled,
                            serverIpAddress = serverIpAddress,
                            currentPort = currentPort,
                            encryptionEnabled = encryptionEnabled,
                            allowOnceOnly = allowOnceOnly,
                            allowNewConnections = allowNewConnections,
                            selectedTheme = selectedTheme,
                            exitBehavior = exitBehavior,
                            eStopKey = eStopKey,
                            onShowSettings = { desktopWindowState.toggleSettings(true) },
                            onShowShortcuts = { desktopWindowState.toggleShortcuts(true) },
                            onExit = onExit,
                            onShowExitDialog = { desktopWindowState.toggleExitDialog(true) }
                        )
                    },
                    second = {
                        MoveableHorizontalSplitPane(
                            name = "Main Horizontal",
                            firstName = "Left Sidebar",
                            secondName = "Right Panel",
                            splitPaneState = mainHorizontalSplitter,
                            consoleViewModel = consoleViewModel,
                            settingsViewModel = settingsViewModel,
                            firstMinSize = 150.dp,
                            secondMinSize = 500.dp,
                            first = {
                                MoveableVerticalSplitPane(
                                    name = "Sidebar Split",
                                    firstName = "Console",
                                    secondName = "Inspector",
                                    splitPaneState = sidebarSplitter,
                                    consoleViewModel = consoleViewModel,
                                    settingsViewModel = settingsViewModel,
                                    firstMinSize = 100.dp,
                                    secondMinSize = 100.dp,
                                    first = {
                                        Box(
                                            modifier = Modifier.fillMaxSize()
                                                .background(panelBackground(1))
                                                .padding(8.dp)
                                        ) {
                                            Console(viewModel = consoleViewModel)
                                        }
                                    },
                                    second = {
                                        Box(
                                            modifier = Modifier.fillMaxSize()
                                                .background(panelBackground(2))
                                                .padding(8.dp)
                                        ) {
                                            InspectorScreen(
                                                viewModel = inspectorViewModel, 
                                                macroManagerViewModel = macroManagerViewModel,
                                                onOpenVariableSettings = {
                                                    desktopWindowState.toggleSettings(true)
                                                }
                                            )
                                        }
                                    }
                                )
                            },
                            second = {
                                MoveableHorizontalSplitPane(
                                    name = "Secondary Panel",
                                    firstName = "Connections",
                                    secondName = "Macros",
                                    splitPaneState = secondaryPanelSplitter,
                                    consoleViewModel = consoleViewModel,
                                    settingsViewModel = settingsViewModel,
                                    firstMinSize = 250.dp,
                                    secondMinSize = 500.dp,
                                    first = {
                                        Box(modifier = Modifier.fillMaxSize().background(panelBackground(3)).padding(8.dp)) {
                                            ConnectedDevicesScreen(
                                                devices = connectedDevices,
                                                history = connectionHistory,
                                                trustedDevices = trustedDevices,
                                                totalCurrencySpent = totalCurrencySpent,
                                                onDisconnect = { desktopViewModel.disconnectClient(it) },
                                                onUnpair = { desktopViewModel.unpairDevice(it) },
                                                onBan = { desktopViewModel.banDevice(it.id, it.name) },
                                                onUnban = { desktopViewModel.unbanDevice(it) },
                                                onClearHistory = { desktopViewModel.clearConnectionHistory() }
                                            )
                                        }
                                    },
                                    second = {
                                        MacroEditingArea(
                                            macroManagerViewModel = macroManagerViewModel,
                                            macroEditorViewModel = macroEditorViewModel,
                                            macroTimelineViewModel = macroTimelineViewModel,
                                            settingsViewModel = settingsViewModel,
                                            consoleViewModel = consoleViewModel,
                                            selectedTheme = selectedTheme,
                                            onAddEventClicked = {
                                                newEventViewModel.reset()
                                                desktopWindowState.toggleNewEventDialog(true)
                                            },
                                            onRecordMacroClicked = {
                                                recordMacroViewModel.reset()
                                                desktopWindowState.toggleRecordDialog(true)
                                            },
                                            onEditEventClicked = { event, index ->
                                                newEventViewModel.loadFromEvent(event, index)
                                                desktopWindowState.toggleNewEventDialog(true)
                                            },
                                            onEditTriggerClicked = { trigger ->
                                                newEventViewModel.loadFromTrigger(trigger)
                                                desktopWindowState.toggleNewEventDialog(true)
                                            },
                                            onMarketplaceClicked = {
                                                desktopWindowState.toggleMarketplace(true)
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }
                )
            }
        }
    }
}
}
