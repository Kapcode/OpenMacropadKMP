package com.kapcode.`open`.macropad.kmps.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.kapcode.`open`.macropad.kmps.*
import com.kapcode.open.macropad.kmps.desktop.viewmodel.ConsoleViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoggingToFileWarningDialog(
    selectedTheme: String,
    consoleViewModel: ConsoleViewModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AppDialog(
        onCloseRequest = onDismiss,
        state = rememberWindowState(width = 500.dp, height = 400.dp),
        title = stringResource(Res.string.logging_warning_title),
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
                    Column {
                        Text(
                            text = stringResource(Res.string.logging_warning_desc),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(Res.string.implications), style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(Res.string.logging_warning_security))
                        Text(stringResource(Res.string.logging_warning_hardware))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(Res.string.logging_warning_temporary),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(Res.string.cancel))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text(stringResource(Res.string.understand_enable_temporarily))
                        }
                    }
        }
    }
}
