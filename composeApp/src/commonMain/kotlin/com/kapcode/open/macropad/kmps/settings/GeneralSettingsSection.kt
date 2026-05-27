package com.kapcode.open.macropad.kmps.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import com.kapcode.open.macropad.kmps.isDesktop
import com.kapcode.open.macropad.kmps.utils.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GeneralSettingsSection(viewModel: SettingsViewModel) {
    val currentTheme by viewModel.theme.collectAsState()
    val analyticsEnabled by viewModel.analyticsEnabled.collectAsState()
    val enableToasts by viewModel.enableToasts.collectAsState()
    val enableBackgroundToasts by viewModel.enableBackgroundToasts.collectAsState()
    val macroDirectory by viewModel.macroDirectory.collectAsState()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    Column {
        if (isDesktop) {
            Text("Directory Settings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            Column(Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = macroDirectory.ifEmpty { "No directory selected" },
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    label = { Text("Macro Storage Path") },
                    trailingIcon = {
                        Row {
                            IconButton(onClick = {
                                if (macroDirectory.isNotEmpty()) {
                                    scope.launch {
                                        clipboard.copyToClipboard(macroDirectory)
                                    }
                                }
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                            IconButton(onClick = { viewModel.onOpenFolder() }) {
                                Icon(Icons.Default.FolderOpen, contentDescription = "Open Folder")
                            }
                        }
                    },
                    isError = macroDirectory.isEmpty()
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }

        Text("Theme", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Column(Modifier.selectableGroup()) {
            AppTheme.entries.forEach { theme ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (theme == currentTheme),
                            onClick = { viewModel.setTheme(theme) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (theme == currentTheme),
                        onClick = null
                    )
                    Text(
                        text = theme.name.replace("Blue", " Blue"),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Privacy", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Send Analytics (Opt in)", style = MaterialTheme.typography.bodyLarge)
                Text("Allow the app to collect anonymous performance telemetry and startup metrics.", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = analyticsEnabled,
                onCheckedChange = { viewModel.setAnalyticsEnabled(it) }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        val multiQrEnabled by viewModel.multiQrEnabled.collectAsState()
        Text("Advanced Pairing", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Multi-QR Mode", style = MaterialTheme.typography.bodyLarge)
                Text("Displays multiple QR codes for easier scanning on desk-mounted devices.", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = multiQrEnabled,
                onCheckedChange = { viewModel.setMultiQrEnabled(it) }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        val slamFireEnabled by viewModel.slamFireEnabled.collectAsState()
        val slamFireTrigger by viewModel.slamFireTrigger.collectAsState()
        val scannerTimeoutHours by viewModel.scannerTimeoutHours.collectAsState()
        
        Text("Scanner Settings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                "Scanner Timeout: ${if (scannerTimeoutHours >= 48) "Never" else "$scannerTimeoutHours Hours"}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "Stops scanning after a period of time to save battery.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Slider(
                value = scannerTimeoutHours.toFloat(),
                onValueChange = { viewModel.setScannerTimeoutHours(it.toInt()) },
                valueRange = 1f..48f,
                steps = 46 // 48 - 1 - 1 = 46 steps for integer values from 1 to 48
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Slam Fire", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Slam Fire Mode", style = MaterialTheme.typography.bodyLarge)
                Text("Trigger an 'Okay' action via hardware interaction.", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = slamFireEnabled,
                onCheckedChange = { viewModel.setSlamFireEnabled(it) }
            )
        }

        if (slamFireEnabled) {
            Column(Modifier.selectableGroup().padding(start = 16.dp)) {
                SlamFireTrigger.entries.forEach { trigger ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = (trigger == slamFireTrigger),
                                onClick = { viewModel.setSlamFireTrigger(trigger) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (trigger == slamFireTrigger),
                            onClick = null
                        )
                        Text(
                            text = when(trigger) {
                                SlamFireTrigger.VolumeDown -> "Volume Down"
                                SlamFireTrigger.VolumeUp -> "Volume Up"
                                SlamFireTrigger.Power -> "Power Button (Limited Support)"
                                SlamFireTrigger.Bixby -> "Bixby / Side Button"
                                SlamFireTrigger.Assistant -> "Assistant Button"
                                SlamFireTrigger.ProximityCovered -> "Proximity Sensor (Covered)"
                                SlamFireTrigger.ProximityUncovered -> "Proximity Sensor (Uncovered)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Notifications (Toasts)", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Notifications", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = enableToasts,
                onCheckedChange = { viewModel.setEnableToasts(it) }
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Notifications in Background", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = enableBackgroundToasts,
                onCheckedChange = { viewModel.setEnableBackgroundToasts(it) }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
    }
}
