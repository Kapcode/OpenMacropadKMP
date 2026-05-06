package switchdektoptocompose.logic

import switchdektoptocompose.model.LogLevel

/**
 * Interface to decouple hardware logic (TriggerListener, SequenceEvaluator) 
 * from the UI layer (DesktopViewModel).
 */
interface TriggerBridge {
    fun addLog(level: LogLevel, msg: String)
    fun stopAllMacros()
    fun cancelAllMacros()
    fun toggleInspector()
    fun showTriggerConfirmation(unified: UnifiedTrigger)
    fun isMacroExecutionEnabled(): Boolean
    fun getConsoleLogs(): String
    fun copyLogsToClipboard()
}
