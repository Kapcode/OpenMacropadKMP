package switchdektoptocompose

import java.awt.Robot

class MacroPlayer {
    private val robot = Robot().apply {
        isAutoWaitForIdle = true
        autoDelay = 50 // Set a default 50ms delay between all robot actions
    }

    fun play(events: List<MacroEventState>) {
        val initialAutoDelay = robot.autoDelay
        try {
            for (event in events) {
                when (event) {
                    is MacroEventState.KeyEvent -> {
                        val keyCodes = KeyParser.parseAwtKeys(event.keyName)
                        for (keyCode in keyCodes) {
                            when (event.action) {
                                KeyAction.PRESS -> robot.keyPress(keyCode)
                                KeyAction.RELEASE -> robot.keyRelease(keyCode)
                            }
                        }
                    }
                    is MacroEventState.SetAutoWaitEvent -> {
                        robot.autoDelay = event.delayMs
                    }
                    is MacroEventState.DelayEvent -> {
                        robot.delay(event.durationMs.toInt())
                    }
                    is MacroEventState.MouseEvent -> {
                        println("Simulating Mouse Event (Not Implemented): ${event.action} at (${event.x}, ${event.y})")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Restore the initial auto delay after playback
            robot.autoDelay = initialAutoDelay
        }
    }
}