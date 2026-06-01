package com.kapcode.open.macropad.kmps.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.delay
import com.kapcode.open.macropad.kmps.desktop.model.ClientInfo
import com.kapcode.open.macropad.kmps.desktop.viewmodel.ConsoleViewModel
import com.kapcode.open.macropad.kmps.desktop.viewmodel.PairingViewModel
import com.kapcode.open.macropad.kmps.desktop.ui.pairing.SmallPairingLayout
import com.kapcode.open.macropad.kmps.desktop.ui.pairing.UnifiedPairingLayout

@Composable
fun PairingRequestDialog(
    requests: List<ClientInfo>,
    selectedTheme: String,
    consoleViewModel: ConsoleViewModel,
    pairingViewModel: PairingViewModel,
    isAlwaysAllowAvailable: Boolean = true,
    onApprove: (String, String, Boolean) -> Unit,
    onDeny: (String) -> Unit,
    onBan: (String, String) -> Unit,
    onCancelAll: () -> Unit
) {
    val fleetMode by pairingViewModel.fleetModeEnabled.collectAsState()
    
    // Dynamically update window size when fleetMode changes
    val windowState = rememberWindowState(
        width = if (fleetMode) 1600.dp else 1000.dp,
        height = if (fleetMode) 1200.dp else 800.dp
    )
    
    // Sync window state if fleetMode changes after initial composition
    LaunchedEffect(fleetMode) {
        val targetSize = if (fleetMode) {
            androidx.compose.ui.unit.DpSize(1600.dp, 1200.dp)
        } else {
            androidx.compose.ui.unit.DpSize(1000.dp, 800.dp)
        }
        
        if (windowState.size != targetSize) {
            // Add a very small delay before resizing to let the OS stabilize if just opened
            delay(50)
            windowState.size = targetSize
        }
    }

    AppDialog(
        onCloseRequest = onCancelAll,
        state = windowState,
        title = if (fleetMode) "PAIRING AND SYNC (FLEET) (${requests.size} Devices)" else "DEVICE SYNC",
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel,
        resizable = true
    ) {
        // Use the window state size directly instead of reading it during composition to avoid loops
        val currentWidth = remember(windowState.size) { windowState.size.width }
        val currentHeight = remember(windowState.size) { windowState.size.height }

        Box(modifier = Modifier.fillMaxSize()) {
            if (!fleetMode) {
                SmallPairingLayout(
                    requests = requests,
                    pairingViewModel = pairingViewModel,
                    onApprove = onApprove,
                    onDeny = onDeny,
                    onBan = onBan,
                    isAlwaysAllowAvailable = isAlwaysAllowAvailable,
                    onClose = onCancelAll
                )
            } else {
                UnifiedPairingLayout(
                    requests = requests,
                    pairingViewModel = pairingViewModel,
                    onApprove = onApprove,
                    onDeny = onDeny,
                    onBan = onBan,
                    isAlwaysAllowAvailable = isAlwaysAllowAvailable,
                    onClose = onCancelAll,
                    maxWidth = currentWidth,
                    maxHeight = currentHeight
                )
            }
        }
    }
}
