package switchdektoptocompose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import switchdektoptocompose.model.ClientInfo
import com.kapcode.open.macropad.kmps.ui.theme.AppTheme
import switchdektoptocompose.viewmodel.ConsoleViewModel
import switchdektoptocompose.utils.QrCodeGenerator
import androidx.compose.foundation.Image

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
        state = rememberWindowState(width = 600.dp, height = 500.dp),
        title = "Pairing Request",
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.weight(1f)) {
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
                            )
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
                                    if (code.length == 6) "${code.take(3)}-${code.drop(3)}" else code,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    letterSpacing = 2.sp
                                )

                                if (request.codeMatched) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF008000),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "Code Matched",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF008000)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                request.verificationCode?.let { code ->
                    Spacer(Modifier.width(24.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = Color.White,
                            modifier = Modifier.size(200.dp)
                        ) {
                            val qrBitmap = remember(code) { QrCodeGenerator.generateQrCode(code, 200) }
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = "QR Code",
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Text(
                            "Scan to connect",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Text(
                "Verify this code matches on your mobile device or scan the QR code.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                        OutlinedButton(
                            onClick = { onApprove(false) },
                            enabled = request.codeMatched
                        ) {
                            Text("Allow Once")
                        }
                        if (isAlwaysAllowAvailable) {
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { onApprove(true) },
                                enabled = request.codeMatched
                            ) {
                                Text("Always Allow")
                            }
                        } else {
                            // Optionally add a note why Always Allow is missing
                        }
                    }
        }
    }
}
