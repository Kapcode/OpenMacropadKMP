package switchdektoptocompose

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

// Now holds the entire macro state, not just the file
data class ActiveTrigger(
    val keyCode: Int,
    val macro: MacroFileState
)

class TriggerListener(
    private val onTrigger: (MacroFileState) -> Unit // Callback now passes the full state
) : NativeKeyListener {

    private val activeTriggers = ConcurrentHashMap<Int, ActiveTrigger>()

    init {
        val logger = Logger.getLogger(GlobalScreen::class.java.getPackage().name)
        logger.level = Level.OFF
        logger.useParentHandlers = false
    }

    fun updateActiveTriggers(macros: List<MacroFileState>) {
        activeTriggers.clear()
        // Filter only for active macros
        macros.filter { it.isActive }.forEach { macroState ->
            try {
                // Get content from the file if it exists, otherwise from the state object
                val content = macroState.file?.readText() ?: macroState.content
                if (content.isBlank()) return@forEach

                val triggerJson = JSONObject(content).optJSONObject("trigger")
                if (triggerJson != null) {
                    val keyName = triggerJson.getString("keyName")
                    KeyParser.parseNativeHookKeys(keyName).firstOrNull()?.let { keyCode ->
                        activeTriggers[keyCode] = ActiveTrigger(keyCode, macroState)
                        println("Trigger registered: ${NativeKeyEvent.getKeyText(keyCode)} for ${macroState.name}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
        try {
            if (GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.removeNativeKeyListener(this)
                GlobalScreen.unregisterNativeHook()
                println("Trigger Listener: Shutdown complete.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun nativeKeyReleased(e: NativeKeyEvent) {
        activeTriggers[e.keyCode]?.let { trigger ->
            println("Trigger Detected: ${NativeKeyEvent.getKeyText(e.keyCode)} for ${trigger.macro.name}")
            onTrigger(trigger.macro) // Pass the full macro state to the callback
        }
    }
    
    override fun nativeKeyPressed(e: NativeKeyEvent) {}
    override fun nativeKeyTyped(e: NativeKeyEvent) {}
}