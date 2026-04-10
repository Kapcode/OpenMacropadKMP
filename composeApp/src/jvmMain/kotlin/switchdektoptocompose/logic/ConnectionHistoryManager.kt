package switchdektoptocompose.logic

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList

object ConnectionHistoryManager {
    private val workingDir = File(System.getProperty("user.home"), ".openmacropad")
    private val historyFile = File(workingDir, "connection_history.json")
    private val maxEntries = 100

    data class ConnectionEvent(
        val timestamp: String,
        val clientId: String,
        val clientName: String,
        val action: String,
        val metadata: String? = null,
        val reason: String? = null
    ) {
        fun toJSONObject(): JSONObject {
            return JSONObject().apply {
                put("timestamp", timestamp)
                put("clientId", clientId)
                put("clientName", clientName)
                put("action", action)
                metadata?.let { put("metadata", it) }
                reason?.let { put("reason", it) }
            }
        }

        companion object {
            fun fromJSONObject(obj: JSONObject): ConnectionEvent {
                return ConnectionEvent(
                    timestamp = obj.getString("timestamp"),
                    clientId = obj.getString("clientId"),
                    clientName = obj.getString("clientName"),
                    action = obj.getString("action"),
                    metadata = if (obj.has("metadata")) obj.getString("metadata") else null,
                    reason = if (obj.has("reason")) obj.getString("reason") else null
                )
            }
        }
    }

    private val events = CopyOnWriteArrayList<ConnectionEvent>()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    init {
        load()
    }

    private fun load() {
        if (!historyFile.exists()) return
        try {
            val content = historyFile.readText()
            val array = JSONArray(content)
            events.clear()
            for (i in 0 until array.length()) {
                events.add(ConnectionEvent.fromJSONObject(array.getJSONObject(i)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun save() {
        try {
            if (!workingDir.exists()) workingDir.mkdirs()
            val array = JSONArray()
            events.forEach { array.put(it.toJSONObject()) }
            historyFile.writeText(array.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun logEvent(clientId: String, clientName: String, action: String, metadata: String? = null, reason: String? = null) {
        val event = ConnectionEvent(
            timestamp = LocalDateTime.now().format(formatter),
            clientId = clientId,
            clientName = clientName,
            action = action,
            metadata = metadata,
            reason = reason
        )
        events.add(0, event)
        while (events.size > maxEntries) {
            events.removeAt(events.size - 1)
        }
        save()
    }

    fun getHistory(): List<ConnectionEvent> = events.toList()

    fun clearHistory() {
        events.clear()
        save()
    }
}
