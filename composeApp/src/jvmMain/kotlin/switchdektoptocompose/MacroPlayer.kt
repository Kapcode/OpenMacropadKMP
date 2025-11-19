package switchdektoptocompose

import java.awt.Robot
import java.awt.event.InputEvent

class MacroPlayer {
    private val robot = Robot().apply {
        isAutoWaitForIdle = false
        autoDelay = 50 // Set a default 50ms delay between all robot actions
    }

    fun play(events: List<MacroEventState>) {
        println("MacroPlayer: Starting playback of ${events.size} events.")
        val initialAutoDelay = robot.autoDelay
        try {
            for ((index, event) in events.withIndex()) {
                println("MacroPlayer: Processing event $index: $event")
                when (event) {
                    is MacroEventState.KeyEvent -> {
                        val keyCodes = KeyParser.parseAwtKeys(event.keyName)
                        for (keyCode in keyCodes) {
                            when (event.action) {
                                KeyAction.PRESS -> {
                                    println("MacroPlayer: Robot keyPress $keyCode")
                                    robot.keyPress(keyCode)
                                }
                                KeyAction.RELEASE -> {
                                    println("MacroPlayer: Robot keyRelease $keyCode")
                                    robot.keyRelease(keyCode)
                                }
                            }
                        }
                    }
                    is MacroEventState.SetAutoWaitEvent -> {
                        robot.autoDelay = event.delayMs
                    }
                    is MacroEventState.DelayEvent -> {
                        println("MacroPlayer: Delaying ${event.durationMs}ms")
                        robot.delay(event.durationMs.toInt())
                    }
                    is MacroEventState.MouseEvent -> {
                        if (event.action == MouseAction.MOVE) {
                            println("MacroPlayer: Robot mouseMove ${event.x}, ${event.y}")
                            robot.mouseMove(event.x, event.y)
                        } else {
                            println("Simulating Mouse Event (Not Implemented): ${event.action} at (${event.x}, ${event.y})")
                        }
                    }
                    is MacroEventState.MouseButtonEvent -> {
                        try {
                            val mask = InputEvent.getMaskForButton(event.buttonNumber)
                            println("Simulating Mouse Button: ${event.action} on button ${event.buttonNumber} (Mask: $mask)")
                            when (event.action) {
                                KeyAction.PRESS -> {
                                    println("MacroPlayer: Robot mousePress $mask")
                                    robot.mousePress(mask)
                                }
                                KeyAction.RELEASE -> {
                                    println("MacroPlayer: Robot mouseRelease $mask")
                                    robot.mouseRelease(mask)
                                }
                            }
                        } catch (e: IllegalArgumentException) {
                            System.err.println("Invalid mouse button number: ${event.buttonNumber}")
                        }
                    }
                    is MacroEventState.ScrollEvent -> {
                        println("MacroPlayer: Robot mouseWheel ${event.scrollAmount}")
                        robot.mouseWheel(event.scrollAmount)
                    }
                }
                println("MacroPlayer: Finished event $index")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Restore the initial auto delay after playback
            robot.autoDelay = initialAutoDelay
            println("MacroPlayer: Playback finished.")
        }
    }
}