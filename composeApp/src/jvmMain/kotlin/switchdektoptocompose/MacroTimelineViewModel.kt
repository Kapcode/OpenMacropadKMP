package switchdektoptocompose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

// ... (Sealed class and enums remain the same) ...
sealed class MacroEventState(val id: String = UUID.randomUUID().toString()) {
    data class KeyEvent(
        val keyName: String,
        val action: KeyAction
    ) : MacroEventState()

    data class MouseEvent(
        val x: Int,
        val y: Int,
        val action: MouseAction
    ) : MacroEventState()

    data class DelayEvent(
        val durationMs: Long
    ) : MacroEventState()
}

enum class KeyAction { PRESS, RELEASE }
enum class MouseAction { MOVE, CLICK }

class MacroTimelineViewModel(
    private val macroEditorViewModel: MacroEditorViewModel
) {

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

    /**
     * Moves an event from one position to another for live reordering.
     */
    fun moveEvent(from: Int, to: Int) {
        _events.value = _events.value.toMutableList().apply {
            add(to, removeAt(from))
        }
    }

    /**
     * Finalizes the reordering. Can be used for persistence later.
     */
    fun onReorderFinished(from: Int, to: Int) {
        println("Moved item from $from to $to")
        // Here you can add logic to save the new order
    }

    fun loadEventsFromJson(jsonContent: String) {
        // ... (this function remains the same) ...
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
            e.printStackTrace()
        }
        _events.value = newEvents
    }
}