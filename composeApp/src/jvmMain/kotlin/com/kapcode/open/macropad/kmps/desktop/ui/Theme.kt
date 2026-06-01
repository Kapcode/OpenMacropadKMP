package com.kapcode.open.macropad.kmps.desktop.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

// High-contrast blue for accents
val accentBlue = Color(0xFF3385FF)

// --- Dark Blue Theme ---
val DarkBlueColorScheme = darkColorScheme(
    primary = accentBlue,
    secondary = Color(0xFF526070),
    background = Color(0xFF1A1C1E),
    surface = Color(0xFF1E2022),
    surfaceVariant = Color(0xFF2C2F33),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE3E3E3), // High-emphasis off-white for text
    onSurface = Color(0xFFE3E3E3)      // High-emphasis off-white for text
)

// --- Light Blue Theme ---
val LightBlueColorScheme = lightColorScheme(
    primary = accentBlue,
    secondary = Color(0xFF526070),
    background = Color(0xFFF7F9FC), // Clean, slightly off-white
    surface = Color.White,
    surfaceVariant = Color(0xFFEDF1F5),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1C1E), // High-emphasis dark gray for text
    onSurface = Color(0xFF1A1C1E)      // High-emphasis dark gray for text
)

/**
 * Generates a slightly different background color for each panel to make them distinguishable.
 * Offsets saturation and brightness based on the theme's surface color.
 */
@Composable
fun panelBackground(index: Int): Color {
    val surface = MaterialTheme.colorScheme.surface
    val argb = surface.toArgb()
    
    val hsb = FloatArray(3)
    java.awt.Color.RGBtoHSB((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF, hsb)
    
    val isDark = hsb[2] < 0.5f
    
    // Offset saturation and brightness based on index
    // We use the theme's color value as a base and shift it
    val sStep = 0.01f
    val vStep = 0.025f
    
    // Increment saturation slightly to give each panel a unique "depth"
    hsb[1] = (hsb[1] + sStep * index).coerceIn(0f, 1f)
    
    if (isDark) {
        // In dark mode, higher index panels get slightly lighter
        hsb[2] = (hsb[2] + vStep * index).coerceIn(0f, 1f)
    } else {
        // In light mode, higher index panels get slightly darker
        hsb[2] = (hsb[2] - vStep * index).coerceIn(0f, 1f)
    }
    
    val rgb = java.awt.Color.HSBtoRGB(hsb[0], hsb[1], hsb[2])
    // Preserve alpha channel from the original surface color
    return Color((argb and 0xFF000000.toInt()) or (rgb and 0x00FFFFFF))
}
