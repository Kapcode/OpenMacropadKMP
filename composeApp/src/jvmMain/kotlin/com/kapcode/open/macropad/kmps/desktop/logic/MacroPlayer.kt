package com.kapcode.open.macropad.kmps.desktop.logic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import com.kapcode.open.macropad.kmps.desktop.model.*
import java.awt.MouseInfo
import java.awt.Robot
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

class MacroPlayer(
    private val onLog: (LogLevel, String) -> Unit = { _, _ -> },
    private val getActiveProcess: () -> String? = { null },
    private val onPlayMacroRequested: (String) -> Unit = { },
    private val onNotify: (String) -> Unit = { },
) {
    private val robot = Robot().apply {
        isAutoWaitForIdle = false
        autoDelay = 0 // We will handle delays manually to allow cancellation
    }
    
    private var currentAutoDelay = 50L // Default manual delay

    private val jsContext: Context by lazy {
        Context.newBuilder("js")
            .allowHostAccess(HostAccess.ALL)
            .allowHostClassLookup { false } // Restrict access to Java classes
            .option("engine.WarnInterpreterOnly", "false") // Silence harmless performance warning
            .build().apply {
                getBindings("js").putMember("kap", KapHostApi())
            }
    }

    inner class KapHostApi {
        @Suppress("unused")
        fun pressKey(keyName: String) {
            val keyCodes = KeyParser.parseAwtKeys(keyName)
            keyCodes.forEach { robot.keyPress(it) }
        }
        @Suppress("unused")
        fun releaseKey(keyName: String) {
            val keyCodes = KeyParser.parseAwtKeys(keyName)
            keyCodes.forEach { robot.keyRelease(it) }
        }
        @Suppress("unused")
        fun moveMouse(x: Int, y: Int) {
            robot.mouseMove(x, y)
        }
        @Suppress("unused")
        fun clickMouse(button: Int) {
            val mask = InputEvent.getMaskForButton(button)
            robot.mousePress(mask)
            robot.mouseRelease(mask)
        }
        @Suppress("unused")
        fun delay(ms: Long) {
            Thread.sleep(ms)
        }
        @Suppress("unused")
        fun log(message: String) {
            onLog(LogLevel.Info, "[JS] $message")
        }
        @Suppress("unused")
        fun getActiveProcess(): String? {
            return this@MacroPlayer.getActiveProcess()
        }
        @Suppress("unused")
        fun playMacro(macroId: String) {
            onPlayMacroRequested(macroId)
        }
        @Suppress("unused")
        fun notify(message: String) {
            onNotify(message)
        }
        @Suppress("unused")
        fun getClipboardText(): String? {
            return try {
                val transferable = java.awt.Toolkit.getDefaultToolkit().systemClipboard.getContents(null)
                if (transferable != null && transferable.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)) {
                    transferable.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor) as String
                } else null
            } catch (_: Exception) { null }
        }
        @Suppress("unused")
        fun setClipboardText(text: String) {
            try {
                val selection = java.awt.datatransfer.StringSelection(text)
                java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
            } catch (_: Exception) { }
        }
    }

    suspend fun executeScript(script: String) {
        withContext(Dispatchers.Default) {
            try {
                jsContext.eval("js", script)
            } catch (e: Exception) {
                onLog(LogLevel.Error, "Script error: ${e.message}")
            }
        }
    }

    suspend fun play(events: List<MacroEventState>) {
        val initialAutoDelay = robot.autoDelay
        try {
            for (event in events) {
                yield() // Check for cancellation
                
                when (event) {
                    is MacroEventState.KeyEvent -> {
                        val keyCodes = KeyParser.parseAwtKeys(event.keyName)
                        if (keyCodes.isEmpty()) {
                            onLog(LogLevel.Warn, "No AWT mapping for key: ${event.keyName}")
                        }
                        for (keyCode in keyCodes) {
                            try {
                                when (event.action) {
                                    KeyAction.PRESS -> {
                                        robot.keyPress(keyCode)
                                    }
                                    KeyAction.RELEASE -> {
                                        robot.keyRelease(keyCode)
                                    }
                                }
                            } catch (e: Exception) {
                                onLog(LogLevel.Error, "Robot failed to ${event.action} key $keyCode (${event.keyName}): ${e.message}")
                            }
                            delay(currentAutoDelay)
                        }
                    }
                    is MacroEventState.SetAutoWaitEvent -> {
                        currentAutoDelay = event.delayMs.toLong()
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
                        delay(currentAutoDelay)
                    }
                    is MacroEventState.MouseButtonEvent -> {
                        try {
                            val mask = InputEvent.getMaskForButton(event.buttonNumber)
                            when (event.action) {
                                KeyAction.PRESS -> robot.mousePress(mask)
                                KeyAction.RELEASE -> robot.mouseRelease(mask)
                            }
                            delay(currentAutoDelay)
                        } catch (_: IllegalArgumentException) {
                            System.err.println("Invalid mouse button number: ${event.buttonNumber}")
                        }
                    }
                    is MacroEventState.ScrollEvent -> {
                        robot.mouseWheel(-event.scrollAmount)
                        delay(currentAutoDelay)
                    }
                    is MacroEventState.ScriptEvent -> {
                        executeScript(event.script)
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
    
    fun emergencyReleaseAll() {
        try {
            // Release modifier keys
            robot.keyRelease(KeyEvent.VK_SHIFT)
            robot.keyRelease(KeyEvent.VK_CONTROL)
            robot.keyRelease(KeyEvent.VK_ALT)
            robot.keyRelease(KeyEvent.VK_META) // Windows/Super key

            // Release mouse buttons
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
            robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK)
            robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK)
        } catch (e: Exception) {
            // Logged by the caller
        }
    }
}
