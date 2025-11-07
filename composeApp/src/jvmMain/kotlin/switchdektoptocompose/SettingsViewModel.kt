package switchdektoptocompose

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.prefs.Preferences
import javax.swing.JFileChooser

class SettingsViewModel {

    // Use Java's Preferences API for simple persistent storage on desktop
    private val prefs = Preferences.userNodeForPackage(SettingsViewModel::class.java)
    
    // --- Keys for Preferences ---
    private val macroDirKey = "macro_directory"
    private val themeKey = "ui_theme"

    // --- StateFlows for UI ---
    // Make sure availableThemes is initialized BEFORE it is used by loadTheme()
    val availableThemes = listOf("Dark Blue", "Light Blue")

    private val _macroDirectory = MutableStateFlow(loadMacroDirectory())
    val macroDirectory = _macroDirectory.asStateFlow()

    private val _selectedTheme = MutableStateFlow(loadTheme())
    val selectedTheme = _selectedTheme.asStateFlow()

    // --- Macro Directory Logic ---
    private fun loadMacroDirectory(): String {
        val defaultDir = System.getProperty("user.home") + File.separator + "OpenMacropad"
        return prefs.get(macroDirKey, defaultDir)
    }

    private fun saveMacroDirectory(path: String) {
        prefs.put(macroDirKey, path)
        _macroDirectory.value = path
    }

    fun chooseMacroDirectory() {
        val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Select Macro Directory"
            currentDirectory = File(macroDirectory.value)
        }

        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedDirectory = fileChooser.selectedFile.absolutePath
            saveMacroDirectory(selectedDirectory)
        }
    }

    // --- Theme Logic ---
    private fun loadTheme(): String {
        return prefs.get(themeKey, availableThemes.first())
    }

    fun selectTheme(theme: String) {
        if (theme in availableThemes) {
            prefs.put(themeKey, theme)
            _selectedTheme.value = theme
        }
    }
}