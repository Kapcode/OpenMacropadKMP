package com.kapcode.open.macropad.kmps

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformAdBanner(location: AdLocation, modifier: Modifier = Modifier)
