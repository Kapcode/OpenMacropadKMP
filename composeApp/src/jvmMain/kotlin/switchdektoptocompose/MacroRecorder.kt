package switchdektoptocompose

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MacroRecorder(
    private val options: RecordMacroViewModel,
    private val onRecordingFinished: (String) -> Unit
) : NativeKeyListener, NativeMouseInputListener, NativeMouseWheelListener {

    private val events = mutableListOf<JSONObject>()
    private var lastEventTime = 0L
    private var stopKeyCode: Int? = null

    fun start() {
        if (!GlobalScreen.isNativeHookRegistered()) {
            GlobalScreen.registerNativeHook()
        }
        
        stopKeyCode = KeyParser.parseNativeHookKeys(options.selectedStopKey.value).firstOrNull()
        
        if (options.recordKeys.value) GlobalScreen.addNativeKeyListener(this)
        if (options.recordMouseButtons.value || options.recordMouseMoves.value) GlobalScreen.addNativeMouseListener(this)
        if (options.recordMouseScroll.value) GlobalScreen.addNativeMouseWheelListener(this)

        lastEventTime = System.currentTimeMillis()
        
        if (options.useRecordingDuration.value) {
            val duration = options.recordingDurationMs.value.toLongOrNull() ?: 5000L
            CoroutineScope(Dispatchers.Default).launch {
                delay(duration)
                stop()
            }
        }
    }

    private fun stop() {
        GlobalScreen.removeNativeKeyListener(this)
        GlobalScreen.removeNativeMouseListener(this)
        GlobalScreen.removeNativeMouseWheelListener(this)
        
        // Finalize JSON
        val root = JSONObject()
        root.put("events", JSONArray(events))
        
        onRecordingFinished(root.toString(4))
    }
    
    private fun addDelay() {
        if (options.recordDelays.value) {
            val currentTime = System.currentTimeMillis()
            val delay = currentTime - lastEventTime
            if (delay > 10) { // Threshold to avoid tiny delays
                val delayEvent = JSONObject().apply {
                    put("type", "delay")
                    put("durationMs", delay)
                }
                events.add(delayEvent)
            }
            lastEventTime = currentTime
        }
    }

    // --- Key Listener ---
    override fun nativeKeyPressed(e: NativeKeyEvent) {
        if (e.keyCode == stopKeyCode) {
            stop()
            return
        }
        if (options.recordKeys.value) {
            addDelay()
            val keyEvent = JSONObject().apply {
                put("type", "key")
                put("action", "PRESS")
                put("keyName", NativeKeyEvent.getKeyText(e.keyCode))
            }
            events.add(keyEvent)
        }
    }

    override fun nativeKeyReleased(e: NativeKeyEvent) {
        if (options.recordKeys.value && e.keyCode != stopKeyCode) {
            addDelay()
            val keyEvent = JSONObject().apply {
                put("type", "key")
                put("action", "RELEASE")
                put("keyName", NativeKeyEvent.getKeyText(e.keyCode))
            }
            events.add(keyEvent)
        }
    }

    // --- Mouse Listeners ---
    override fun nativeMousePressed(e: NativeMouseEvent) {
        if (options.recordMouseButtons.value) {
            addDelay()
            val mouseEvent = JSONObject().apply {
                put("type", "mousebutton")
                put("action", "PRESS")
                put("buttonNumber", e.button)
            }
            events.add(mouseEvent)
        }
    }

    override fun nativeMouseReleased(e: NativeMouseEvent) {
        if (options.recordMouseButtons.value) {
            addDelay()
            val mouseEvent = JSONObject().apply {
                put("type", "mousebutton")
                put("action", "RELEASE")
                put("buttonNumber", e.button)
            }
            events.add(mouseEvent)
        }
    }
    
    override fun nativeMouseMoved(e: NativeMouseEvent) {
        if (options.recordMouseMoves.value) {
             addDelay()
             val mouseEvent = JSONObject().apply {
                put("type", "mouse")
                put("action", "MOVE")
                put("x", e.x)
                put("y", e.y)
                put("isAnimated", false) // Animation is a playback option
             }
             events.add(mouseEvent)
        }
    }
    
    override fun nativeMouseWheelMoved(e: NativeMouseWheelEvent) {
        if (options.recordMouseScroll.value) {
            addDelay()
            val scrollEvent = JSONObject().apply {
                put("type", "scroll")
                put("scrollAmount", e.wheelRotation)
            }
            events.add(scrollEvent)
        }
    }
    
    // --- Unused Overrides ---
    override fun nativeKeyTyped(e: NativeKeyEvent) {}
    override fun nativeMouseClicked(e: NativeMouseEvent) {}
    override fun nativeMouseDragged(e: NativeMouseEvent) {}
}