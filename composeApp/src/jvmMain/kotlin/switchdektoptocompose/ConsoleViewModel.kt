package switchdektoptocompose

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConsoleViewModel {
    private val _logMessages = MutableStateFlow<List<String>>(emptyList())
    val logMessages = _logMessages.asStateFlow()

    private val _logLevel = MutableStateFlow(LogLevel.Info)
    val logLevel = _logLevel.asStateFlow()

    private val allLogs = mutableListOf<Pair<LogLevel, String>>()

    fun setLogLevel(level: LogLevel) {
        _logLevel.value = level
        filterLogs()
    }

    fun addLog(level: LogLevel, message: String) {
        allLogs.add(level to message)
        if (level.ordinal >= _logLevel.value.ordinal) {
            _logMessages.value = _logMessages.value + "[$level] $message"
        }
    }

    private fun filterLogs() {
        _logMessages.value = allLogs.filter { (level, _) ->
            level.ordinal >= _logLevel.value.ordinal
        }.map { (level, message) -> "[$level] $message" }
    }
}
