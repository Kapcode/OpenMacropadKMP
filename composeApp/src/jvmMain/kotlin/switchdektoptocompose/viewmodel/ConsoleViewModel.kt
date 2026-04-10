package switchdektoptocompose.viewmodel

import switchdektoptocompose.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong

class ConsoleViewModel {
    data class LogEntry(
        val id: Long,
        val level: LogLevel,
        val message: String,
        val timestamp: String,
        val formatted: String
    )

    private val _logMessages = MutableStateFlow<List<LogEntry>>(emptyList())
    val logMessages = _logMessages.asStateFlow()

    private val _selectionStartId = MutableStateFlow<Long?>(null)
    val selectionStartId = _selectionStartId.asStateFlow()

    private val _selectionEndId = MutableStateFlow<Long?>(null)
    val selectionEndId = _selectionEndId.asStateFlow()

    fun updateSelection(id: Long, isShiftPressed: Boolean) {
        if (isShiftPressed && _selectionStartId.value != null) {
            _selectionEndId.value = id
        } else {
            _selectionStartId.value = id
            _selectionEndId.value = id
        }
    }

    fun extendSelectionToId(id: Long) {
        if (_selectionStartId.value != null) {
            _selectionEndId.value = id
        }
    }

    fun clearSelection() {
        _selectionStartId.value = null
        _selectionEndId.value = null
    }

    fun getSelectedText(): String {
        val startId = _selectionStartId.value ?: return ""
        val endId = _selectionEndId.value ?: return ""

        val messages = _logMessages.value
        val startIndex = messages.indexOfFirst { it.id == startId }
        val endIndex = messages.indexOfFirst { it.id == endId }

        if (startIndex == -1 || endIndex == -1) return ""

        val range = if (startIndex <= endIndex) startIndex..endIndex else endIndex..startIndex
        return messages.slice(range).joinToString("\n") { it.formatted }
    }

    private val _logLevel = MutableStateFlow(LogLevel.Info)
    val logLevel = _logLevel.asStateFlow()

    private val _isAutoScrollEnabled = MutableStateFlow(true)
    val isAutoScrollEnabled = _isAutoScrollEnabled.asStateFlow()

    private val _isLoggingToFile = MutableStateFlow(false)
    val isLoggingToFile = _isLoggingToFile.asStateFlow()

    private val _showLoggingWarning = MutableStateFlow(false)
    val showLoggingWarning = _showLoggingWarning.asStateFlow()

    private val allLogs = mutableListOf<LogEntry>()
    private val timestampFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    private var logFile: File? = null
    private val idGenerator = AtomicLong(0)

    fun setLogLevel(level: LogLevel) {
        _logLevel.value = level
        filterLogs()
    }

    fun setAutoScroll(enabled: Boolean) {
        _isAutoScrollEnabled.value = enabled
    }

    fun toggleLoggingToFile() {
        if (!_isLoggingToFile.value) {
            _showLoggingWarning.value = true
        } else {
            stopLoggingToFile()
        }
    }

    fun confirmLoggingToFile() {
        _showLoggingWarning.value = false
        startLoggingToFile()
    }

    fun dismissLoggingWarning() {
        _showLoggingWarning.value = false
    }

    private fun startLoggingToFile() {
        val fileName = "macropad_log_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.txt"
        logFile = File(fileName)
        _isLoggingToFile.value = true
        addLog(LogLevel.Info, "Started logging to file: ${logFile?.absolutePath}")
    }

    private fun stopLoggingToFile() {
        addLog(LogLevel.Info, "Stopped logging to file.")
        _isLoggingToFile.value = false
        logFile = null
    }

    fun addLog(level: LogLevel, message: String) {
        val timestamp = LocalDateTime.now().format(timestampFormatter)
        val formattedLog = "[$timestamp] [$level] $message"
        val entry = LogEntry(
            id = idGenerator.incrementAndGet(),
            level = level,
            message = message,
            timestamp = timestamp,
            formatted = formattedLog
        )
        
        allLogs.add(entry)
        
        if (level.ordinal >= _logLevel.value.ordinal) {
            _logMessages.value = _logMessages.value + entry
        }

        if (_isLoggingToFile.value) {
            logFile?.appendText("$formattedLog\n")
        }
    }

    private fun filterLogs() {
        _logMessages.value = allLogs.filter { it.level.ordinal >= _logLevel.value.ordinal }
    }
}
