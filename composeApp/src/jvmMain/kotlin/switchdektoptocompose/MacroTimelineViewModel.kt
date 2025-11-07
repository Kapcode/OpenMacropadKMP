package switchdektoptocompose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

sealed class MacroEventState(val id: String = UUID.randomUUID().toString()) {
    data class KeyEvent(val keyName: String, val action: KeyAction) : MacroEventState()
    data class MouseEvent(val x: Int, val y: Int, val action: MouseAction) : MacroEventState()
    data class DelayEvent(val durationMs: Long) : MacroEventState()
}

enum class KeyAction { PRESS, RELEASE }
enum class MouseAction { MOVE, CLICK }

class MacroTimelineViewModel(private val macroEditorViewModel: MacroEditorViewModel) {

    private val _triggerEvent = MutableStateFlow<MacroEventState.KeyEvent?>(null)
    val triggerEvent = _triggerEvent.asStateFlow()

    private val _events = MutableStateFlow<List<MacroEventState>>(emptyList())
    val events = _events.asStateFlow()

    private val viewModelScope = CoroutineScope(Dispatchers.Default)

    init {
        viewModelScope.launch {
            macroEditorViewModel.tabs.collectLatest { tabs ->
                val selectedIndex = macroEditorViewModel.selectedTabIndex.value
                tabs.getOrNull(selectedIndex)?.let { loadEventsFromJson(it.content) }
            }
        }
    }
    
    fun addEvents(newEvents: List<MacroEventState>, isTrigger: Boolean) {
        if (isTrigger && newEvents.isNotEmpty() && newEvents.first() is MacroEventState.KeyEvent) {
            _triggerEvent.value = newEvents.first() as MacroEventState.KeyEvent
        } else {
            _events.update { it + newEvents }
        }
        updateEditorText()
    }

    fun moveEvent(from: Int, to: Int) {
        _events.value = _events.value.toMutableList().apply { add(to, removeAt(from)) }
        updateEditorText()
    }

    fun onReorderFinished(from: Int, to: Int) {
        println("Moved item from $from to $to")
        updateEditorText()
    }
    
    private fun updateEditorText() {
        val rootJson = JSONObject()
        
        _triggerEvent.value?.let { trigger ->
            val triggerJson = JSONObject()
            triggerJson.put("type", "key")
            triggerJson.put("action", trigger.action.name)
            triggerJson.put("keyName", trigger.keyName)
            rootJson.put("trigger", triggerJson)
        }

        val eventsJsonArray = JSONArray()
        _events.value.forEach { event ->
            val eventJson = JSONObject()
            when (event) {
                is MacroEventState.KeyEvent -> {
                    eventJson.put("type", "key")
                    eventJson.put("action", event.action.name)
                    eventJson.put("keyName", event.keyName)
                }
                is MacroEventState.MouseEvent -> {
                    eventJson.put("type", "mouse")
                    eventJson.put("action", event.action.name)
                    eventJson.put("x", event.x)
                    eventJson.put("y", event.y)
                }
                is MacroEventState.DelayEvent -> {
                    eventJson.put("type", "delay")
                    eventJson.put("durationMs", event.durationMs)
                }
            }
            eventsJsonArray.put(eventJson)
        }
        rootJson.put("events", eventsJsonArray)
        
        macroEditorViewModel.updateSelectedTabContent(rootJson.toString(4))
    }

    fun loadEventsFromJson(jsonContent: String) {
        try {
            val json = JSONObject(jsonContent)
            
            _triggerEvent.value = json.optJSONObject("trigger")?.let {
                MacroEventState.KeyEvent(
                    keyName = it.getString("keyName"),
                    action = KeyAction.valueOf(it.getString("action").uppercase())
                )
            }
            
            val newEvents = mutableListOf<MacroEventState>()
            json.optJSONArray("events")?.let { eventsArray ->
                for (i in 0 until eventsArray.length()) {
                    eventsArray.getJSONObject(i)?.let { eventObj ->
                        when (eventObj.getString("type").lowercase()) {
                            "key" -> newEvents.add(MacroEventState.KeyEvent(eventObj.getString("keyName"), KeyAction.valueOf(eventObj.getString("action").uppercase())))
                            "mouse" -> newEvents.add(MacroEventState.MouseEvent(eventObj.optInt("x", 0), eventObj.optInt("y", 0), MouseAction.valueOf(eventObj.getString("action").uppercase())))
                            "delay" -> newEvents.add(MacroEventState.DelayEvent(eventObj.getLong("durationMs")))
                        }
                    }
                }
            }
            _events.value = newEvents
        } catch (e: Exception) {
            _triggerEvent.value = null
            _events.value = emptyList()
        }
    }
}