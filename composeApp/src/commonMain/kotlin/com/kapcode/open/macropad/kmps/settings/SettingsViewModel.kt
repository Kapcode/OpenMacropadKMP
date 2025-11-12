package com.kapcode.open.macropad.kmps.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// A simple enum to represent the available themes in a type-safe way.
enum class AppTheme {
    LightBlue,
    DarkBlue
}

/**
 * A ViewModel for handling application settings.
 *
 * This can be shared between Android and Desktop to provide consistent settings logic.
 */
class SettingsViewModel {

    private val _theme = MutableStateFlow(AppTheme.DarkBlue) // Default to Dark Blue
    val theme = _theme.asStateFlow()

    fun setTheme(theme: AppTheme) {
        _theme.value = theme
    }
}
