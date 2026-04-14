package com.kapcode.open.macropad.kmps.settings

import com.kapcode.open.macropad.kmps.models.TrustedServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AppTheme {
    LightBlue,
    DarkBlue
}

/**
 * A shared ViewModel for handling application settings across both
 * Android and Desktop platforms.
 */
class SettingsViewModel {

    private val _theme = MutableStateFlow(AppTheme.DarkBlue) // Default to Dark Blue
    val theme = _theme.asStateFlow()

    private val _analyticsEnabled = MutableStateFlow(false) // Default to Opt-in
    val analyticsEnabled = _analyticsEnabled.asStateFlow()

    private val _isGlobalLoading = MutableStateFlow(false)
    val isGlobalLoading = _isGlobalLoading.asStateFlow()

    private val _multiQrEnabled = MutableStateFlow(false)
    val multiQrEnabled = _multiQrEnabled.asStateFlow()

    private val _slamFireEnabled = MutableStateFlow(true)
    val slamFireEnabled = _slamFireEnabled.asStateFlow()

    private val _slamFireTrigger = MutableStateFlow(SlamFireTrigger.VolumeDown)
    val slamFireTrigger = _slamFireTrigger.asStateFlow()

    private val _slamFireSelectedMacro = MutableStateFlow<String?>(null)
    val slamFireSelectedMacro = _slamFireSelectedMacro.asStateFlow()

    private val _slamFireDoubleSelectedMacro = MutableStateFlow<String?>(null)
    val slamFireDoubleSelectedMacro = _slamFireDoubleSelectedMacro.asStateFlow()

    private val _slamFireDoubleThreshold = MutableStateFlow(300L) // Default 300ms
    val slamFireDoubleThreshold = _slamFireDoubleThreshold.asStateFlow()

    private val _scannerTimeoutHours = MutableStateFlow(2) // Default 2 hours
    val scannerTimeoutHours = _scannerTimeoutHours.asStateFlow()

    private val _serverHistory = MutableStateFlow<List<TrustedServer>>(emptyList())
    val serverHistory = _serverHistory.asStateFlow()

    fun setTheme(theme: AppTheme) {
        _theme.value = theme
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        _analyticsEnabled.value = enabled
    }

    fun setGlobalLoading(isLoading: Boolean) {
        _isGlobalLoading.value = isLoading
    }

    fun setMultiQrEnabled(enabled: Boolean) {
        _multiQrEnabled.value = enabled
    }

    fun setSlamFireEnabled(enabled: Boolean) {
        _slamFireEnabled.value = enabled
    }

    fun setSlamFireTrigger(trigger: SlamFireTrigger) {
        _slamFireTrigger.value = trigger
    }

    fun setSlamFireSelectedMacro(macro: String?) {
        _slamFireSelectedMacro.value = macro
    }

    fun setSlamFireDoubleSelectedMacro(macro: String?) {
        _slamFireDoubleSelectedMacro.value = macro
    }

    fun setSlamFireDoubleThreshold(threshold: Long) {
        _slamFireDoubleThreshold.value = threshold
    }

    fun setScannerTimeoutHours(hours: Int) {
        _scannerTimeoutHours.value = hours
    }

    fun setServerHistory(history: List<TrustedServer>) {
        _serverHistory.value = history.sortedByDescending { it.lastConnectedTimestamp }
    }

    fun updateServerHistory(server: TrustedServer) {
        _serverHistory.update { history ->
            val newList = history.filterNot { it.serverId == server.serverId }.toMutableList()
            newList.add(0, server)
            newList.sortedByDescending { it.lastConnectedTimestamp }.take(10)
        }
    }
}

enum class SlamFireTrigger {
    VolumeDown,
    VolumeUp,
    Power,
    Bixby,
    Assistant,
    ProximityCovered,
    ProximityUncovered
}
