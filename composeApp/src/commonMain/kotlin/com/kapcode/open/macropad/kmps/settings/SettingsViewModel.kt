package com.kapcode.open.macropad.kmps.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    fun setTheme(theme: AppTheme) {
        _theme.value = theme
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        _analyticsEnabled.value = enabled
    }

    fun setGlobalLoading(isLoading: Boolean) {
        _isGlobalLoading.value = isLoading
    }
}
