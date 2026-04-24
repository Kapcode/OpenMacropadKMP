package com.kapcode.open.macropad.kmps.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.kapcode.open.macropad.kmps.ClientViewModel

@Composable
fun ClientSettingsSection(clientViewModel: ClientViewModel) {
    Column {
        Text("Developer Settings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        
        Button(
            onClick = { clientViewModel.sendTestUpgrade() },
            modifier = Modifier.padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("TEST SERVER UPGRADE (SIMULATION)")
        }

        Text(
            "Sends a dummy upgrade payload to the server to test the handshake, hash verification, and consent UI without restarting the server.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
