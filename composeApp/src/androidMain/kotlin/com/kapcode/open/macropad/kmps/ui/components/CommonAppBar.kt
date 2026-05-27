package com.kapcode.open.macropad.kmps.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.BillingConstants
import com.kapcode.open.macropad.kmps.BillingManager
import com.kapcode.open.macropad.kmps.TokenManager
import com.kapcode.open.macropad.kmps.loadRewardedAd
import com.kapcode.open.macropad.kmps.showRewardedAd

val GoldCurrencyColor = Color(0xFFFFD700)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonAppBar(
    title: String,
    onSettingsClick: () -> Unit,
    navigationIcon: @Composable () -> Unit,
    currency: Long = 0,
    isQrScannerActive: Boolean = false,
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onCloseScanner: () -> Unit = {},
    isAutoZoomEnabled: Boolean = false,
    onAutoZoomToggle: (Boolean) -> Unit = {},
    isAutoFocusEnabled: Boolean = false,
    onAutoFocusToggle: (Boolean) -> Unit = {},
    isCoordinateCaptureActive: Boolean = false,
    onCoordinateCaptureToggle: (Boolean) -> Unit = {},
    isPro: Boolean = false,
    isServerPro: Boolean = false,
    serverProTimeRemaining: Long = 0,
    isAdFree: Boolean = false,
    isDeveloperMode: Boolean = false,
    billingManager: BillingManager? = null,
    onProPurchaseClick: (() -> Unit)? = null,
    onAdFreePurchaseClick: (() -> Unit)? = null,
    tokensPerAd: Int = BillingConstants.TOKENS_PER_REWARDED_AD,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as Activity
    val tokenManager = remember { TokenManager.getInstance(context) }
    val tokenBalance by tokenManager.tokenBalance.collectAsState()
    val formattedPrices by (billingManager?.formattedPrices?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
    var showProPurchaseDialog by remember { mutableStateOf(false) }

    if (showProPurchaseDialog) {
        ProPurchaseDialog(
            onDismissRequest = { showProPurchaseDialog = false },
            isPro = isPro || isServerPro,
            isAdFree = isAdFree,
            isDeveloperMode = isDeveloperMode,
            formattedPrices = formattedPrices,
            tokensPerAd = tokensPerAd,
            onWatchAd = {
                loadRewardedAd(
                    context,
                    onAdLoaded = { ad ->
                        showRewardedAd(activity, ad) {
                            tokenManager.awardTokens(tokensPerAd)
                        }
                    }
                ) {
                    Toast.makeText(context, "Ad failed to load. Please try again later.", Toast.LENGTH_SHORT).show()
                }
                showProPurchaseDialog = false
            },
            onPurchaseSubscription = {
                if (isDeveloperMode) {
                    onProPurchaseClick?.invoke()
                    showProPurchaseDialog = false
                } else {
                    billingManager?.launchBillingFlow(activity, BillingConstants.PRODUCT_ID_PRO_SUB)
                }
            },
            onPurchaseOneTime = {
                if (isDeveloperMode) {
                    onProPurchaseClick?.invoke()
                    showProPurchaseDialog = false
                } else {
                    billingManager?.launchBillingFlow(activity, BillingConstants.PRODUCT_ID_PRO_ONE_TIME)
                }
            },
            onRemoveAdsSubscription = {
                if (isDeveloperMode) {
                    onAdFreePurchaseClick?.invoke()
                    showProPurchaseDialog = false
                } else {
                    billingManager?.launchBillingFlow(activity, BillingConstants.PRODUCT_ID_AD_FREE_SUB)
                }
            },
            onRemoveAdsOneTime = {
                if (isDeveloperMode) {
                    onAdFreePurchaseClick?.invoke()
                    showProPurchaseDialog = false
                } else {
                    billingManager?.launchBillingFlow(activity, BillingConstants.PRODUCT_ID_AD_FREE_ONE_TIME)
                }
            }
        )
    }

    val titleContent = @Composable {
        if (title == "Settings") {
            Text(title, style = MaterialTheme.typography.titleMedium)
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showProPurchaseDialog = true }
            ) {
                if (isPro) {
                    Icon(Icons.Default.Star, null, tint = GoldCurrencyColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Pro Active", style = MaterialTheme.typography.titleMedium, color = GoldCurrencyColor, fontWeight = FontWeight.Bold)
                } else if (isServerPro) {
                    val hours = serverProTimeRemaining / (1000 * 60 * 60)
                    val minutes = (serverProTimeRemaining / (1000 * 60)) % 60
                    Icon(Icons.Default.Dns, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Server Pro (${hours}h ${minutes}m)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                } else {
                    OutlinedCard(
                        onClick = { showProPurchaseDialog = true },
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.outlinedCardColors(contentColor = MaterialTheme.colorScheme.primary),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            "BUY PRO",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }

    if (isQrScannerActive) {
        CenterAlignedTopAppBar(
            title = titleContent,
            navigationIcon = {
                IconButton(onClick = onCloseScanner) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Scanner")
                }
            },
            actions = {
                // Auto Focus Toggle (New)
                IconButton(onClick = { onAutoFocusToggle(!isAutoFocusEnabled) }) {
                    Icon(
                        imageVector = if (isAutoFocusEnabled) Icons.Default.AutoMode else Icons.Default.TimerOff,
                        contentDescription = "Toggle Auto Focus",
                        tint = if (isAutoFocusEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Auto Zoom Toggle
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
                    modifier = Modifier.clickable { showProPurchaseDialog = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CurrencyExchange,
                        contentDescription = "Tokens",
                        tint = GoldCurrencyColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tokenBalance.toString(),
                        color = GoldCurrencyColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    } else {
        TopAppBar(
            title = titleContent,
            navigationIcon = navigationIcon,
            actions = {
                if (currency > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = "Currency",
                            tint = GoldCurrencyColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currency.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = GoldCurrencyColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row(
                    modifier = Modifier.clickable { showProPurchaseDialog = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CurrencyExchange,
                        contentDescription = "Tokens",
                        tint = GoldCurrencyColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tokenBalance.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = GoldCurrencyColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { onCoordinateCaptureToggle(!isCoordinateCaptureActive) }) {
                    Icon(
                        imageVector = if (isCoordinateCaptureActive) Icons.Default.Adjust else Icons.Default.Add,
                        contentDescription = "Capture Coordinates",
                        tint = if (isCoordinateCaptureActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
