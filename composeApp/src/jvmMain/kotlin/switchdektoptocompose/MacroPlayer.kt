package switchdektoptocompose

import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import java.awt.MouseInfo
import java.awt.Robot
import java.awt.event.InputEvent

class MacroPlayer {
    private val robot = Robot().apply {
        isAutoWaitForIdle = false
        autoDelay = 0 // We will handle delays manually to allow cancellation
    }

    suspend fun play(events: List<MacroEventState>) {
        val initialAutoDelay = robot.autoDelay
        try {
            for ((index, event) in events.withIndex()) {
                yield() // Check for cancellation
                // Concise logging
                // println("Event $index: $event") 
                
                when (event) {
                    is MacroEventState.KeyEvent -> {
                        val keyCodes = KeyParser.parseAwtKeys(event.keyName)
                        for (keyCode in keyCodes) {
                            when (event.action) {
                                KeyAction.PRESS -> {
                                    robot.keyPress(keyCode)
                                }
                                KeyAction.RELEASE -> {
                                    robot.keyRelease(keyCode)
                                }
                            }
                            delay(50) // Manual delay
                        }
                    }
                    is MacroEventState.SetAutoWaitEvent -> {
                        // No-op
                    }
                    is MacroEventState.DelayEvent -> {
                        delay(event.durationMs)
                    }
                    is MacroEventState.MouseEvent -> {
                        if (event.action == MouseAction.MOVE) {
                            if (event.isAnimated) {
                                animateMouse(event.x, event.y)
                            } else {
                                robot.mouseMove(event.x, event.y)
                            }
                        } else {
                            println("Warning: Mouse click via MouseEvent not supported, use MouseButtonEvent.")
                        }
                        delay(50)
                    }
                    is MacroEventState.MouseButtonEvent -> {
                        try {
                            val mask = InputEvent.getMaskForButton(event.buttonNumber)
                            when (event.action) {
                                KeyAction.PRESS -> robot.mousePress(mask)
                                KeyAction.RELEASE -> robot.mouseRelease(mask)
                            }
                            delay(50)
                        } catch (e: IllegalArgumentException) {
                            System.err.println("Invalid mouse button number: ${event.buttonNumber}")
                        }
                    }
                    is MacroEventState.ScrollEvent -> {
                        robot.mouseWheel(event.scrollAmount)
                        delay(50)
                    }
                }
            }
        } finally {
            robot.autoDelay = initialAutoDelay
        }
    }

    private suspend fun animateMouse(targetX: Int, targetY: Int) {
        val currentPos = MouseInfo.getPointerInfo().location
        val startX = currentPos.x
        val startY = currentPos.y
        val duration = 500 // Animation duration in ms
        val steps = 50 // Number of steps

        val stepTime = duration / steps
        val dx = (targetX - startX).toDouble() / steps
        val dy = (targetY - startY).toDouble() / steps

        for (i in 1..steps) {
            yield() // Allow cancellation during animation
            val nextX = (startX + dx * i).toInt()
            val nextY = (startY + dy * i).toInt()
            robot.mouseMove(nextX, nextY)
            delay(stepTime.toLong())
        }
    }
}