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

    fun setTheme(theme: AppTheme) {
        _theme.value = theme
    }
}
