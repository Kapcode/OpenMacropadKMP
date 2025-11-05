import java.awt.event.KeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent

object KeyMap {
    // Map for java.awt.Robot playback (uses KeyEvent.VK_ codes)
    val awtKeyCodeMap: Map<String, Int> = mapOf(
        // Letters
        "A" to KeyEvent.VK_A,
        "B" to KeyEvent.VK_B,
        "C" to KeyEvent.VK_C,
        "D" to KeyEvent.VK_D,
        "E" to KeyEvent.VK_E,
        "F" to KeyEvent.VK_F,
        "G" to KeyEvent.VK_G,
        "H" to KeyEvent.VK_H,
        "I" to KeyEvent.VK_I,
        "J" to KeyEvent.VK_J,
        "K" to KeyEvent.VK_K,
        "L" to KeyEvent.VK_L,
        "M" to KeyEvent.VK_M,
        "N" to KeyEvent.VK_N,
        "O" to KeyEvent.VK_O,
        "P" to KeyEvent.VK_P,
        "Q" to KeyEvent.VK_Q,
        "R" to KeyEvent.VK_R,
        "S" to KeyEvent.VK_S,
        "T" to KeyEvent.VK_T,
        "U" to KeyEvent.VK_U,
        "V" to KeyEvent.VK_V,
        "W" to KeyEvent.VK_W,
        "X" to KeyEvent.VK_X,
        "Y" to KeyEvent.VK_Y,
        "Z" to KeyEvent.VK_Z,

        // Numbers
        "0" to KeyEvent.VK_0,
        "1" to KeyEvent.VK_1,
        "2" to KeyEvent.VK_2,
        "3" to KeyEvent.VK_3,
        "4" to KeyEvent.VK_4,
        "5" to KeyEvent.VK_5,
        "6" to KeyEvent.VK_6,
        "7" to KeyEvent.VK_7,
        "8" to KeyEvent.VK_8,
        "9" to KeyEvent.VK_9,

        // Function Keys
        "F1" to KeyEvent.VK_F1,
        "F2" to KeyEvent.VK_F2,
        "F3" to KeyEvent.VK_F3,
        "F4" to KeyEvent.VK_F4,
        "F5" to KeyEvent.VK_F5,
        "F6" to KeyEvent.VK_F6,
        "F7" to KeyEvent.VK_F7,
        "F8" to KeyEvent.VK_F8,
        "F9" to KeyEvent.VK_F9,
        "F10" to KeyEvent.VK_F10,
        "F11" to KeyEvent.VK_F11,
        "F12" to KeyEvent.VK_F12,

        // Modifiers
        "SHIFT" to KeyEvent.VK_SHIFT,
        "CTRL" to KeyEvent.VK_CONTROL,
        "ALT" to KeyEvent.VK_ALT,
        "WINDOWS" to KeyEvent.VK_WINDOWS,
        "META" to KeyEvent.VK_META, // Often same as Windows/Command

        // Special Keys
        "SPACE" to KeyEvent.VK_SPACE,
        "ENTER" to KeyEvent.VK_ENTER,
        "ESCAPE" to KeyEvent.VK_ESCAPE,
        "BACK_SPACE" to KeyEvent.VK_BACK_SPACE,
        "TAB" to KeyEvent.VK_TAB,
        "CAPS_LOCK" to KeyEvent.VK_CAPS_LOCK,
        "NUM_LOCK" to KeyEvent.VK_NUM_LOCK,
        "SCROLL_LOCK" to KeyEvent.VK_SCROLL_LOCK,
        "INSERT" to KeyEvent.VK_INSERT,
        "DELETE" to KeyEvent.VK_DELETE,
        "HOME" to KeyEvent.VK_HOME,
        "END" to KeyEvent.VK_END,
        "PAGE_UP" to KeyEvent.VK_PAGE_UP,
        "PAGE_DOWN" to KeyEvent.VK_PAGE_DOWN,
        "UP" to KeyEvent.VK_UP,
        "DOWN" to KeyEvent.VK_DOWN,
        "LEFT" to KeyEvent.VK_LEFT,
        "RIGHT" to KeyEvent.VK_RIGHT,
        "PRINTSCREEN" to KeyEvent.VK_PRINTSCREEN,
        "PAUSE" to KeyEvent.VK_PAUSE,
        "CONTEXT_MENU" to KeyEvent.VK_CONTEXT_MENU,

        // Punctuation and Symbols
        "COMMA" to KeyEvent.VK_COMMA,
        "PERIOD" to KeyEvent.VK_PERIOD,
        "SLASH" to KeyEvent.VK_SLASH,
        "SEMICOLON" to KeyEvent.VK_SEMICOLON,
        "EQUALS" to KeyEvent.VK_EQUALS,
        "MINUS" to KeyEvent.VK_MINUS,
        "OPEN_BRACKET" to KeyEvent.VK_OPEN_BRACKET,
        "BACK_SLASH" to KeyEvent.VK_BACK_SLASH,
        "CLOSE_BRACKET" to KeyEvent.VK_CLOSE_BRACKET,
        "QUOTE" to KeyEvent.VK_QUOTE,
        "BACK_QUOTE" to KeyEvent.VK_BACK_QUOTE,

        // Aliases
        "ESC" to KeyEvent.VK_ESCAPE,
        "CONTROL" to KeyEvent.VK_CONTROL,
        "TILDE" to KeyEvent.VK_BACK_QUOTE,
        "CONTEXT" to KeyEvent.VK_CONTEXT_MENU
    )

