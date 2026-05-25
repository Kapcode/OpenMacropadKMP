package com.kapcode.open.macropad.kmps.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.BillingConstants

@Composable
fun ProPurchaseDialog(
    onDismissRequest: () -> Unit,
    onWatchAd: () -> Unit,
    onPurchaseSubscription: () -> Unit,
    onPurchaseOneTime: () -> Unit,
    isPro: Boolean = false
) {
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
                    text = if (isPro) "Pro Access Active" else "Unlock OpenMacropad Pro",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text(
                        text = "Get global server access for 12 hours. Pro status is shared with the server and all connected clients.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }

                item {
                    PurchaseOptionCard(
                        title = "Watch Ad",
                        description = "Earn ${BillingConstants.TOKENS_PER_REWARDED_AD} tokens for free.",
                        icon = Icons.Default.PlayCircle,
                        onClick = onWatchAd
                    )
                }

                item {
                    PurchaseOptionCard(
                        title = "Pro Subscription",
                        description = "Unlimited Pro access with a monthly or yearly sub.",
                        icon = Icons.Default.CalendarMonth,
                        onClick = onPurchaseSubscription,
                        badge = "Coming Soon"
                    )
                }

                item {
                    PurchaseOptionCard(
                        title = "Pro Pass (One-Time)",
                        description = "Permanent Pro access for this device and server.",
                        icon = Icons.Default.CardMembership,
                        onClick = onPurchaseOneTime,
                        highlight = true
                    )
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
    icon: ImageVector,
    onClick: () -> Unit,
    badge: String? = null,
    highlight: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
