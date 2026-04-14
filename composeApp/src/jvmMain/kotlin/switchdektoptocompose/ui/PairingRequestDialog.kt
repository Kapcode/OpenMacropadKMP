package switchdektoptocompose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import switchdektoptocompose.model.ClientInfo
import switchdektoptocompose.viewmodel.ConsoleViewModel
import switchdektoptocompose.viewmodel.PairingViewModel
import switchdektoptocompose.ui.pairing.SmallPairingLayout
import switchdektoptocompose.ui.pairing.UnifiedPairingLayout

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
        windowState.size = if (fleetMode) {
            androidx.compose.ui.unit.DpSize(1600.dp, 1200.dp)
        } else {
            androidx.compose.ui.unit.DpSize(1000.dp, 800.dp)
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
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isSmallScreen = maxWidth < 1000.dp || maxHeight < 700.dp
            
            if (isSmallScreen || !fleetMode) {
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
                    maxWidth = maxWidth,
                    maxHeight = maxHeight
                )
            }
        }
    }
}
