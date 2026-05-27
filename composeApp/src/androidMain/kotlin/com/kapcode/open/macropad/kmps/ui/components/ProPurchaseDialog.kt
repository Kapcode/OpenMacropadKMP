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

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isPro) "Pro Access Active" else "Support OpenMacropad",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Unlock premium features, remove ads, and support the development of OpenMacropad.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // 1. Watch Ad
                PurchaseOptionCard(
                    title = "Watch Ad",
                    description = "Watch a short video to earn tokens.",
                    price = "+$tokensPerAd Tokens",
                    benefits = listOf(
                        "Earn $tokensPerAd tokens for free",
                        "No permanent commitment",
                        "Supports development"
                    ),
                    icon = Icons.Default.PlayCircle,
                    onClick = onWatchAd,
                    badge = "WATCH AD"
                )

                Text(
                    text = "One-Time Purchases",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                // 2. Ad Free One-Time
                PurchaseOptionCard(
                    title = "Remove Ads",
                    description = "One-time payment to remove banner ads forever.",
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

                // 3. Pro One-Time
                PurchaseOptionCard(
                    title = "Pro Lifetime Pass",
                    description = "Permanent Pro access for this device and server.",
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
                    text = "Subscriptions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                // 4. Ad Free Subscription
                PurchaseOptionCard(
                    title = "Ad-Free Monthly",
                    description = "Remove ads with a small monthly contribution.",
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

                // 5. Pro Subscription
                PurchaseOptionCard(
                    title = "Pro Monthly",
                    description = "Unlimited Pro access with a monthly sub.",
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

                // Add a banner ad inside the dialog if ads are not disabled
                if (!isPro && !isAdFree && !isDeveloperMode) { // Logic simplified for the dialog itself as it's the gateway
                    AdmobBanner(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        }
    )
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
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
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
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = price,
                                style = MaterialTheme.typography.labelLarge,
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
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 48.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.padding(start = 48.dp)) {
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
