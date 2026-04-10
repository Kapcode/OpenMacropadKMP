package com.kapcode.open.macropad.kmps.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun GeneralSettingsSection(viewModel: SettingsViewModel) {
    val currentTheme by viewModel.theme.collectAsState()
    val analyticsEnabled by viewModel.analyticsEnabled.collectAsState()

    Column {
        Text("Theme", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Column(Modifier.selectableGroup()) {
            AppTheme.entries.forEach { theme ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (theme == currentTheme),
                            onClick = { viewModel.setTheme(theme) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
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
                .height(56.dp)
                .padding(horizontal = 16.dp),
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
                .height(56.dp)
                .padding(horizontal = 16.dp),
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
        
        Text("Slam Fire", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
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
    }
}
