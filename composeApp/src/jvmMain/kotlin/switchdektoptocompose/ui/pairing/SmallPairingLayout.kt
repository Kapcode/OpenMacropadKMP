package switchdektoptocompose.ui.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import switchdektoptocompose.model.ClientInfo
import switchdektoptocompose.viewmodel.PairingViewModel

@Composable
fun SmallPairingLayout(
    requests: List<ClientInfo>,
    pairingViewModel: PairingViewModel,
    onApprove: (String, String, Boolean) -> Unit,
    onDeny: (String) -> Unit,
    onBan: (String, String) -> Unit,
    isAlwaysAllowAvailable: Boolean,
    onClose: () -> Unit
) {
    val fleetMode by pairingViewModel.fleetModeEnabled.collectAsState()
    val qrBitmaps by pairingViewModel.qrBitmaps.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SYNC",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pairing & Sync (Fleet) Mode", style = MaterialTheme.typography.labelLarge)
                Switch(
                    checked = fleetMode,
                    onCheckedChange = { pairingViewModel.setFleetModeEnabled(it) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                TextButton(onClick = onClose) {
                    Text("DISMISS ALL")
                }
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(requests, key = { it.id }) { request ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Small QR for the specific device
                        QrImage(qrBitmaps[request.id])
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(request.name.uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Code: ${request.verificationCode}", style = MaterialTheme.typography.bodyLarge, letterSpacing = 1.sp)
                            
                            Spacer(Modifier.height(8.dp))
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onApprove(request.id, request.name, false) },
                                    enabled = request.codeMatched,
                                    modifier = Modifier.weight(1f)
                                ) { Text("ONCE") }
                                
                                if (isAlwaysAllowAvailable) {
                                    Button(
                                        onClick = { onApprove(request.id, request.name, true) },
                                        enabled = request.codeMatched,
                                        modifier = Modifier.weight(1f)
                                    ) { Text("ALWAYS") }
                                }
                            }
                            
                            Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onDeny(request.id) }, modifier = Modifier.weight(1f)) { Text("DENY") }
                                IconButton(onClick = { onBan(request.id, request.name) }) {
                                    Icon(Icons.Default.Warning, "Ban", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
