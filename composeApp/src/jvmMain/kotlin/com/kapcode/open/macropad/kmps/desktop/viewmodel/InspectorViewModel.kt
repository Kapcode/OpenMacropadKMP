package com.kapcode.open.macropad.kmps.desktop.viewmodel

import com.kapcode.open.macropad.kmps.desktop.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.kapcode.open.macropad.kmps.desktop.logic.ProcessWatcher
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class InspectorViewModel(
    private val consoleViewModel: ConsoleViewModel,
    private val processWatcher: ProcessWatcher
) {
    private val _selectedFKey = MutableStateFlow("F1")
    val selectedFKey = _selectedFKey.asStateFlow()
    
    val focusHistory = processWatcher.focusHistory

    private val _screenshotOnPress = MutableStateFlow(false)
    val screenshotOnPress = _screenshotOnPress.asStateFlow()

    private val _topLeftX = MutableStateFlow("")
    val topLeftX = _topLeftX.asStateFlow()

    private val _topLeftY = MutableStateFlow("")
    val topLeftY = _topLeftY.asStateFlow()

    private val _bottomRightX = MutableStateFlow("")
    val bottomRightX = _bottomRightX.asStateFlow()

    private val _bottomRightY = MutableStateFlow("")
    val bottomRightY = _bottomRightY.asStateFlow()

    private val _maxScreenshots = MutableStateFlow("10")
    val maxScreenshots = _maxScreenshots.asStateFlow()

    private val _screenshotCount = MutableStateFlow(0)
    val screenshotCount = _screenshotCount.asStateFlow()

    fun onMaxScreenshotsChanged(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _maxScreenshots.value = value
        }
    }

    fun incrementScreenshotCount() {
        _screenshotCount.value += 1
    }

    fun resetScreenshotCount() {
        _screenshotCount.value = 0
    }

    fun canTakeScreenshot(): Boolean {
        val max = _maxScreenshots.value.toIntOrNull() ?: return true
        return _screenshotCount.value < max
    }

    fun onFKeySelected(key: String) {
        _selectedFKey.value = key
    }

    fun onScreenshotToggled(enabled: Boolean) {
        _screenshotOnPress.value = enabled
    }

    fun onTopLeftXChanged(value: String) {
        _topLeftX.value = value
    }

    fun onTopLeftYChanged(value: String) {
        _topLeftY.value = value
    }

    fun onBottomRightXChanged(value: String) {
        _bottomRightX.value = value
    }

    fun onBottomRightYChanged(value: String) {
        _bottomRightY.value = value
    }

    fun toggleInspector() {
        _screenshotOnPress.value = !_screenshotOnPress.value
        val status = if (_screenshotOnPress.value) "ENABLED" else "DISABLED"
        consoleViewModel.addLog(LogLevel.Info, "Global Key Inspector $status via shortcut.")
    }

    fun copyToClipboard(text: String) {
        try {
            val selection = StringSelection(text)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
            consoleViewModel.addLog(LogLevel.Info, "Copied to clipboard: $text")
        } catch (e: Exception) {
            consoleViewModel.addLog(LogLevel.Error, "Failed to copy to clipboard: ${e.message}")
        }
    }
}