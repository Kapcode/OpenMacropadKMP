package com.kapcode.open.macropad.kmps

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Global state to track if a foreground ad (e.g. in a dialog) is active.
 * Used to hide background ads to comply with "one ad per screen" best practices.
 */
object AdVisibilityManager {
    var isForegroundAdVisible by mutableStateOf(false)
}

@Composable
fun AdmobBanner(
    modifier: Modifier = Modifier,
    adSize: AdSize = AdSize.BANNER
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(adSize)
                adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test ad unit ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
