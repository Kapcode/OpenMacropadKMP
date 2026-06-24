package com.kapcode.open.macropad.kmps.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import org.jetbrains.compose.resources.painterResource
import com.kapcode.`open`.macropad.kmps.Res
import com.kapcode.`open`.macropad.kmps.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun RewardConfirmDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    kapsPerAd: Int
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
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.macropadIcon64),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(Res.string.earn_kaps),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(Res.string.earn_kaps_desc, kapsPerAd),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.kaps_usage_desc),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismissRequest) {
                            Text(stringResource(Res.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(stringResource(Res.string.watch_video_with_reward, kapsPerAd))
                        }
                    }
                }

                // Banner at the bottom of the dialog (Sticky Footer)
                AdmobBanner(
                    location = AdLocation.REWARD_CONFIRM,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                )
            }
        }
    }
}
