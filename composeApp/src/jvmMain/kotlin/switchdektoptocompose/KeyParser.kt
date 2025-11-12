package switchdektoptocompose

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import java.awt.event.KeyEvent

object KeyParser {

    private val awtKeyMap = mapOf(
        "ENTER" to KeyEvent.VK_ENTER, "BACKSPACE" to KeyEvent.VK_BACK_SPACE, "TAB" to KeyEvent.VK_TAB,
        "SHIFT" to KeyEvent.VK_SHIFT, "CONTROL" to KeyEvent.VK_CONTROL, "CTRL" to KeyEvent.VK_CONTROL,
        "ALT" to KeyEvent.VK_ALT, "ESCAPE" to KeyEvent.VK_ESCAPE, "SPACE" to KeyEvent.VK_SPACE,
        "LEFT" to KeyEvent.VK_LEFT, "UP" to KeyEvent.VK_UP, "RIGHT" to KeyEvent.VK_RIGHT, "DOWN" to KeyEvent.VK_DOWN,
        "A" to KeyEvent.VK_A, "B" to KeyEvent.VK_B, "C" to KeyEvent.VK_C, "D" to KeyEvent.VK_D, "E" to KeyEvent.VK_E,
        "F" to KeyEvent.VK_F, "G" to KeyEvent.VK_G, "H" to KeyEvent.VK_H, "I" to KeyEvent.VK_I, "J" to KeyEvent.VK_J,
        "K" to KeyEvent.VK_K, "L" to KeyEvent.VK_L, "M" to KeyEvent.VK_M, "N" to KeyEvent.VK_N, "O" to KeyEvent.VK_O,
        "P" to KeyEvent.VK_P, "Q" to KeyEvent.VK_Q, "R" to KeyEvent.VK_R, "S" to KeyEvent.VK_S, "T" to KeyEvent.VK_T,
        "U" to KeyEvent.VK_U, "V" to KeyEvent.VK_V, "W" to KeyEvent.VK_W, "X" to KeyEvent.VK_X, "Y" to KeyEvent.VK_Y,
        "Z" to KeyEvent.VK_Z,
        // Use VK_META for Robot on Linux, which is aliased correctly on Windows.
        "WINDOWS" to KeyEvent.VK_META, "WIN" to KeyEvent.VK_META, "META" to KeyEvent.VK_META,
        "F1" to KeyEvent.VK_F1, "F2" to KeyEvent.VK_F2, "F3" to KeyEvent.VK_F3, "F4" to KeyEvent.VK_F4,
        "F5" to KeyEvent.VK_F5, "F6" to KeyEvent.VK_F6, "F7" to KeyEvent.VK_F7, "F8" to KeyEvent.VK_F8,
        "F9" to KeyEvent.VK_F9, "F10" to KeyEvent.VK_F10, "F11" to KeyEvent.VK_F11, "F12" to KeyEvent.VK_F12,
        "DELETE" to KeyEvent.VK_DELETE,
    ).withDefault { KeyEvent.VK_UNDEFINED }

    private val nativeHookKeyMap = mapOf(
        "ENTER" to NativeKeyEvent.VC_ENTER, "BACKSPACE" to NativeKeyEvent.VC_BACKSPACE, "TAB" to NativeKeyEvent.VC_TAB,
        "SHIFT" to NativeKeyEvent.VC_SHIFT, "CONTROL" to NativeKeyEvent.VC_CONTROL, "CTRL" to NativeKeyEvent.VC_CONTROL,
        "ALT" to NativeKeyEvent.VC_ALT, "ESCAPE" to NativeKeyEvent.VC_ESCAPE, "SPACE" to NativeKeyEvent.VC_SPACE,
        "LEFT" to NativeKeyEvent.VC_LEFT, "UP" to NativeKeyEvent.VC_UP, "RIGHT" to NativeKeyEvent.VC_RIGHT, "DOWN" to NativeKeyEvent.VC_DOWN,
        "A" to NativeKeyEvent.VC_A, "B" to NativeKeyEvent.VC_B, "C" to NativeKeyEvent.VC_C, "D" to NativeKeyEvent.VC_D, "E" to NativeKeyEvent.VC_E,
        "F" to NativeKeyEvent.VC_F, "G" to NativeKeyEvent.VC_G, "H" to NativeKeyEvent.VC_H, "I" to NativeKeyEvent.VC_I, "J" to NativeKeyEvent.VC_J,
        "K" to NativeKeyEvent.VC_K, "L" to NativeKeyEvent.VC_L, "M" to NativeKeyEvent.VC_M, "N" to NativeKeyEvent.VC_N, "O" to NativeKeyEvent.VC_O,
        "P" to NativeKeyEvent.VC_P, "Q" to NativeKeyEvent.VC_Q, "R" to NativeKeyEvent.VC_R, "S" to NativeKeyEvent.VC_S, "T" to NativeKeyEvent.VC_T,
        "U" to NativeKeyEvent.VC_U, "V" to NativeKeyEvent.VC_V, "W" to NativeKeyEvent.VC_W, "X" to NativeKeyEvent.VC_X, "Y" to NativeKeyEvent.VC_Y,
        "Z" to NativeKeyEvent.VC_Z, "WINDOWS" to NativeKeyEvent.VC_META, "WIN" to NativeKeyEvent.VC_META, "META" to NativeKeyEvent.VC_META,
        "F1" to NativeKeyEvent.VC_F1, "F2" to NativeKeyEvent.VC_F2, "F3" to NativeKeyEvent.VC_F3, "F4" to NativeKeyEvent.VC_F4,
        "F5" to NativeKeyEvent.VC_F5, "F6" to NativeKeyEvent.VC_F6, "F7" to NativeKeyEvent.VC_F7, "F8" to NativeKeyEvent.VC_F8,
        "F9" to NativeKeyEvent.VC_F9, "F10" to NativeKeyEvent.VC_F10, "F11" to NativeKeyEvent.VC_F11, "F12" to NativeKeyEvent.VC_F12,
        "DELETE" to NativeKeyEvent.VC_DELETE,
    ).withDefault { NativeKeyEvent.VC_UNDEFINED }

    fun parseAwtKeys(keysString: String): List<Int> {
        return keysString.split('+', ',')
            .map { it.trim().uppercase() }
            .mapNotNull { awtKeyMap[it] }
    }
    
    fun parseNativeHookKeys(keysString: String): List<Int> {
        return keysString.split('+', ',')
            .map { it.trim().uppercase() }
            .mapNotNull { nativeHookKeyMap[it] }
    }
}