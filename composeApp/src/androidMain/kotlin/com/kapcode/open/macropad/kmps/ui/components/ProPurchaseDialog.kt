package com.kapcode.open.macropad.kmps.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.AdVisibilityManager
import com.kapcode.open.macropad.kmps.AdmobBanner
import com.kapcode.open.macropad.kmps.BillingConstants
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kapcode.`open`.macropad.kmps.Res
import com.kapcode.`open`.macropad.kmps.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProPurchaseDialog(
    onDismissRequest: () -> Unit,
    onWatchAd: () -> Unit,
    onPurchaseSubscription: () -> Unit,
    onPurchaseOneTime: () -> Unit,
    onRemoveAdsSubscription: () -> Unit,
    onRemoveAdsOneTime: () -> Unit,
    formattedPrices: Map<String, String> = emptyMap(),
    isPro: Boolean = false,
    isAdFree: Boolean = false,
    isDeveloperMode: Boolean = false,
    tokensPerAd: Int = BillingConstants.TOKENS_PER_REWARDED_AD,
) {
    LaunchedEffect(Unit) {
        AdVisibilityManager.isForegroundAdVisible = true
    }
    
    DisposableEffect(Unit) {
        onDispose {
            AdVisibilityManager.isForegroundAdVisible = false
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // --- Fixed Sticky Header ---
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isPro) stringResource(Res.string.pro_access_active) else stringResource(Res.string.support_macrokap),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        // --- Moved Banner Ad to Sticky Header ---
                        val adsDisabled = isPro || isAdFree || isDeveloperMode
                        if (!adsDisabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            AdmobBanner(modifier = Modifier.fillMaxWidth().height(50.dp))
                        }
                    }
                }

                // --- Scrollable Body ---
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.unlock_premium),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )

                    Text(
                        text = stringResource(Res.string.one_time_purchases),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    // 1. Ad Free One-Time
                    PurchaseOptionCard(
                        title = stringResource(Res.string.remove_ads),
                        description = stringResource(Res.string.remove_ads_desc),
                        price = formattedPrices[BillingConstants.PRODUCT_ID_AD_FREE_ONE_TIME] ?: "---",
                        benefits = listOf(
                            "Removes banner ads",
                            "Removes interruptible ads",
                            "Keeps rewarded ads for tokens",
                            "One-time payment"
                        ),
                        icon = Icons.Default.DoneAll,
                        onClick = onRemoveAdsOneTime,
                        badge = if (isAdFree) "ACTIVE" else if (isDeveloperMode) "DEV" else "$",
                        highlight = !isAdFree
                    )

                    // 2. Pro One-Time
                    PurchaseOptionCard(
                        title = stringResource(Res.string.pro_lifetime),
                        description = stringResource(Res.string.pro_lifetime_desc),
                        price = formattedPrices[BillingConstants.PRODUCT_ID_PRO_ONE_TIME] ?: "---",
                        benefits = listOf(
                            "Unlimited Pro access forever",
                            "Global server access (No tokens)",
                            "Pro shared with all clients",
                            "Exclusive future features"
                        ),
                        icon = Icons.Default.CardMembership,
                        onClick = onPurchaseOneTime,
                        badge = if (isPro) "ACTIVE" else if (isDeveloperMode) "DEV" else "$$$$$",
                        highlight = !isPro
                    )

                    Text(
                        text = stringResource(Res.string.subscriptions),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    // 3. Ad Free Subscription
                    PurchaseOptionCard(
                        title = stringResource(Res.string.ad_free_monthly),
                        description = stringResource(Res.string.ad_free_monthly_desc),
                        price = formattedPrices[BillingConstants.PRODUCT_ID_AD_FREE_SUB] ?: "---",
                        benefits = listOf(
                            "Removes banner ads",
                            "Removes interruptible ads",
                            "Keeps rewarded ads for tokens",
                            "Cancel anytime"
                        ),
                        icon = Icons.Default.Block,
                        onClick = onRemoveAdsSubscription,
                        badge = if (isAdFree) "ACTIVE" else "$$",
                        highlight = !isAdFree
                    )

                    // 4. Pro Subscription
                    PurchaseOptionCard(
                        title = stringResource(Res.string.pro_monthly),
                        description = stringResource(Res.string.pro_monthly_desc),
                        price = formattedPrices[BillingConstants.PRODUCT_ID_PRO_SUB] ?: "---",
                        benefits = listOf(
                            "All Pro features included",
                            "Unlimited global access",
                            "Auto-renewing convenience",
                            "Cancel anytime"
                        ),
                        icon = Icons.Default.CalendarMonth,
                        onClick = onPurchaseSubscription,
                        badge = if (isPro) "ACTIVE" else "$$$$",
                        highlight = !isPro
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // --- Sticky Footer ---
                Surface(
                    tonalElevation = 2.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Watch Ad (Sticky Footer)
                        PurchaseOptionCard(
                            title = stringResource(Res.string.watch_ad),
                            description = stringResource(Res.string.watch_ad_desc),
                            price = "+$tokensPerAd Tokens",
                            benefits = listOf("Earn tokens for free", "Supports development"),
                            icon = Icons.Default.PlayCircle,
                            onClick = onWatchAd,
                            badge = "FREE",
                            compact = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismissRequest) {
                                Text(stringResource(Res.string.close))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseOptionCard(
    title: String,
    description: String,
    price: String,
    benefits: List<String>,
    icon: ImageVector,
    onClick: () -> Unit,
    badge: String? = null,
    highlight: Boolean = false,
    compact: Boolean = false,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        val padding = if (compact) 12.dp else 16.dp
        val iconSize = if (compact) 24.dp else 32.dp
        val textPadding = if (compact) 36.dp else 48.dp

        Column(modifier = Modifier.padding(padding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = price,
                                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        if (badge != null) {
                            Surface(
                                color = if (badge == "ACTIVE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                shape = MaterialTheme.shapes.extraSmall,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (badge == "ACTIVE") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            
            if (!compact) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = textPadding, end = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.padding(start = textPadding)) {
                    benefits.forEach { benefit ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 1.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = benefit,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ProPurchaseDialogPreview() {
    AppTheme {
        ProPurchaseDialog(
            onDismissRequest = {},
            onWatchAd = {},
            onPurchaseSubscription = {},
            onPurchaseOneTime = {},
            onRemoveAdsSubscription = {},
            onRemoveAdsOneTime = {},
            tokensPerAd = 25,
            formattedPrices = mapOf(
                BillingConstants.PRODUCT_ID_PRO_ONE_TIME to "$19.99",
                BillingConstants.PRODUCT_ID_PRO_SUB to "$2.99/mo",
                BillingConstants.PRODUCT_ID_AD_FREE_ONE_TIME to "$4.99",
                BillingConstants.PRODUCT_ID_AD_FREE_SUB to "$0.99/mo"
            )
        )
    }
}
