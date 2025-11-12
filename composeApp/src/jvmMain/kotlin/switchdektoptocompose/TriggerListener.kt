package switchdektoptocompose

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.dispatcher.SwingDispatchService
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.SwingUtilities

data class ActiveTrigger(
    val keyCode: Int,
    val macro: MacroFileState,
    val allowedClients: String
)

class TriggerListener(
    private val viewModel: DesktopViewModel,
    private val onTrigger: (MacroFileState) -> Unit
) : NativeKeyListener {

    private val activeTriggers = ConcurrentHashMap<Int, ActiveTrigger>()

    init {
        // Set the event dispatcher to the Swing dispatch service
        GlobalScreen.setEventDispatcher(SwingDispatchService())

        val logger = Logger.getLogger(GlobalScreen::class.java.getPackage().name)
        logger.level = Level.INFO
        logger.useParentHandlers = false
    }

    fun updateActiveTriggers(macros: List<MacroFileState>) {
        activeTriggers.clear()
        macros.filter { it.isActive }.forEach { macroState ->
            try {
                val content = macroState.file?.readText() ?: macroState.content
                if (content.isBlank()) return@forEach

                val triggerJson = JSONObject(content).optJSONObject("trigger")
                if (triggerJson != null) {
                    val keyName = triggerJson.getString("keyName")
                    val allowedClients = triggerJson.optString("allowedClients", "")
                    KeyParser.parseNativeHookKeys(keyName).firstOrNull()?.let { keyCode ->
                        activeTriggers[keyCode] = ActiveTrigger(keyCode, macroState, allowedClients)
                        println("Trigger registered: ${NativeKeyEvent.getKeyText(keyCode)} for ${macroState.name}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        println("Active triggers updated. Total: ${activeTriggers.size}")
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
        // Use invokeLater to ensure shutdown is non-blocking and on the correct thread.
        if (GlobalScreen.isNativeHookRegistered()) {
            SwingUtilities.invokeLater {
                try {
                    GlobalScreen.removeNativeKeyListener(this)
                    GlobalScreen.unregisterNativeHook()
                    println("Trigger Listener: Shutdown complete.")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun nativeKeyReleased(e: NativeKeyEvent) {
        if (viewModel.isMacroExecutionEnabled.value) {
            activeTriggers[e.keyCode]?.let { trigger ->
                println("Trigger key detected: ${NativeKeyEvent.getKeyText(e.keyCode)}")
                if (trigger.macro.isActive) {
                    println("   - Firing trigger for active macro: ${trigger.macro.name}")
                    // TODO: Add client filtering logic using trigger.allowedClients
                    onTrigger(trigger.macro)
                } else {
                    println("   - Ignoring trigger for inactive macro: ${trigger.macro.name}")
                }
            }
        }
    }
    
    override fun nativeKeyPressed(e: NativeKeyEvent) {}
    override fun nativeKeyTyped(e: NativeKeyEvent) {}
}