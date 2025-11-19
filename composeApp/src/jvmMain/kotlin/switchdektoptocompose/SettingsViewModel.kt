package switchdektoptocompose

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.swing.JFileChooser

class SettingsViewModel {
    // --- StateFlows for UI ---
    private val _macroDirectory = MutableStateFlow(AppSettings.macroDirectory)
    val macroDirectory = _macroDirectory.asStateFlow()

    private val _serverPort = MutableStateFlow(AppSettings.serverPort)
    val serverPort = _serverPort.asStateFlow()

    private val _secureServerPort = MutableStateFlow(AppSettings.secureServerPort)
    val secureServerPort = _secureServerPort.asStateFlow()

    private val _eStopKey = MutableStateFlow(AppSettings.eStopKey)
    val eStopKey = _eStopKey.asStateFlow()
    
    private val _minimizeToTray = MutableStateFlow(AppSettings.minimizeToTray)
    val minimizeToTray = _minimizeToTray.asStateFlow()

    // For now, we'll keep theme settings separate as they are specific to the Compose UI.
    // In the future, this could also be moved to the properties file if desired.
    private val _selectedTheme = MutableStateFlow("Dark Blue") // Default value
    val selectedTheme = _selectedTheme.asStateFlow() // Corrected the typo from _selected_theme
    val availableThemes = listOf("Dark Blue", "Light Blue")

    fun chooseMacroDirectory() {
        val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Select Macro Directory"
            currentDirectory = File(macroDirectory.value)
        }

        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedDirectory = fileChooser.selectedFile.absolutePath
            // Save the setting to the properties file via switchdektoptocompose.AppSettings
            AppSettings.macroDirectory = selectedDirectory
            // Update the UI by updating the StateFlow
            _macroDirectory.value = selectedDirectory
        }
    }

    fun onServerPortChange(port: String) {
        port.toIntOrNull()?.let {
            _serverPort.value = it
            AppSettings.serverPort = it
        }
    }

    fun onSecureServerPortChange(port: String) {
        port.toIntOrNull()?.let {
            _secureServerPort.value = it
            AppSettings.secureServerPort = it
        }
    }

    fun setEStopKey(key: String) {
        _eStopKey.value = key
        AppSettings.eStopKey = key
    }
    
    fun setMinimizeToTray(enabled: Boolean) {
        _minimizeToTray.value = enabled
        AppSettings.minimizeToTray = enabled
    }

    fun selectTheme(theme: String) {
        if (theme in availableThemes) {
            _selectedTheme.value = theme
        }
    }
}