package com.kapcode.open.macropad.kmps.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
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

    TopAppBar(
        title = { Text(title) },
        navigationIcon = navigationIcon,
        actions = {
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