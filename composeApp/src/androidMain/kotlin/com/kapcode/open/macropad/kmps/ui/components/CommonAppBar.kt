package com.kapcode.open.macropad.kmps.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.BillingConstants
import com.kapcode.open.macropad.kmps.TokenManager
import com.kapcode.open.macropad.kmps.loadRewardedAd
import com.kapcode.open.macropad.kmps.showRewardedAd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonAppBar(
    title: String,
    onSettingsClick: () -> Unit,
    navigationIcon: @Composable () -> Unit,
    isQrScannerActive: Boolean = false,
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onCloseScanner: () -> Unit = {},
    isAutoZoomEnabled: Boolean = false,
    onAutoZoomToggle: (Boolean) -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as Activity
    val tokenManager = remember { TokenManager.getInstance(context) }
    val tokenBalance by tokenManager.tokenBalance.collectAsState()
    var showGetTokensDialog by remember { mutableStateOf(false) }

    if (showGetTokensDialog) {
        GetTokensDialog(
            onDismissRequest = { showGetTokensDialog = false },
            onConfirm = {
                loadRewardedAd(
                    context,
                    onAdLoaded = { ad ->
                        showRewardedAd(activity, ad) {
                            tokenManager.awardTokens(BillingConstants.TOKENS_PER_REWARDED_AD)
                        }
                    },
                    onAdFailedToLoad = {
                        Toast.makeText(context, "Ad failed to load. Please try again later.", Toast.LENGTH_SHORT).show()
                    }
                )
                showGetTokensDialog = false
            }
        )
    }

    if (isQrScannerActive) {
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text("QR Scanner", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            },
            navigationIcon = {
                IconButton(onClick = onCloseScanner) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Scanner")
                }
            },
            actions = {
                IconButton(onClick = { onAutoZoomToggle(!isAutoZoomEnabled) }) {
                    Icon(
                        imageVector = if (isAutoZoomEnabled) Icons.Default.Timer else Icons.Default.TimerOff,
                        contentDescription = "Toggle Auto Zoom",
                        tint = if (isAutoZoomEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val zoomOutInteractionSource = remember { MutableInteractionSource() }
                val isZoomOutPressed by zoomOutInteractionSource.collectIsPressedAsState()
                LaunchedEffect(isZoomOutPressed) {
                    if (isZoomOutPressed) {
                        onZoomOut()
                        delay(400)
                        while (true) {
                            onZoomOut()
                            delay(100)
                        }
                    }
                }
                IconButton(onClick = {}, interactionSource = zoomOutInteractionSource) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out")
                }

                val zoomInInteractionSource = remember { MutableInteractionSource() }
                val isZoomInPressed by zoomInInteractionSource.collectIsPressedAsState()
                LaunchedEffect(isZoomInPressed) {
                    if (isZoomInPressed) {
                        onZoomIn()
                        delay(400)
                        while (true) {
                            onZoomIn()
                            delay(100)
                        }
                    }
                }
                IconButton(onClick = {}, interactionSource = zoomInInteractionSource) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In")
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Optionally show tokens even in QR mode if there's space
                Row(
                    modifier = Modifier.clickable { showGetTokensDialog = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = "Tokens")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = tokenBalance.toString())
                }
            }
        )
    } else {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = navigationIcon,
            actions = {
                actions()
                Row(
                    modifier = Modifier.clickable { showGetTokensDialog = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Tokens"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = tokenBalance.toString())
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
        )
    }
}
