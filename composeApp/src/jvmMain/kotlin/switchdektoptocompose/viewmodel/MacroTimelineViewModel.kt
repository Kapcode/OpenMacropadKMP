package switchdektoptocompose.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import switchdektoptocompose.model.*
import java.util.*

data class TimelineUiState(
    val triggerEvent: TriggerState? = null,
    val events: List<MacroEventState> = emptyList()
)

class MacroTimelineViewModel(private val macroEditorViewModel: MacroEditorViewModel) {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState = _uiState.asStateFlow()

    val triggerEvent = _uiState.map { it.triggerEvent }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, null)

    val events = _uiState.map { it.events }
        .stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.Eagerly, emptyList())

    private val viewModelScope = CoroutineScope(Dispatchers.Default)

    init {
        viewModelScope.launch {
            macroEditorViewModel.tabs.collectLatest { tabs ->
                tabs.getOrNull(macroEditorViewModel.selectedTabIndex.value)?.let {
                    loadEventsFromJson(it.content)
                }
            }
        }
    }
    
    fun addOrUpdateTrigger(
        keyName: String, 
        allowedClients: String,
        triggerType: TriggerType = TriggerType.RELEASE,
        holdDurationMs: Long = 500,
        multiTapCount: Int = 2,
        tapWindowMs: Long = 300,
        sequenceWindowMs: Long = 1000,
        confirmationRequired: Boolean = false
    ) {
        _uiState.update { it.copy(triggerEvent = TriggerState(
            keyName, 
            allowedClients,
            triggerType,
            holdDurationMs,
            multiTapCount,
            tapWindowMs,
            sequenceWindowMs,
            confirmationRequired
        )) }
        updateEditorText()
    }

    fun deleteTrigger() {
        _uiState.update { it.copy(triggerEvent = null) }
        updateEditorText()
    }

    fun updateEvent(index: Int, newEvent: MacroEventState) {
        _uiState.update { state ->
            val newList = state.events.toMutableList()
            if (index in newList.indices) {
                newList[index] = newEvent
            }
            state.copy(events = newList)
        }
        updateEditorText()
    }

    fun addEvents(newEvents: List<MacroEventState>) {
        _uiState.update { state ->
            val currentEvents = state.events
            val autoWaitEvents = newEvents.filterIsInstance<MacroEventState.SetAutoWaitEvent>()
            val otherEvents = newEvents.filterNot { it is MacroEventState.SetAutoWaitEvent }
            
            // Prepend auto-wait events, append the rest
            state.copy(events = autoWaitEvents + currentEvents + otherEvents)
        }
        updateEditorText()
    }

    fun moveEvent(from: Int, to: Int) {
        _uiState.update { state ->
            state.copy(events = state.events.toMutableList().apply { add(to, removeAt(from)) })
        }
        updateEditorText()
    }

    fun deleteEvent(index: Int) {
        if (index in _uiState.value.events.indices) {
            _uiState.update { state ->
                state.copy(events = state.events.toMutableList().apply { removeAt(index) })
            }
            updateEditorText()
        }
    }

    fun onReorderFinished(from: Int, to: Int) {
        updateEditorText()
    }
    
    private fun updateEditorText() {
        val rootJson = JSONObject()
        val state = _uiState.value
        
        state.triggerEvent?.let { trigger ->
            val triggerJson = JSONObject()
            triggerJson.put("type", "key")
            triggerJson.put("action", trigger.triggerType.name)
            triggerJson.put("keyName", trigger.keyName)
            triggerJson.put("allowedClients", trigger.allowedClients)
            triggerJson.put("confirmationRequired", trigger.confirmationRequired)
            
            when (trigger.triggerType) {
                TriggerType.HOLD -> triggerJson.put("durationMs", trigger.holdDurationMs)
                TriggerType.MULTI_TAP -> {
                    triggerJson.put("tapCount", trigger.multiTapCount)
                    triggerJson.put("windowMs", trigger.tapWindowMs)
                }
                TriggerType.SEQUENCE -> triggerJson.put("windowMs", trigger.sequenceWindowMs)
                else -> {}
            }
            
            rootJson.put("trigger", triggerJson)
        }

        val eventsJsonArray = JSONArray()
        state.events.forEach { event ->
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
                    eventJson.put("isAnimated", event.isAnimated)
                }
                is MacroEventState.MouseButtonEvent -> {
                    eventJson.put("type", "mousebutton")
                    eventJson.put("action", event.action.name)
                    eventJson.put("buttonNumber", event.buttonNumber)
                }
                is MacroEventState.ScrollEvent -> {
                    eventJson.put("type", "scroll")
                    eventJson.put("scrollAmount", if (event.scrollAmount > 0) "+${event.scrollAmount}" else "${event.scrollAmount}")
                }
                is MacroEventState.DelayEvent -> {
                    eventJson.put("type", "delay")
                    eventJson.put("durationMs", event.durationMs)
                }
                is MacroEventState.SetAutoWaitEvent -> {
                    eventJson.put("type", "set_auto_wait")
                    eventJson.put("value", event.delayMs)
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
            
            val trigger = json.optJSONObject("trigger")?.let {
                TriggerState(
                    keyName = it.optString("keyName", "ESCAPE"),
                    allowedClients = it.optString("allowedClients", ""),
                    triggerType = try { TriggerType.valueOf(it.optString("action", "RELEASE")) } catch(e: Exception) { TriggerType.RELEASE },
                    holdDurationMs = it.optLong("durationMs", 500),
                    multiTapCount = it.optInt("tapCount", 2),
                    tapWindowMs = it.optLong("windowMs", 300),
                    sequenceWindowMs = it.optLong("windowMs", 1000), // Note: sequence uses same key windowMs if saved that way
                    confirmationRequired = it.optBoolean("confirmationRequired", false)
                )
            }
            
            val newEvents = mutableListOf<MacroEventState>()
            json.optJSONArray("events")?.let { eventsArray ->
                for (i in 0 until eventsArray.length()) {
                    eventsArray.getJSONObject(i)?.let { eventObj ->
                        when (eventObj.getString("type").lowercase()) {
                            "key" -> newEvents.add(MacroEventState.KeyEvent(eventObj.getString("keyName"), KeyAction.valueOf(eventObj.getString("action").uppercase())))
                            "mouse" -> newEvents.add(MacroEventState.MouseEvent(
                                eventObj.optInt("x", 0), 
                                eventObj.optInt("y", 0), 
                                MouseAction.valueOf(eventObj.getString("action").uppercase()),
                                eventObj.optBoolean("isAnimated", false)
                            ))
                            "mousebutton" -> newEvents.add(MacroEventState.MouseButtonEvent(eventObj.getInt("buttonNumber"), KeyAction.valueOf(eventObj.getString("action").uppercase())))
                            "scroll" -> newEvents.add(MacroEventState.ScrollEvent(eventObj.getString("scrollAmount").replace("+", "").toInt()))
                            "delay" -> newEvents.add(MacroEventState.DelayEvent(eventObj.getLong("durationMs")))
                            "set_auto_wait" -> newEvents.add(MacroEventState.SetAutoWaitEvent(eventObj.getInt("value")))
                        }
                    }
                }
            }
            _uiState.update { it.copy(triggerEvent = trigger, events = newEvents) }
        } catch (e: Exception) {
            _uiState.update { it.copy(triggerEvent = null, events = emptyList()) }
        }
    }
}
