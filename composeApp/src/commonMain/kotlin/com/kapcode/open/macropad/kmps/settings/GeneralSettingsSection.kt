package com.kapcode.`open`.macropad.kmps.settings

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
import com.kapcode.`open`.macropad.kmps.*
import com.kapcode.open.macropad.kmps.utils.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

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
            Text(stringResource(Res.string.directory_settings), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            Column(Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = macroDirectory.ifEmpty { stringResource(Res.string.no_directory_selected) },
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    label = { Text(stringResource(Res.string.macro_storage_path)) },
                    trailingIcon = {
                        Row {
                            IconButton(onClick = {
                                if (macroDirectory.isNotEmpty()) {
                                    scope.launch {
                                        clipboard.copyToClipboard(macroDirectory)
                                    }
                                }
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(Res.string.action_copy))
                            }
                            IconButton(onClick = { viewModel.onOpenFolder() }) {
                                Icon(Icons.Default.FolderOpen, contentDescription = stringResource(Res.string.open_folder))
                            }
                        }
                    },
                    isError = macroDirectory.isEmpty()
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }

        Text(stringResource(Res.string.theme), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
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

        Text(stringResource(Res.string.privacy), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.send_analytics), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(Res.string.send_analytics_desc), style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = analyticsEnabled,
                onCheckedChange = { viewModel.setAnalyticsEnabled(it) }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        val multiQrEnabled by viewModel.multiQrEnabled.collectAsState()
        Text(stringResource(Res.string.advanced_pairing), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.multi_qr_mode), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(Res.string.multi_qr_mode_desc), style = MaterialTheme.typography.bodySmall)
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
        
        Text(stringResource(Res.string.scanner_settings), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                stringResource(Res.string.scanner_timeout_format, if (scannerTimeoutHours >= 48) stringResource(Res.string.never) else "$scannerTimeoutHours " + stringResource(Res.string.hours)),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                stringResource(Res.string.scanner_timeout_desc),
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

        Text(stringResource(Res.string.slam_fire), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.slam_fire_mode), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(Res.string.slam_fire_desc), style = MaterialTheme.typography.bodySmall)
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
                                SlamFireTrigger.VolumeDown -> stringResource(Res.string.volume_down)
                                SlamFireTrigger.VolumeUp -> stringResource(Res.string.volume_up)
                                SlamFireTrigger.Power -> stringResource(Res.string.power_button_limited)
                                SlamFireTrigger.Bixby -> stringResource(Res.string.bixby_side_button)
                                SlamFireTrigger.Assistant -> stringResource(Res.string.assistant_button)
                                SlamFireTrigger.ProximityCovered -> stringResource(Res.string.proximity_covered)
                                SlamFireTrigger.ProximityUncovered -> stringResource(Res.string.proximity_uncovered)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(stringResource(Res.string.notifications_toasts), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(Res.string.enable_notifications), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
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
            Text(stringResource(Res.string.notifications_in_background), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = enableBackgroundToasts,
                onCheckedChange = { viewModel.setEnableBackgroundToasts(it) }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
    }
}
