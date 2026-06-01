package com.kapcode.open.macropad.kmps

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PlatformAdBanner(location: AdLocation, modifier: Modifier) {
    // No-op for JVM/Desktop
}
