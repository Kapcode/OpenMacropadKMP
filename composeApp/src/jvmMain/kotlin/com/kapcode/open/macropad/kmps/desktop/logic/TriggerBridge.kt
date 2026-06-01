package com.kapcode.open.macropad.kmps.desktop.logic

import com.kapcode.open.macropad.kmps.desktop.model.LogLevel

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
