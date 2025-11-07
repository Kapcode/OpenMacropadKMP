package switchdektoptocompose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed class MacroEventState(val id: String = UUID.randomUUID().toString()) {
    data class KeyEvent(val keyName: String, val action: KeyAction) : MacroEventState()
    data class MouseEvent(val x: Int, val y: Int, val action: MouseAction) : MacroEventState()
    data class DelayEvent(val durationMs: Long) : MacroEventState()
}

enum class KeyAction { PRESS, RELEASE }
enum class MouseAction { MOVE, CLICK }

class MacroTimelineViewModel(private val macroEditorViewModel: MacroEditorViewModel) {

    private val _events = MutableStateFlow<List<MacroEventState>>(emptyList())
    val events = _events.asStateFlow()

    private val viewModelScope = CoroutineScope(Dispatchers.Default)

    init {
        viewModelScope.launch {
            macroEditorViewModel.tabs.collectLatest { tabs ->
                val selectedIndex = macroEditorViewModel.selectedTabIndex.value
                val currentTab = tabs.getOrNull(selectedIndex)
                if (currentTab != null) {
                    loadEventsFromJson(currentTab.content)
                } else {
                    _events.value = emptyList()
                }
            }
        }
    }
    
    fun addEvents(newEvents: List<MacroEventState>, isTrigger: Boolean) {
        if (isTrigger && newEvents.isNotEmpty()) {
            val currentEvents = _events.value.toMutableList()
            // A simple heuristic to find if a trigger (ON-RELEASE key event) is already present
            val triggerIndex = currentEvents.indexOfFirst { it is MacroEventState.KeyEvent && it.action == KeyAction.RELEASE }
            if (triggerIndex == 0) { // Check if it's at the beginning
                currentEvents[0] = newEvents.first()
            } else {
                currentEvents.add(0, newEvents.first())
            }
            _events.value = currentEvents
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
        val finalJson = JSONObject().put("events", eventsJsonArray)
        macroEditorViewModel.updateSelectedTabContent(finalJson.toString(4))
    }

    fun loadEventsFromJson(jsonContent: String) {
        val newEvents = mutableListOf<MacroEventState>()
        try {
            val json = JSONObject(jsonContent)
            val eventsArray = json.getJSONArray("events")
            for (i in 0 until eventsArray.length()) {
                val eventObj = eventsArray.getJSONObject(i)
                when (eventObj.getString("type").lowercase()) {
                    "key" -> {
                        newEvents.add(MacroEventState.KeyEvent(
                            keyName = eventObj.getString("keyName"),
                            action = KeyAction.valueOf(eventObj.getString("action").uppercase())
                        ))
                    }
                    "mouse" -> {
                        newEvents.add(MacroEventState.MouseEvent(
                            x = eventObj.optInt("x", 0),
                            y = eventObj.optInt("y", 0),
                            action = MouseAction.valueOf(eventObj.getString("action").uppercase())
                        ))
                    }
                    "delay" -> {
                        newEvents.add(MacroEventState.DelayEvent(
                            durationMs = eventObj.getLong("durationMs")
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            newEvents.clear()
        }
        _events.value = newEvents
    }
}