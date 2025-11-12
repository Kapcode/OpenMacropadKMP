package switchdektoptocompose

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