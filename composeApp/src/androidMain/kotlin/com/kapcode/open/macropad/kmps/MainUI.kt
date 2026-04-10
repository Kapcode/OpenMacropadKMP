package com.kapcode.open.macropad.kmps

import android.content.Intent
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
import com.kapcode.open.macropad.kmps.settings.AppTheme as SettingsAppTheme
import com.kapcode.open.macropad.kmps.settings.SettingsScreen
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel
import com.kapcode.open.macropad.kmps.ui.components.CommonAppBar
import com.kapcode.open.macropad.kmps.ui.components.LoadingIndicator
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme
import kotlinx.coroutines.delay

@Composable
fun MainUI(
    settingsViewModel: SettingsViewModel,
    clientDiscovery: ClientDiscovery,
    onLaunchClient: (ServerInfo, String) -> Unit
) {
    val theme by settingsViewModel.theme.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showAd by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(800) // Delay ad loading further to keep the transition smooth
        showAd = true
    }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    AppTheme(useDarkTheme = theme == SettingsAppTheme.DarkBlue) {
        Scaffold(
            topBar = {
                CommonAppBar(
                    title = if (showSettings) "Settings" else "Open Macropad",
                    onSettingsClick = { showSettings = !showSettings },
                    navigationIcon = {
                        if (showSettings) {
                            IconButton(onClick = { showSettings = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    }
                )
            },
            bottomBar = { 
                if (showAd) {
                    BottomAppBar { AdmobBanner() }
                }
            }
        ) { innerPadding ->
            if (showSettings) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                val foundServers by clientDiscovery.foundServers.collectAsState()
                val isScanning by clientDiscovery.isScanning.collectAsState()
                val isGlobalLoading by settingsViewModel.isGlobalLoading.collectAsState()
                
                if (isGlobalLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                } else {
                    val serverInfos = remember(foundServers) {
                        foundServers.map {
                            ServerInfo(
                                name = it.name,
                                address = it.address,
                                isSecure = it.isSecure,
                                fingerprint = it.fingerprint
                            )
                        }
                    }
                    App(
                        modifier = Modifier.padding(innerPadding),
                        scanServers = {
                            clientDiscovery.foundServers.value = emptyList()
                            clientDiscovery.start()
                        },
                        stopScanning = { clientDiscovery.stop() },
                        foundServers = serverInfos,
                        isScanning = isScanning,
                        onConnectClick = { serverInfo, deviceName ->
                            onLaunchClient(serverInfo, deviceName)
                        }
                    )
                }
            }
        }
    }
}
