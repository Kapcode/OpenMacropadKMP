package com.kapcode.open.macropad.kmps.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.WindowScope
import kotlinx.coroutines.delay

/**
 * A reusable logic block that performs an "aggressive redraw" sequence to prevent
 * the common Compose for Desktop glitch where windows appear as transparent or "smeared"
 * frames until clicked or resized.
 */
@Composable
fun WindowScope.RedrawFix(
    trigger: Any? = Unit,
    enabled: Boolean = true,
    stages: Int = 15,
    initialDelay: Long = 10,
    stageDelay: Long = 50
) {
    if (!enabled) return

    LaunchedEffect(trigger) {
        delay(initialDelay) // Small breathing room for window manager
        repeat(stages) { stage ->
            try {
                if (window.isVisible) {
                    window.toFront()
                    window.requestFocusInWindow()
                    window.revalidate()
                    window.repaint()

                    // On several stages, perform a tiny move and resize to trigger window manager refresh
                    // This is the most reliable way to fix the "stuck frame" issue in Linux/VM environments.
                    if (stage % 4 == 0) {
                        val pos = window.location
                        val size = window.size
                        window.setLocation(pos.x + 1, pos.y)
                        window.setSize(size.width + 1, size.height)
                        delay(10)
                        window.setLocation(pos.x, pos.y)
                        window.setSize(size.width, size.height)
                    }
                }
            } catch (e: Exception) {
                // Ignore window state errors during rapid init
            }

            delay(if (stage == 0) initialDelay else stageDelay)
        }
    }
}
