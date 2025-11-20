package com.kapcode.open.macropad.kmps.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.kapcode.open.macropad.kmps.BillingConstants

@Composable
fun GetTokensDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Get More Tokens") },
        text = {
            Text("Watch a short ad to earn ${BillingConstants.TOKENS_PER_REWARDED_AD} tokens. It costs ${BillingConstants.TOKENS_PER_MACRO_PRESS} token per macro press.")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Watch Ad")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}