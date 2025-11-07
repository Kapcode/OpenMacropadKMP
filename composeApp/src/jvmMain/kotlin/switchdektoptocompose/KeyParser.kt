package switchdektoptocompose

import java.awt.event.KeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent

/**
 * A self-contained object to handle parsing key strings and converting them
 * to key codes for both AWT Robot and JNativeHook.
 * This is based on the original KeyMap.kt but is specific to the new Compose UI.
 */
object KeyParser {

    // Simplified map for AWT Robot playback.
    // In a real app, this would be much more comprehensive.
    private val awtKeyMap = mapOf(
        "ctrl" to KeyEvent.VK_CONTROL,
        "shift" to KeyEvent.VK_SHIFT,
        "alt" to KeyEvent.VK_ALT,
        "win" to KeyEvent.VK_WINDOWS,
        "a" to KeyEvent.VK_A,
        "b" to KeyEvent.VK_B,
        "c" to KeyEvent.VK_C
        // ... add all other necessary keys
    )

    // Simplified map for JNativeHook triggers.
    private val nativeHookKeyMap = mapOf(
        "ctrl" to NativeKeyEvent.VC_CONTROL,
        "shift" to NativeKeyEvent.VC_SHIFT,
        "alt" to NativeKeyEvent.VC_ALT,
        "a" to NativeKeyEvent.VC_A,
        "b" to NativeKeyEvent.VC_B,
        "c" to NativeKeyEvent.VC_C
        // ... add all other necessary keys
    )

    /**
     * Parses a string like "Ctrl+C" into a list of AWT key codes.
     */
    fun parseAwtKeys(keysString: String): List<Int> {
        return keysString.split('+', ',')
            .map { it.trim().lowercase() }
            .mapNotNull { awtKeyMap[it] }
    }
    
    /**
     * Parses a string like "Ctrl,C" into a list of JNativeHook key codes.
     */
    fun parseNativeHookKeys(keysString: String): List<Int> {
        return keysString.split('+', ',')
            .map { it.trim().lowercase() }
            .mapNotNull { nativeHookKeyMap[it] }
    }
}