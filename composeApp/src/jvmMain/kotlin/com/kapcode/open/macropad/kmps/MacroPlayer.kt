import AppSettings
import org.json.JSONObject
import java.awt.MouseInfo
import java.awt.Robot
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.File

class MacroPlayer {

    // Public entry point to start a macro playback
    fun play(macroJSON: String) {
        val robot = Robot()
        robot.isAutoWaitForIdle = true
        playMacroInternal(macroJSON, robot)
    }

    private fun playMacroInternal(macroJSON: String, robot: Robot, startIndex: Int = 0, callStack: MutableSet<String> = mutableSetOf()) {
        val json = JSONObject(macroJSON)
        val eventsArray = json.getJSONArray("events")

        for (i in startIndex until eventsArray.length()) {
            val eventObject = eventsArray.getJSONObject(i)
            when (eventObject.getString("type")) {
                "key" -> {
                    val keyText = eventObject.getString("key")
                    val command = eventObject.getString("command")
                    // Use uppercase for map lookup to ensure consistency
                    val keyCode = KeyMap.awtKeyCodeMap[keyText.uppercase()]
                    if (keyCode != null) {
                        when (command) {
                            "PRESS" -> robot.keyPress(keyCode)
                            "RELEASE" -> robot.keyRelease(keyCode)
                        }
                    } else {
                        println("Warning: Key not found in awtKeyCodeMap: $keyText")
                    }
                }
                "mouse" -> {
                    val command = eventObject.getString("command")
                    when (command) {
                        "PRESS" -> robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                        "RELEASE" -> robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
                        "CLICK" -> {
                            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
                        }
                        "SNAP_TO", "DRAG" -> {
                            val x = eventObject.getInt("x")
                            val y = eventObject.getInt("y")
                            robot.mouseMove(x, y)
                        }
                        "ANIMATE_TO" -> {
                            val targetX = eventObject.getInt("x")
                            val targetY = eventObject.getInt("y")
                            val startPoint = MouseInfo.getPointerInfo().location
                            val startX = startPoint.x
                            val startY = startPoint.y

                            val steps = 50
                            for (j in 0..steps) {
                                val currentX = startX + (targetX - startX) * j / steps
                                val currentY = startY + (targetY - startY) * j / steps
                                robot.mouseMove(currentX, currentY)
                                robot.delay(10) // Keep delay for animation smoothness
                            }
                            robot.mouseMove(targetX, targetY)
                        }
                    }
                }
                "run_macro" -> {
                    val macroName = eventObject.getString("macro_name")
                    if (callStack.contains(macroName)) {
                        println("Infinite loop detected: Macro '$macroName' is already in the call stack.")
                        continue // Skip this event to prevent infinite recursion
                    }

                    val macroFolder = File(AppSettings.macroDirectory)
                    val fileToRun = File(macroFolder, macroName)
                    if (fileToRun.exists()) {
                        val newCallStack = callStack.toMutableSet()
                        newCallStack.add(macroName)
                        playMacroInternal(fileToRun.readText(), robot, 0, newCallStack)
                    } else {
                        println("Macro to run not found: $macroName")
                    }
                }
                "set_auto_wait" -> {
                    val waitValue = eventObject.getInt("value")
                    robot.autoDelay = waitValue
                }
            }
        }
    }

    fun typeMacro(macroText: String) {
        val robot = Robot()
        robot.isAutoWaitForIdle = true // Also apply this fix here
        val shiftChars = "~!@#$%^&*()_+{}|:\"<>?"

        for (char in macroText) {
            val isUpperCase = char.isUpperCase()
            val needsShift = isUpperCase || shiftChars.contains(char)

            val keyText = KeyMap.charToKeyTextMap[char.lowercaseChar()] ?: when (char) {
                '~' -> "Back Quote"
                '!' -> "1"
                '@' -> "2"
                '#' -> "3"
                '$' -> "4"
                '%' -> "5"
                '^' -> "6"
                '&' -> "7"
                '*' -> "8"
                '(' -> "9"
                ')' -> "0"
                '_' -> "Minus"
                '+' -> "Equals"
                '{' -> "Open Bracket"
                '}' -> "Close Bracket"
                '|' -> "Back Slash"
                ':' -> "Semicolon"
                '\"' -> "Quote"
                '<' -> "Comma"
                '>' -> "Period"
                '?' -> "Slash"
                else -> null
            }

            if (keyText != null) {
                val keyCode = KeyMap.awtKeyCodeMap[keyText.uppercase()]
                if (keyCode != null) {
                    if (needsShift) {
                        robot.keyPress(KeyEvent.VK_SHIFT)
                    }

                    robot.keyPress(keyCode)
                    robot.keyRelease(keyCode)

                    if (needsShift) {
                        robot.keyRelease(KeyEvent.VK_SHIFT)
                    }
                }
            }
        }
    }
}