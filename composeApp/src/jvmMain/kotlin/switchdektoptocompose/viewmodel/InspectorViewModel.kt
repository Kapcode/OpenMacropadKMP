package switchdektoptocompose.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InspectorViewModel(
    private val consoleViewModel: ConsoleViewModel
) {
    private val _selectedFKey = MutableStateFlow("F1")
    val selectedFKey = _selectedFKey.asStateFlow()

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
}