    // Map for JNativeHook event handling (uses NativeKeyEvent.VC_ codes)
    val stringToNativeKeyCodeMap: Map<String, Int> = mapOf(
        // Letters
        "A" to NativeKeyEvent.VC_A,
        "B" to NativeKeyEvent.VC_B,
        "C" to NativeKeyEvent.VC_C,
        "D" to NativeKeyEvent.VC_D,
        "E" to NativeKeyEvent.VC_E,
        "F" to NativeKeyEvent.VC_F,
        "G" to NativeKeyEvent.VC_G,
        "H" to NativeKeyEvent.VC_H,
        "I" to NativeKeyEvent.VC_I,
        "J" to NativeKeyEvent.VC_J,
        "K" to NativeKeyEvent.VC_K,
        "L" to NativeKeyEvent.VC_L,
        "M" to NativeKeyEvent.VC_M,
        "N" to NativeKeyEvent.VC_N,
        "O" to NativeKeyEvent.VC_O,
        "P" to NativeKeyEvent.VC_P,
        "Q" to NativeKeyEvent.VC_Q,
        "R" to NativeKeyEvent.VC_R,
        "S" to NativeKeyEvent.VC_S,
        "T" to NativeKeyEvent.VC_T,
        "U" to NativeKeyEvent.VC_U,
        "V" to NativeKeyEvent.VC_V,
        "W" to NativeKeyEvent.VC_W,
        "X" to NativeKeyEvent.VC_X,
        "Y" to NativeKeyEvent.VC_Y,
        "Z" to NativeKeyEvent.VC_Z,

        // Numbers
        "0" to NativeKeyEvent.VC_0,
        "1" to NativeKeyEvent.VC_1,
        "2" to NativeKeyEvent.VC_2,
        "3" to NativeKeyEvent.VC_3,
        "4" to NativeKeyEvent.VC_4,
        "5" to NativeKeyEvent.VC_5,
        "6" to NativeKeyEvent.VC_6,
        "7" to NativeKeyEvent.VC_7,
        "8" to NativeKeyEvent.VC_8,
        "9" to NativeKeyEvent.VC_9,

        // Function Keys
        "F1" to NativeKeyEvent.VC_F1,
        "F2" to NativeKeyEvent.VC_F2,
        "F3" to NativeKeyEvent.VC_F3,
        "F4" to NativeKeyEvent.VC_F4,
        "F5" to NativeKeyEvent.VC_F5,
        "F6" to NativeKeyEvent.VC_F6,
        "F7" to NativeKeyEvent.VC_F7,
        "F8" to NativeKeyEvent.VC_F8,
        "F9" to NativeKeyEvent.VC_F9,
        "F10" to NativeKeyEvent.VC_F10,
        "F11" to NativeKeyEvent.VC_F11,
        "F12" to NativeKeyEvent.VC_F12,

        // Modifiers (Generic)
        "SHIFT" to NativeKeyEvent.VC_SHIFT,
        "CTRL" to NativeKeyEvent.VC_CONTROL,
        "ALT" to NativeKeyEvent.VC_ALT,
        "WINDOWS" to NativeKeyEvent.VC_META,
        "META" to NativeKeyEvent.VC_META,

        // Special Keys
        "SPACE" to NativeKeyEvent.VC_SPACE,
        "ENTER" to NativeKeyEvent.VC_ENTER,
        "ESCAPE" to NativeKeyEvent.VC_ESCAPE,
        "BACK_SPACE" to NativeKeyEvent.VC_BACKSPACE,
        "TAB" to NativeKeyEvent.VC_TAB,
        "CAPS_LOCK" to NativeKeyEvent.VC_CAPS_LOCK,
        "NUM_LOCK" to NativeKeyEvent.VC_NUM_LOCK,
        "SCROLL_LOCK" to NativeKeyEvent.VC_SCROLL_LOCK,
        "INSERT" to NativeKeyEvent.VC_INSERT,
        "DELETE" to NativeKeyEvent.VC_DELETE,
        "HOME" to NativeKeyEvent.VC_HOME,
        "END" to NativeKeyEvent.VC_END,
        "PAGE_UP" to NativeKeyEvent.VC_PAGE_UP,
        "PAGE_DOWN" to NativeKeyEvent.VC_PAGE_DOWN,
        "UP" to NativeKeyEvent.VC_UP,
        "DOWN" to NativeKeyEvent.VC_DOWN,
        "LEFT" to NativeKeyEvent.VC_LEFT,
        "RIGHT" to NativeKeyEvent.VC_RIGHT,
        "PRINTSCREEN" to NativeKeyEvent.VC_PRINTSCREEN,
        "PAUSE" to NativeKeyEvent.VC_PAUSE,
        "CONTEXT_MENU" to NativeKeyEvent.VC_CONTEXT_MENU,

        // Punctuation and Symbols
        "COMMA" to NativeKeyEvent.VC_COMMA,
        "PERIOD" to NativeKeyEvent.VC_PERIOD,
        "SLASH" to NativeKeyEvent.VC_SLASH,
        "SEMICOLON" to NativeKeyEvent.VC_SEMICOLON,
        "EQUALS" to NativeKeyEvent.VC_EQUALS,
        "MINUS" to NativeKeyEvent.VC_MINUS,
        "OPEN_BRACKET" to NativeKeyEvent.VC_OPEN_BRACKET,
        "BACK_SLASH" to NativeKeyEvent.VC_BACK_SLASH,
        "CLOSE_BRACKET" to NativeKeyEvent.VC_CLOSE_BRACKET,
        "QUOTE" to NativeKeyEvent.VC_QUOTE,
        "BACK_QUOTE" to NativeKeyEvent.VC_BACKQUOTE,

        // Aliases
        "ESC" to NativeKeyEvent.VC_ESCAPE,
        "CONTROL" to NativeKeyEvent.VC_CONTROL,
        "TILDE" to NativeKeyEvent.VC_BACKQUOTE,
        "CONTEXT" to NativeKeyEvent.VC_CONTEXT_MENU
    )

