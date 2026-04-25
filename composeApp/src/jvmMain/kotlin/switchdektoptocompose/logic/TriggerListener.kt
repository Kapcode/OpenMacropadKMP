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

    private val evaluator = SequenceEvaluator(
        viewModel,
        onTriggerRoutine = { routine -> viewModel.macroManagerViewModel.onTriggerRoutine(routine) },
        onTriggerMacro = { macro -> onTrigger(macro) }
    )
    private var eStopKeyCode: Int? = null
    private var copyConsoleShortcut: String? = null
    private var stopKeyShortcut: String? = null
    private var inspectKeyShortcut: String? = null
    private val listenerScope = CoroutineScope(Dispatchers.Default)

    init {
        val logger = java.util.logging.Logger.getLogger(GlobalScreen::class.java.getPackage().name)
        logger.level = java.util.logging.Level.WARNING // Reduce logging level to avoid spam
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
        inspectKeyShortcut: String? = null,
        routines: List<com.kapcode.open.macropad.kmps.models.AutomationRoutine> = emptyList()
    ) {
        val unifiedTriggers = mutableListOf<UnifiedTrigger>()
        
        eStopKeyCode = KeyParser.parseNativeHookKeys(eStopKeyName).firstOrNull()
        this.copyConsoleShortcut = copyConsoleShortcut
        this.stopKeyShortcut = stopKeyShortcut
        this.inspectKeyShortcut = inspectKeyShortcut
        println("Trigger Listener: E-Stop key set to $eStopKeyName (${eStopKeyCode})")

        // Parse advanced routines
        routines.forEach { routine ->
            val keyCodes = when (val t = routine.trigger) {
                is com.kapcode.open.macropad.kmps.models.AutomationTrigger.KeyHold -> KeyParser.parseNativeHookKeys(t.keyName)
                is com.kapcode.open.macropad.kmps.models.AutomationTrigger.MultiTap -> KeyParser.parseNativeHookKeys(t.keyName)
                is com.kapcode.open.macropad.kmps.models.AutomationTrigger.Sequence -> KeyParser.parseNativeHookKeys(t.keys.joinToString(","))
            }
            
            if (keyCodes.isNotEmpty()) {
                val triggerType = when (routine.trigger) {
                    is com.kapcode.open.macropad.kmps.models.AutomationTrigger.KeyHold -> TriggerType.HOLD
                    is com.kapcode.open.macropad.kmps.models.AutomationTrigger.MultiTap -> TriggerType.MULTI_TAP
                    is com.kapcode.open.macropad.kmps.models.AutomationTrigger.Sequence -> TriggerType.SEQUENCE
                }
                
                unifiedTriggers.add(UnifiedTrigger(
                    id = routine.id,
                    keyCodes = keyCodes,
                    triggerType = triggerType,
                    durationMs = (routine.trigger as? com.kapcode.open.macropad.kmps.models.AutomationTrigger.KeyHold)?.durationMs ?: 0L,
                    tapCount = (routine.trigger as? com.kapcode.open.macropad.kmps.models.AutomationTrigger.MultiTap)?.tapCount ?: 0,
                    windowMs = (routine.trigger as? com.kapcode.open.macropad.kmps.models.AutomationTrigger.MultiTap)?.windowMs 
                        ?: (routine.trigger as? com.kapcode.open.macropad.kmps.models.AutomationTrigger.Sequence)?.windowMs ?: 0L,
                    confirmationRequired = false, // Routines handle confirmation internally if needed
                    routine = routine
                ))
            }
        }

        // Parse standalone macros
        macros.filter { it.isActive }.forEach { macroState ->
            try {
                val content = macroState.file?.readText() ?: macroState.content
                if (content.isBlank()) return@forEach

                val triggerJson = org.json.JSONObject(content).optJSONObject("trigger")
                if (triggerJson != null) {
                    val keyName = triggerJson.getString("keyName")
                    val actionStr = triggerJson.optString("action", "RELEASE")
                    val keyCodes = KeyParser.parseNativeHookKeys(keyName)
                    
                    if (keyCodes.isNotEmpty()) {
                        val triggerType = try { TriggerType.valueOf(actionStr) } catch(e: Exception) { TriggerType.RELEASE }
                        
                        unifiedTriggers.add(UnifiedTrigger(
                            id = macroState.id,
                            keyCodes = keyCodes,
                            triggerType = triggerType,
                            durationMs = triggerJson.optLong("durationMs", 500L),
                            tapCount = triggerJson.optInt("tapCount", 2),
                            windowMs = triggerJson.optLong("windowMs", 300L),
                            confirmationRequired = triggerJson.optBoolean("confirmationRequired", false),
                            macro = macroState
                        ))
                        println("Trigger registered: $keyName ($triggerType) for ${macroState.name}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        evaluator.updateTriggers(unifiedTriggers)
        println("Active triggers updated. Total unified triggers: ${unifiedTriggers.size}")
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

    override fun nativeKeyPressed(e: NativeKeyEvent) {
        evaluator.onKeyPressed(e.keyCode)
        
        // Check for E-Stop first
        if (e.keyCode == eStopKeyCode) {
            val msg = "E-STOP ACTIVATED via keyboard shortcut!"
            println("Trigger Listener: $msg")
            viewModel.consoleViewModel.addLog(LogLevel.Error, msg)
            viewModel.stopAllMacros()
            return
        }
    }

    override fun nativeKeyReleased(e: NativeKeyEvent) {
        evaluator.onKeyReleased(e.keyCode)
        
        val keyText = NativeKeyEvent.getKeyText(e.keyCode)
        val modifiers = NativeKeyEvent.getModifiersText(e.modifiers)
        val fullShortcut = if (modifiers.isNotEmpty()) "$modifiers+$keyText".replace(" ", "") else keyText

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
            // Triggering is now handled entirely by the SequenceEvaluator state machine
        }
    }
    
    override fun nativeKeyTyped(e: NativeKeyEvent) {}
}
