package com.kapcode.open.macropad.kmp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ConnectionItem(
    name: String,
    ipAddressPort: String,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit = {} // Added onClick parameter
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick(ipAddressPort) }, // Make the card clickable and pass the IP:Port
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ipAddressPort,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Placeholder for image buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Image buttons will go here later
            }
        }
    }
}

@Preview
@Composable
fun ConnectionItemPreview() {
    MaterialTheme {
        ConnectionItem(
            name = "My Macropad Server",
            ipAddressPort = "192.168.1.100:9999",
            onClick = { /* Preview click handler */ }
        )
    }
}