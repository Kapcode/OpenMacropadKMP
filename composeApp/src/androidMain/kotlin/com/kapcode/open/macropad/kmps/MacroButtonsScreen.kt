package com.kapcode.open.macropad.kmps

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MacroButton(
    macroName: String,
    showFeedback: Boolean,
    onClick: () -> Unit
) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(showFeedback) {
        if (showFeedback) {
            alpha.animateTo(0.5f, animationSpec = tween(100))
            alpha.animateTo(0f, animationSpec = tween(100))
        }
    }

    Box {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text(macroName)
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = alpha.value)),
            contentAlignment = Alignment.Center
        ) {}
    }
}

@Composable
fun MacroButtonsScreen(macros: List<String>, onMacroClick: (String) -> Unit) {
    var lastClickedMacro by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        macros.forEach { macroName ->
            MacroButton(
                macroName = macroName,
                showFeedback = lastClickedMacro == macroName,
                onClick = {
                    lastClickedMacro = macroName
                    onMacroClick(macroName)
                    scope.launch {
                        delay(200)
                        lastClickedMacro = null
                    }
                }
            )
        }
    }
}