package switchdektoptocompose.logic

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import switchdektoptocompose.model.*
import switchdektoptocompose.viewmodel.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

data class ActiveTrigger(
    val keyCode: Int,
    val macro: MacroFileState,
    val allowedClients: String
)

class TriggerListener(
    private val viewModel: DesktopViewModel,
    private val onTrigger: (MacroFileState) -> Unit
) : NativeKeyListener {

    private val activeTriggers = ConcurrentHashMap<Int, MutableList<ActiveTrigger>>()
    private var eStopKeyCode: Int? = null
    private var copyConsoleShortcut: String? = null
    private var stopKeyShortcut: String? = null
    private var inspectKeyShortcut: String? = null
    private val listenerScope = CoroutineScope(Dispatchers.Default)

    init {
        val logger = Logger.getLogger(GlobalScreen::class.java.getPackage().name)
        logger.level = Level.WARNING // Reduce logging level to avoid spam
        logger.useParentHandlers = false

        // Ensure cleanup on JVM shutdown
        Runtime.getRuntime().addShutdownHook(Thread {
            if (GlobalScreen.isNativeHookRegistered()) {
                try {
                    GlobalScreen.unregisterNativeHook()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    fun updateActiveTriggers(
        macros: List<MacroFileState>, 
        eStopKeyName: String = "F12",
        copyConsoleShortcut: String? = null,
        stopKeyShortcut: String? = null,
        inspectKeyShortcut: String? = null
    ) {
        activeTriggers.clear()
        
        eStopKeyCode = KeyParser.parseNativeHookKeys(eStopKeyName).firstOrNull()
        this.copyConsoleShortcut = copyConsoleShortcut
        this.stopKeyShortcut = stopKeyShortcut
        this.inspectKeyShortcut = inspectKeyShortcut
        println("Trigger Listener: E-Stop key set to $eStopKeyName (${eStopKeyCode})")

        macros.filter { it.isActive }.forEach { macroState ->
            try {
                val content = macroState.file?.readText() ?: macroState.content
                if (content.isBlank()) return@forEach

                val triggerJson = JSONObject(content).optJSONObject("trigger")
                if (triggerJson != null) {
                    val keyName = triggerJson.getString("keyName")
                    val allowedClients = triggerJson.optString("allowedClients", "")
                    KeyParser.parseNativeHookKeys(keyName).firstOrNull()?.let { keyCode ->
                        activeTriggers.computeIfAbsent(keyCode) { mutableListOf() }
                            .add(ActiveTrigger(keyCode, macroState, allowedClients))
                        println("Trigger registered: ${NativeKeyEvent.getKeyText(keyCode)} for ${macroState.name}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Check for collisions
        activeTriggers.forEach { (keyCode, triggers) ->
            if (triggers.size > 1) {
                val macroNames = triggers.joinToString(", ") { it.macro.name }
                val keyName = NativeKeyEvent.getKeyText(keyCode)
                val warningMsg = "Warning: Multiple macros bound to '$keyName': $macroNames"
                println(warningMsg)
                viewModel.consoleViewModel.addLog(LogLevel.Warn, warningMsg)
            }
        }

        println("Active triggers updated. Total keys monitored: ${activeTriggers.size}")
    }

    fun startListening() {
        try {
            if (!GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.registerNativeHook()
                GlobalScreen.addNativeKeyListener(this)
                println("Trigger Listener: Started listening for global hotkeys.")
            }
        } catch (e: Exception) {
            System.err.println("There was a problem registering the native hook.")
            e.printStackTrace()
        }
    }

    fun shutdown() {
        if (GlobalScreen.isNativeHookRegistered()) {
            try {
                GlobalScreen.removeNativeKeyListener(this)
                GlobalScreen.unregisterNativeHook()
                println("Trigger Listener: Shutdown complete.")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun nativeKeyReleased(e: NativeKeyEvent) {
        val keyText = NativeKeyEvent.getKeyText(e.keyCode)
        val modifiers = NativeKeyEvent.getModifiersText(e.modifiers)
        val fullShortcut = if (modifiers.isNotEmpty()) "$modifiers+$keyText".replace(" ", "") else keyText

        // Check for E-Stop first
        if (e.keyCode == eStopKeyCode) {
            val msg = "E-STOP ACTIVATED via keyboard shortcut!"
            println("Trigger Listener: $msg")
            viewModel.consoleViewModel.addLog(LogLevel.Error, msg)
            viewModel.stopAllMacros()
            return
        }

        // Check for Copy Console Shortcut
        if (copyConsoleShortcut != null && fullShortcut == copyConsoleShortcut) {
            listenerScope.launch(Dispatchers.Main) {
                val text = viewModel.consoleViewModel.logMessages.value.joinToString("\n") { it.formatted }
                val selection = java.awt.datatransfer.StringSelection(text)
                java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
                viewModel.consoleViewModel.addLog(LogLevel.Info, "Console output copied to clipboard via shortcut ($fullShortcut).")
            }
        }

        // Check for Stop Macro Shortcut
        if (stopKeyShortcut != null && fullShortcut == stopKeyShortcut) {
            println("Trigger Listener: STOP MACRO ACTIVATED!")
            viewModel.macroManagerViewModel.cancelAllMacros()
            viewModel.consoleViewModel.addLog(LogLevel.Warn, "Stopping all running macros via shortcut ($fullShortcut).")
        }

        // Check for Inspect Key Shortcut
        if (inspectKeyShortcut != null && fullShortcut == inspectKeyShortcut) {
            println("Trigger Listener: TOGGLE INSPECTOR!")
            listenerScope.launch(Dispatchers.Main) {
                viewModel.inspectorViewModel.toggleInspector()
            }
        }

        if (viewModel.uiState.value.isMacroExecutionEnabled) {
            val triggers = activeTriggers[e.keyCode]
            if (triggers != null) {
                // Offload the processing to a coroutine to return from the native callback ASAP
                listenerScope.launch {
                    triggers.forEach { trigger ->
                        if (trigger.macro.isActive) {
                            viewModel.consoleViewModel.addLog(LogLevel.Info, "Macro triggered via shortcut: ${trigger.macro.name}")
                            onTrigger(trigger.macro)
                        }
                    }
                }
            }
        }
    }
    
    override fun nativeKeyPressed(e: NativeKeyEvent) {}
    override fun nativeKeyTyped(e: NativeKeyEvent) {}
}