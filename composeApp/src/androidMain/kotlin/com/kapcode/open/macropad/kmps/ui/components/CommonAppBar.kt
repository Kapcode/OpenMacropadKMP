package com.kapcode.`open`.macropad.kmps.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kapcode.`open`.macropad.kmps.Res
import com.kapcode.`open`.macropad.kmps.*
import com.kapcode.open.macropad.kmps.ui.theme.DarkGoldCurrencyColor
import com.kapcode.open.macropad.kmps.ui.theme.GoldCurrencyColor
import org.jetbrains.compose.resources.stringResource

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
    kapsPerAd: Int = BillingConstants.KAPS_PER_REWARDED_AD,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as Activity
    val kapManager = remember { KapManager.getInstance(context) }
    val formattedPrices by (billingManager?.formattedPrices?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })
    var showProPurchaseDialog by remember { mutableStateOf(false) }
    var showRewardConfirmDialog by remember { mutableStateOf(false) }

    val adFailedLoadMessage = stringResource(Res.string.ad_failed_load)

    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val currencyColor = if (isLight) DarkGoldCurrencyColor else GoldCurrencyColor
    val textShadow = if (isLight) Shadow(
        color = Color.Black.copy(alpha = 0.3f),
        offset = Offset(1f, 1f),
        blurRadius = 2f
    ) else null

    val currencyTextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = currencyColor,
        fontWeight = FontWeight.Bold,
        shadow = textShadow
    )

    val titleCurrencyTextStyle = MaterialTheme.typography.titleMedium.copy(
        color = currencyColor,
        fontWeight = FontWeight.Bold,
        shadow = textShadow
    )

    val animationManager = LocalKapAnimationManager.current

    if (showRewardConfirmDialog) {
        RewardConfirmDialog(
            onDismissRequest = { showRewardConfirmDialog = false },
            kapsPerAd = kapsPerAd,
            onConfirm = {
                showRewardConfirmDialog = false
                loadRewardedAd(
                    context,
                    onAdLoaded = { ad ->
                        showRewardedAd(activity, ad) {
                            kapManager.awardKaps(kapsPerAd)
                            animationManager.triggerAward()
                        }
                    }
                ) {
                    Toast.makeText(context, adFailedLoadMessage, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showProPurchaseDialog) {
        ProPurchaseDialog(
            onDismissRequest = { showProPurchaseDialog = false },
            isPro = isPro || isServerPro,
            isAdFree = isAdFree,
            isDeveloperMode = isDeveloperMode,
            formattedPrices = formattedPrices,
            kapsPerAd = kapsPerAd,
            onWatchAd = {
                loadRewardedAd(
                    context,
                    onAdLoaded = { ad ->
                        showRewardedAd(activity, ad) {
                            kapManager.awardKaps(kapsPerAd)
                            animationManager.triggerAward()
                        }
                    }
                ) {
                    Toast.makeText(context, adFailedLoadMessage, Toast.LENGTH_SHORT).show()
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
            Text(stringResource(Res.string.settings), style = MaterialTheme.typography.titleMedium)
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showProPurchaseDialog = true }
            ) {
                if (isPro) {
                    Icon(Icons.Default.Star, null, tint = currencyColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(Res.string.pro_active),
                        style = titleCurrencyTextStyle
                    )
                } else if (isServerPro) {
                    val hours = serverProTimeRemaining / (1000 * 60 * 60)
                    val minutes = (serverProTimeRemaining / (1000 * 60)) % 60
                    Icon(Icons.Default.Dns, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(Res.string.server_pro, hours, minutes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    OutlinedCard(
                        onClick = { showProPurchaseDialog = true },
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.outlinedCardColors(contentColor = MaterialTheme.colorScheme.primary),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = stringResource(Res.string.buy_pro),
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
                    Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(Res.string.close_scanner))
                }
            },
            actions = {
                actions()
                // Auto Focus Toggle (New)
                IconButton(onClick = { onAutoFocusToggle(!isAutoFocusEnabled) }) {
                    Icon(
                        imageVector = if (isAutoFocusEnabled) Icons.Default.AutoMode else Icons.Default.TimerOff,
                        contentDescription = stringResource(Res.string.toggle_auto_focus),
                        tint = if (isAutoFocusEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Auto Zoom Toggle
                IconButton(onClick = { onAutoZoomToggle(!isAutoZoomEnabled) }) {
                    Icon(
                        imageVector = if (isAutoZoomEnabled) Icons.Default.Timer else Icons.Default.TimerOff,
                        contentDescription = stringResource(Res.string.toggle_auto_zoom),
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
                    Icon(imageVector = Icons.Default.Remove, contentDescription = stringResource(Res.string.zoom_out))
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
                    Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(Res.string.zoom_in))
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Optionally show Kaps even in QR mode if there's space
                KapBalanceWithTimer(
                    kapManager = kapManager,
                    currencyTextStyle = currencyTextStyle,
                    currencyColor = currencyColor,
                    onRewardClick = { showRewardConfirmDialog = true }
                )
            }
        )
    } else {
        TopAppBar(
            title = titleContent,
            navigationIcon = navigationIcon,
            actions = {
                actions()
                if (currency > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.macropadIcon64),
                            contentDescription = stringResource(Res.string.currency),
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currency.toString(),
                            style = currencyTextStyle
                        )
                    }
                }
                KapBalanceWithTimer(
                    kapManager = kapManager,
                    currencyTextStyle = currencyTextStyle,
                    currencyColor = currencyColor,
                    onRewardClick = { showRewardConfirmDialog = true }
                )
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(Res.string.settings)
                    )
                }
            }
        )
    }
}

@Composable
fun KapBalanceWithTimer(
    kapManager: KapManager,
    currencyTextStyle: TextStyle,
    currencyColor: Color,
    onRewardClick: () -> Unit
) {
    val kapBalance by kapManager.kapBalance.collectAsState()
    val lastSpentTime by kapManager.lastSpentTime.collectAsState()
    var remainingGraceFraction by remember { mutableStateOf(0f) }

    LaunchedEffect(lastSpentTime) {
        if (lastSpentTime > 0) {
            while (true) {
                val now = System.currentTimeMillis()
                val elapsed = now - lastSpentTime
                val remaining = (BillingConstants.GRACE_PERIOD_MS - elapsed).coerceAtLeast(0L)
                remainingGraceFraction = remaining.toFloat() / BillingConstants.GRACE_PERIOD_MS
                if (remaining <= 0) break
                delay(50)
            }
        } else {
            remainingGraceFraction = 0f
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onRewardClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(Res.drawable.macropadIcon64),
                contentDescription = stringResource(Res.string.kaps),
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(20.dp)
                    .trackBalancePosition(LocalKapAnimationManager.current)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = kapBalance.toString(),
                style = currencyTextStyle
            )
        }
        if (remainingGraceFraction > 0) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(14.dp)
                    .padding(top = 2.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                LinearProgressIndicator(
                    progress = { remainingGraceFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Color(0xFF2196F3), // Grace blue
                    trackColor = Color(0xFF2196F3).copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
                
                // Kap Icon Thumb
                Icon(
                    painter = painterResource(Res.drawable.macropadIcon64),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(14.dp)
                        .offset(x = 64.dp * remainingGraceFraction - 7.dp)
                )
            }
        }
    }
}
