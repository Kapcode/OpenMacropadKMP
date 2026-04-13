package switchdektoptocompose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import switchdektoptocompose.viewmodel.ConsoleViewModel

@Composable
fun RecordShortcutDialog(
    title: String,
    selectedTheme: String,
    consoleViewModel: ConsoleViewModel,
    isFullShortcut: Boolean = true,
    onShortcutRecorded: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var recordedKey by remember { mutableStateOf("Press any key...") }
    var lastKeyCode by remember { mutableStateOf(-1) }

    // Use a DisposableEffect to manage the native key listener
    DisposableEffect(Unit) {
        val listener = object : NativeKeyListener {
            override fun nativeKeyPressed(e: NativeKeyEvent) {
                val keyText = NativeKeyEvent.getKeyText(e.keyCode)
                val modifiers = NativeKeyEvent.getModifiersText(e.modifiers)
                
                val fullShortcut = if (isFullShortcut && modifiers.isNotBlank()) {
                    "$modifiers+$keyText".replace(" ", "")
                } else {
                    keyText
                }
                
                recordedKey = fullShortcut
                lastKeyCode = e.keyCode
                
                // If it's a single key mode, we might want to auto-save or at least show it clearly.
                // But for now, we'll let the user click Save to confirm.
            }
        }

        GlobalScreen.addNativeKeyListener(listener)
        onDispose {
            GlobalScreen.removeNativeKeyListener(listener)
        }
    }

    AppDialog(
        onCloseRequest = onDismissRequest,
        state = rememberWindowState(width = 450.dp, height = 350.dp),
        title = title,
        selectedTheme = selectedTheme,
        consoleViewModel = consoleViewModel
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                if (isFullShortcut) Icons.Default.Keyboard else Icons.Default.RadioButtonChecked,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                if (isFullShortcut) "Recording Shortcut..." else "Recording Single Key...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (isFullShortcut) "Press a combination (e.g. Ctrl+C)" else "Press a single key (e.g. F12)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        recordedKey,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.height(48.dp).weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { 
                        if (lastKeyCode != -1) {
                            onShortcutRecorded(recordedKey)
                        }
                    },
                    enabled = lastKeyCode != -1,
                    modifier = Modifier.height(48.dp).weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Save Shortcut")
                }
            }
        }
    }
}
