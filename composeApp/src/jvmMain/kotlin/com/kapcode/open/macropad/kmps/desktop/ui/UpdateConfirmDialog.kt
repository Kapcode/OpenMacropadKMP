package com.kapcode.`open`.macropad.kmps.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import com.kapcode.`open`.macropad.kmps.*
import com.kapcode.open.macropad.kmps.desktop.viewmodel.ConsoleViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun UpdateConfirmDialog(
    selectedTheme: String,
    consoleViewModel: ConsoleViewModel,
    clientName: String,
    isSimulation: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    AppDialog(
        onCloseRequest = onReject,
        title = if (isSimulation) stringResource(Res.string.test_update_received) else stringResource(Res.string.server_update_available),
        state = rememberWindowState(width = 500.dp, height = 400.dp),
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel,
        resizable = false
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        if (isSimulation) MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isSimulation) Icons.Default.SystemUpdate else Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isSimulation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            Text(
                if (isSimulation) stringResource(Res.string.simulation_mode) else stringResource(Res.string.attention_required),
                style = MaterialTheme.typography.labelLarge,
                color = if (isSimulation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )

            Text(
                stringResource(Res.string.remote_update_request),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                stringResource(Res.string.remote_update_desc, clientName),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (!isSimulation) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            stringResource(Res.string.skip_update_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.action_reject))
                }
                
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSimulation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (isSimulation) stringResource(Res.string.run_test_upgrade) else stringResource(Res.string.accept_restart))
                }
            }
        }
    }
}
