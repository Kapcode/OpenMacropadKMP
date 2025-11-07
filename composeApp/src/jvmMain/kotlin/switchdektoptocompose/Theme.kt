package switchdektoptocompose

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// High-contrast blue for accents
val accentBlue = Color(0xFF3385FF)

// --- Dark Blue Theme ---
val DarkBlueColorScheme = darkColorScheme(
    primary = accentBlue,
    secondary = Color(0xFF526070),
    background = Color(0xFF1A1C1E),
    surface = Color(0xFF2C2F33),
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
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1C1E), // High-emphasis dark gray for text
    onSurface = Color(0xFF1A1C1E)      // High-emphasis dark gray for text
)