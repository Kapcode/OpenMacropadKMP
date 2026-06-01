package com.kapcode.open.macropad.kmps

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.kapcode.`open`.macropad.kmps.Res
import com.kapcode.`open`.macropad.kmps.*
import com.kapcode.open.macropad.kmps.utils.ClipboardManager
import com.kapcode.open.macropad.kmps.utils.LocalClipboardManager
import com.kapcode.open.macropad.kmps.hardware.HardwareTriggerManager
import com.kapcode.open.macropad.kmps.settings.AppTheme as SettingsAppTheme
import com.kapcode.open.macropad.kmps.settings.SettingsScreen
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel
import com.kapcode.open.macropad.kmps.ui.components.CommonAppBar
import com.kapcode.open.macropad.kmps.ui.components.LoadingIndicator
import com.kapcode.open.macropad.kmps.ui.components.LegalDialog
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme
import com.kapcode.open.macropad.kmps.models.TrustedServer
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

@Composable
fun MainUI(
    settingsViewModel: SettingsViewModel,
    clientDiscovery: ClientDiscovery,
    hardwareTriggerManager: HardwareTriggerManager? = null,
    billingManager: BillingManager? = null,
    onLaunchClient: (ServerInfo, String) -> Unit,
    onOkayTriggerSet: (() -> Unit) -> Unit
) {
    val theme by settingsViewModel.theme.collectAsState()
    val legalAccepted by settingsViewModel.legalAccepted.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showAd by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val foundServers by clientDiscovery.foundServers.collectAsState()
    val isScanning by clientDiscovery.isScanning.collectAsState()
    val isGlobalLoading by settingsViewModel.isGlobalLoading.collectAsState()

    val defaultServerAddress = remember { mutableStateOf(ServerStorage.getDefaultServer(context)) }

    // Define the "Okay" action
    val onOkayAction = {
        val currentServers = foundServers
        val defaultAddr = defaultServerAddress.value
        
        if (currentServers.size == 1) {
            // Only one server, connect to it
            val server = currentServers.first()
            onLaunchClient(
                ServerInfo(server.name, server.address, server.isSecure, server.fingerprint),
                "${DeviceInfo.name}-${DeviceInfo.uniqueId}"
            )
        } else if (defaultAddr != null) {
            // Multiple servers, but we have a default
            currentServers.find { it.address == defaultAddr }?.let { server ->
                onLaunchClient(
                    ServerInfo(server.name, server.address, server.isSecure, server.fingerprint),
                    "${DeviceInfo.name}-${DeviceInfo.uniqueId}"
                )
            }
        }
    }

    LaunchedEffect(foundServers, defaultServerAddress.value) {
        onOkayTriggerSet(onOkayAction)
    }

    LaunchedEffect(Unit) {
        delay(800) // Delay ad loading further to keep the transition smooth
        showAd = true
    }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    LaunchedEffect(showSettings) {
        if (showSettings) {
            MacroApplication.analyticsManager.trackScreen("Settings", "MainUI")
        } else {
            MacroApplication.analyticsManager.trackScreen("ServerList", "MainUI")
        }
    }

    AppTheme(useDarkTheme = theme == SettingsAppTheme.DarkBlue) {
        if (!legalAccepted) {
            LegalDialog(
                onAccept = { settingsViewModel.setLegalAccepted(true) },
                onDeny = { /* Stay shown */ }
            )
        }

        CompositionLocalProvider(LocalClipboardManager provides ClipboardManager()) {
            Scaffold(
                topBar = {
                    CommonAppBar(
                        title = if (showSettings) stringResource(Res.string.settings) else stringResource(Res.string.app_name),
                        onSettingsClick = { showSettings = !showSettings },
                        navigationIcon = {
                            if (showSettings) {
                                IconButton(onClick = { showSettings = false }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                                }
                            }
                        },
                        isPro = settingsViewModel.isPro.collectAsState().value,
                        isAdFree = settingsViewModel.isAdFree.collectAsState().value,
                        isDeveloperMode = settingsViewModel.isDeveloperMode.collectAsState().value,
                        onProPurchaseClick = { settingsViewModel.setIsPro(!settingsViewModel.isPro.value) },
                        onAdFreePurchaseClick = { settingsViewModel.setIsAdFree(!settingsViewModel.isAdFree.value) },
                        billingManager = billingManager
                    )
                },
            bottomBar = { 
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                val isPro = settingsViewModel.isPro.collectAsState().value
                val isAdFree = settingsViewModel.isAdFree.collectAsState().value
                val isServerPro = settingsViewModel.isServerProActive.collectAsState().value
                val adsDisabled = isPro || isAdFree || isServerPro
                val shouldShowAd = showAd && !isLandscape && !isGlobalLoading && !showSettings && !adsDisabled && !AdVisibilityManager.isForegroundAdVisible
                
                if (shouldShowAd) {
                    BottomAppBar { AdmobBanner(location = AdLocation.MAIN) }
                }
            }
        ) { innerPadding ->
            if (showSettings) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                if (isGlobalLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                } else {
                    val serverInfos = remember(foundServers, defaultServerAddress.value) {
                        foundServers.map {
                            ServerInfo(
                                name = it.name,
                                address = it.address,
                                isSecure = it.isSecure,
                                fingerprint = it.fingerprint,
                                isDefault = it.address == defaultServerAddress.value
                            )
                        }
                    }
                    App(
                        modifier = Modifier.padding(innerPadding),
                        hardwareTriggerManager = hardwareTriggerManager,
                        scanServers = {
                            clientDiscovery.foundServers.value = emptyList()
                            clientDiscovery.start()
                        },
                        stopScanning = { clientDiscovery.stop() },
                        foundServers = serverInfos,
                        isScanning = isScanning,
                        onConnectClick = { serverInfo, deviceName ->
                            if (serverInfo.name == TrustedServer.ACTION_SET_DEFAULT) {
                                val newDefault = if (serverInfo.address == defaultServerAddress.value) null else serverInfo.address
                                ServerStorage.setDefaultServer(context, newDefault)
                                defaultServerAddress.value = newDefault
                            } else {
                                onLaunchClient(serverInfo, deviceName)
                            }
                        }
                    )
                }
            }
        }
    }
}}
