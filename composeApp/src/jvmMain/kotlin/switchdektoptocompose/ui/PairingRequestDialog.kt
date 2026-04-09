package switchdektoptocompose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import switchdektoptocompose.model.ClientInfo
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme
import switchdektoptocompose.viewmodel.ConsoleViewModel

@Composable
fun PairingRequestDialog(
    request: ClientInfo,
    selectedTheme: String,
    consoleViewModel: ConsoleViewModel,
    isAlwaysAllowAvailable: Boolean = true,
    onApprove: (Boolean) -> Unit,
    onDeny: () -> Unit,
    onBan: () -> Unit
) {
    AppDialog(
        onCloseRequest = onDeny,
        state = rememberWindowState(width = 550.dp, height = 380.dp), // Increased height from 300 to 380
        title = "Pairing Request",
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxSize(), // Added fillMaxSize to ensure space-between works
            verticalArrangement = Arrangement.SpaceBetween
        ) {
                    Column {
                        Text(
                            "Allow '${request.name}' to control this PC?",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Device ID: ${request.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        request.verificationCode?.let { code ->
                            Spacer(Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Verification Code",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        code,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        letterSpacing = 4.sp
                                    )
                                }
                            }
                            Text(
                                "Verify this code matches on your mobile device.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onBan,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Ban")
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onDeny) {
                            Text("Deny")
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { onApprove(false) }) {
                            Text("Allow Once")
                        }
                        if (isAlwaysAllowAvailable) {
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { onApprove(true) }) {
                                Text("Always Allow")
                            }
                        } else {
                            // Optionally add a note why Always Allow is missing
                        }
                    }
        }
    }
}
