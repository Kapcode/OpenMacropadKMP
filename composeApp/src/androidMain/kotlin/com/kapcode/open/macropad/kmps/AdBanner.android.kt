package com.kapcode.open.macropad.kmps

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PlatformAdBanner(location: AdLocation, modifier: Modifier) {
    AdmobBanner(location = location, modifier = modifier)
}
