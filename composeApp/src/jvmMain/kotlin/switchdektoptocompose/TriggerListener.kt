package switchdektoptocompose

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

data class ActiveTrigger(
    val keyCode: Int,
    val file: File
)

class TriggerListener(
    private val onTrigger: (File) -> Unit
) : NativeKeyListener {

    private val activeTriggers = ConcurrentHashMap<Int, ActiveTrigger>()

    init {
        val logger = Logger.getLogger(GlobalScreen::class.java.getPackage().name)
        logger.level = Level.OFF
        logger.useParentHandlers = false
    }

    fun updateActiveTriggers(macros: List<MacroFileState>) {
        activeTriggers.clear()
        // Only process macros that are active AND have a physical file
        macros.filter { it.isActive && it.file != null }.forEach { macroState ->
            val macroFile = macroState.file!! // We know it's not null here because of the filter
            try {
                val content = macroFile.readText()
                val triggerJson = JSONObject(content).optJSONObject("trigger")
                if (triggerJson != null) {
                    val keyName = triggerJson.getString("keyName")
                    KeyParser.parseNativeHookKeys(keyName).firstOrNull()?.let { keyCode ->
                        activeTriggers[keyCode] = ActiveTrigger(keyCode, macroFile)
                        println("Trigger registered: ${NativeKeyEvent.getKeyText(keyCode)} for ${macroFile.name}")
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
            println("Trigger Detected: ${NativeKeyEvent.getKeyText(e.keyCode)} for ${trigger.file.name}")
            onTrigger(trigger.file)
        }
    }
    
    override fun nativeKeyPressed(e: NativeKeyEvent) {}
    override fun nativeKeyTyped(e: NativeKeyEvent) {}
}