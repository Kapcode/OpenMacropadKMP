package com.kapcode.open.macropad.kmps.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.ui.components.LegalDialog
import org.jetbrains.compose.resources.stringResource
import com.kapcode.`open`.macropad.kmps.Res
import com.kapcode.`open`.macropad.kmps.*

@Composable
fun LegalSettingsSection() {
    var showLegal by remember { mutableStateOf(false) }

    if (showLegal) {
        LegalDialog(
            onAccept = { showLegal = false },
            onDeny = { showLegal = false }
        )
    }

    Column {
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        Text(
            text = stringResource(Res.string.legal),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedButton(
            onClick = { showLegal = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.view_tos) + " & " + stringResource(Res.string.view_privacy_policy))
        }
    }
}