    val charToKeyTextMap = mapOf(
        // Numbers
        '0' to "0", '1' to "1", '2' to "2", '3' to "3", '4' to "4",
        '5' to "5", '6' to "6", '7' to "7", '8' to "8", '9' to "9",

        // Letters (lowercase for direct mapping)
        'a' to "A", 'b' to "B", 'c' to "C", 'd' to "D", 'e' to "E",
        'f' to "F", 'g' to "G", 'h' to "H", 'i' to "I", 'j' to "J",
        'k' to "K", 'l' to "L", 'm' to "M", 'n' to "N", 'o' to "O",
        'p' to "P", 'q' to "Q", 'r' to "R", 's' to "S", 't' to "T",
        'u' to "U", 'v' to "V", 'w' to "W", 'x' to "X", 'y' to "Y",
        'z' to "Z",

        // Special characters that don't require Shift for their base form
        ' ' to "SPACE",
        '\n' to "ENTER",
        '\t' to "TAB",
        '.' to "PERIOD",
        ',' to "COMMA",
        '/' to "SLASH",
        ';' to "SEMICOLON",
        '=' to "EQUALS",
        '-' to "MINUS",
        '[' to "OPEN_BRACKET",
        '\\' to "BACK_SLASH",
        ']' to "CLOSE_BRACKET",
        '\'' to "QUOTE",
        '`' to "BACK_QUOTE"
    )
}