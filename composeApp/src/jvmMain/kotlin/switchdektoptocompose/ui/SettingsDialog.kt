package switchdektoptocompose.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import switchdektoptocompose.viewmodel.*
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel as SharedSettingsViewModel
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import switchdektoptocompose.ui.settings.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    desktopViewModel: DesktopViewModel,
    settingsViewModel: SettingsViewModel,
    sharedSettingsViewModel: SharedSettingsViewModel,
    consoleViewModel: ConsoleViewModel,
    onDismissRequest: () -> Unit,
    onShowShortcutsRequest: () -> Unit = {},
    onShowPushSettingsRequest: () -> Unit = {},
    initialScrollToSecurity: Boolean = false
) {
    val serverPort by settingsViewModel.serverPort.collectAsState()
    val secureServerPort by settingsViewModel.secureServerPort.collectAsState()
    val selectedTheme by settingsViewModel.selectedTheme.collectAsState()
    val serverViewModel = desktopViewModel.serverViewModel
    val clientCommunicationViewModel = desktopViewModel.clientCommunicationViewModel
    val isServerRunning by serverViewModel.isServerRunning.collectAsState()
    val encryptionEnabled by serverViewModel.encryptionEnabled.collectAsState()
    val bannedDevices by clientCommunicationViewModel.bannedDevices.collectAsState()
    val trustedDevices by clientCommunicationViewModel.trustedDevices.collectAsState()
    val exitBehavior by settingsViewModel.exitBehavior.collectAsState()
    val clickTrayToToggle by settingsViewModel.clickTrayToToggle.collectAsState()
    val allowNewConnections by settingsViewModel.allowNewConnections.collectAsState()
    val allowOnceOnly by settingsViewModel.allowOnceOnly.collectAsState()
    val fleetModeEnabled by settingsViewModel.fleetModeEnabled.collectAsState()
    val enableWebsocketPings by settingsViewModel.enableWebsocketPings.collectAsState()
    val multiQrEnabled by sharedSettingsViewModel.multiQrEnabled.collectAsState()
    val defaultPairingModeQr by settingsViewModel.defaultPairingModeQr.collectAsState()
    val tooltipXOffset by settingsViewModel.tooltipXOffset.collectAsState()
    val tooltipYOffset by settingsViewModel.tooltipYOffset.collectAsState()
    val enablePackSwitchNotifications by settingsViewModel.enablePackSwitchNotifications.collectAsState()
    val enableToasts by settingsViewModel.enableToasts.collectAsState()
    val enableBackgroundToasts by settingsViewModel.enableBackgroundToasts.collectAsState()
    val enableNetworkToasts by settingsViewModel.enableNetworkToasts.collectAsState()
    val includeWindowNamesInToasts by settingsViewModel.includeWindowNamesInToasts.collectAsState()
    val toastDurationMs by settingsViewModel.toastDurationMs.collectAsState()
    val toastTarget by settingsViewModel.toastTarget.collectAsState()
    val notificationClientIds by settingsViewModel.notificationClientIds.collectAsState()

    // Connected Clients Settings
    val clientTheme by settingsViewModel.clientTheme.collectAsState()
    val clientAnalyticsEnabled by settingsViewModel.clientAnalyticsEnabled.collectAsState()
    val clientSlamFireEnabled by settingsViewModel.clientSlamFireEnabled.collectAsState()
    val clientSlamFireAction by settingsViewModel.clientSlamFireAction.collectAsState()

    // Scroll state management
    val scrollState = rememberScrollState()
    var securitySectionOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(initialScrollToSecurity, securitySectionOffset) {
        if (initialScrollToSecurity && securitySectionOffset > 0) {
            scrollState.animateScrollTo(securitySectionOffset.toInt())
        }
    }

    AppDialog(
        onCloseRequest = onDismissRequest,
        state = rememberWindowState(width = 600.dp, height = 700.dp),
        title = "Settings",
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 12.dp)
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                ThemeSettings(selectedTheme, settingsViewModel)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                BehaviorSettings(
                    exitBehavior, clickTrayToToggle, enablePackSwitchNotifications,
                    enableToasts, enableBackgroundToasts, enableNetworkToasts, includeWindowNamesInToasts,
                    toastDurationMs, toastTarget, notificationClientIds, trustedDevices,
                    settingsViewModel, onShowShortcutsRequest
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                UISettings(tooltipXOffset, tooltipYOffset, settingsViewModel)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ClientSettings(clientTheme, clientAnalyticsEnabled, clientSlamFireEnabled, clientSlamFireAction, settingsViewModel, onShowPushSettingsRequest)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                NetworkSettings(
                    serverPort, secureServerPort, encryptionEnabled, isServerRunning,
                    defaultPairingModeQr, allowNewConnections, multiQrEnabled, fleetModeEnabled,
                    allowOnceOnly, enableWebsocketPings, settingsViewModel, sharedSettingsViewModel,
                    serverViewModel, onShowPushSettingsRequest
                ) { securitySectionOffset = it }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                DeviceManagement(trustedDevices, bannedDevices, clientCommunicationViewModel)

                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState),
                style = ScrollbarStyle(
                    minimalHeight = 16.dp,
                    thickness = 8.dp,
                    shape = MaterialTheme.shapes.small,
                    hoverDurationMillis = 300,
                    unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f)
                )
            )
        }
    }
}
