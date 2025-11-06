import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener
import org.json.JSONObject
import UI.RecordingSettingsDialog
import java.awt.Point
import java.util.concurrent.ConcurrentHashMap

class MacroRecorder(private val settings: RecordingSettingsDialog, private val onStop: (List<JSONObject>) -> Unit) : NativeKeyListener, NativeMouseInputListener {

    private val events = mutableListOf<JSONObject>()
    private val pressedKeys = ConcurrentHashMap<Int, Boolean>()
    private var isRecording = false

    fun start() {
        println("Recorder: Starting...")
        isRecording = true
        GlobalScreen.addNativeKeyListener(this)
        if (settings.recordingLevel != RecordingSettingsDialog.RecordingLevel.KEYS_ONLY) {
            GlobalScreen.addNativeMouseListener(this)
            if (settings.recordingLevel == RecordingSettingsDialog.RecordingLevel.KEYS_MOUSE_BUTTONS_AND_MOVES) {
                GlobalScreen.addNativeMouseMotionListener(this)
            }
        }
    }

    fun stop(stoppedByKey: Boolean = false) {
        if (!isRecording) return
        println("Recorder: Stopping...")
        isRecording = false
        GlobalScreen.removeNativeKeyListener(this)
        GlobalScreen.removeNativeMouseListener(this)
        GlobalScreen.removeNativeMouseMotionListener(this)

        if (stoppedByKey) {
            settings.endOnKeys?.let { stopKeys ->
                val stopKeyCodes = stopKeys.mapNotNull { KeyMap.stringToNativeKeyCodeMap[it.uppercase()] }.toSet()
                val numEventsToRemove = stopKeyCodes.size * 2

                if (events.size >= numEventsToRemove) {
                    val lastEvents = events.takeLast(numEventsToRemove)
                    val lastPresses = lastEvents.filter { it.optString("command") == "PRESS" }
                    val lastPressKeyCodes = lastPresses.mapNotNull { KeyMap.stringToNativeKeyCodeMap[it.optString("key").uppercase()] }.toSet()

                    if (lastPressKeyCodes == stopKeyCodes) {
                        println("Recorder: Stop key sequence detected. Removing from recording.")
                        onStop(events.dropLast(numEventsToRemove))
                        return
                    }
                }
            }
        }
        println("Recorder: No stop key sequence found or matched. Returning all events.")
        onStop(events)
    }

    override fun nativeKeyPressed(e: NativeKeyEvent) {
        if (!isRecording) return
        println("Key Pressed: ${NativeKeyEvent.getKeyText(e.keyCode)} (Code: ${e.keyCode})")
        pressedKeys[e.keyCode] = true
        val event = JSONObject()
        event.put("type", "key")
        event.put("command", "PRESS")
        event.put("key", NativeKeyEvent.getKeyText(e.keyCode))
        events.add(event)
    }

    override fun nativeKeyReleased(e: NativeKeyEvent) {
        if (!isRecording) return
        println("Key Released: ${NativeKeyEvent.getKeyText(e.keyCode)} (Code: ${e.keyCode})")

        val event = JSONObject()
        event.put("type", "key")
        event.put("command", "RELEASE")
        event.put("key", NativeKeyEvent.getKeyText(e.keyCode))
        events.add(event)

        settings.endOnKeys?.let { stopKeys ->
            val stopKeyCodes = stopKeys.mapNotNull { KeyMap.stringToNativeKeyCodeMap[it.uppercase()] }.toSet()
            if (stopKeyCodes.isEmpty()) return@let

            val potentialStopSet = pressedKeys.keys.toSet()
            println("Checking for stop keys. Required: $stopKeyCodes, Currently Pressed: $potentialStopSet, Released: ${e.keyCode}")

            if (stopKeyCodes.contains(e.keyCode) && potentialStopSet == stopKeyCodes) {
                println("Stop condition met!")
                stop(stoppedByKey = true)
                return
            }
        }
        pressedKeys.remove(e.keyCode)
    }

    override fun nativeMousePressed(e: NativeMouseEvent) {
        if (!isRecording || settings.recordingLevel == RecordingSettingsDialog.RecordingLevel.KEYS_ONLY) return
        println("Mouse Pressed")
        val event = JSONObject()
        event.put("type", "mouse")
        event.put("command", "PRESS")
        events.add(event)
    }

    override fun nativeMouseReleased(e: NativeMouseEvent) {
        if (!isRecording || settings.recordingLevel == RecordingSettingsDialog.RecordingLevel.KEYS_ONLY) return
        println("Mouse Released")
        val event = JSONObject()
        event.put("type", "mouse")
        event.put("command", "RELEASE")
        events.add(event)
    }

    override fun nativeMouseMoved(e: NativeMouseEvent) {
        if (!isRecording || settings.recordingLevel != RecordingSettingsDialog.RecordingLevel.KEYS_MOUSE_BUTTONS_AND_MOVES) return
        val event = JSONObject()
        event.put("type", "mouse")
        event.put("command", if (settings.animateMouseMoves) "ANIMATE_TO" else "SNAP_TO")
        event.put("x", e.x)
        event.put("y", e.y)
        events.add(event)
    }

    override fun nativeKeyTyped(e: NativeKeyEvent) {}
    override fun nativeMouseClicked(e: NativeMouseEvent) {}
    override fun nativeMouseDragged(e: NativeMouseEvent) {}
}