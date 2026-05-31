package com.kapcode.open.macropad.kmps.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kapcode.`open`.macropad.kmps.Res
import com.kapcode.`open`.macropad.kmps.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun LegalDialog(
    onAccept: () -> Unit,
    onDeny: () -> Unit
) {
    val scrollState = rememberLazyListState()
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = scrollState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0) {
                false
            } else {
                val lastVisibleItem = visibleItemsInfo.lastOrNull()
                lastVisibleItem != null && lastVisibleItem.index == layoutInfo.totalItemsCount - 1 &&
                        (lastVisibleItem.offset + lastVisibleItem.size) <= layoutInfo.viewportEndOffset
            }
        }
    }

    Dialog(
        onDismissRequest = { /* Cannot dismiss */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(Res.string.tos_title) + " & " + stringResource(Res.string.privacy_policy_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(Res.string.legal_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = stringResource(Res.string.tos_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(Res.string.tos_content),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        item {
                            HorizontalDivider()
                        }
                        item {
                            Text(
                                text = stringResource(Res.string.privacy_policy_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(Res.string.privacy_policy_content),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        item {
                            // Dummy item to ensure we can reach the absolute end
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (!isAtBottom) {
                    Text(
                        text = stringResource(Res.string.must_read_to_bottom),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDeny,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.deny))
                    }
                    
                    Button(
                        onClick = onAccept,
                        enabled = isAtBottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.accept))
                    }
                }
            }
        }
    }
}
