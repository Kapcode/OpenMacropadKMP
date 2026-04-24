package switchdektoptocompose.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

data class TooltipSettings(
    val xOffset: Int = 0,
    val yOffset: Int = -24
)

val LocalTooltipSettings = staticCompositionLocalOf { TooltipSettings() }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTooltipArea(
    tooltipText: String,
    delayMillis: Int = 500,
    xOffset: Int? = null,
    yOffset: Int? = null,
    content: @Composable () -> Unit
) {
    val settings = LocalTooltipSettings.current
    val finalXOffset = xOffset ?: settings.xOffset
    val finalYOffset = yOffset ?: settings.yOffset

    TooltipArea(
        tooltip = {
            Surface(
                shape = MaterialTheme.shapes.small,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = tooltipText,
                    modifier = Modifier.padding(4.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        tooltipPlacement = TooltipPlacement.CursorPoint(
            alignment = Alignment.TopCenter,
            offset = DpOffset(finalXOffset.dp, finalYOffset.dp)
        ),
        delayMillis = delayMillis,
        content = content
    )
}
