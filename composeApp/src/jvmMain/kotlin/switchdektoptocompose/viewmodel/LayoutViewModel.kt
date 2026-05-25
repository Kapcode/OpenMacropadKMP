package switchdektoptocompose.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import switchdektoptocompose.logic.AppSettings

class LayoutViewModel(private val settingsViewModel: SettingsViewModel) {
    private val _mainTab = MutableStateFlow(0)
    val mainTab = _mainTab.asStateFlow()

    private val _virtualCursorPosition = MutableStateFlow(androidx.compose.ui.geometry.Offset.Zero)
    val virtualCursorPosition = _virtualCursorPosition.asStateFlow()

    private val _isVirtualCursorVisible = MutableStateFlow(false)
    val isVirtualCursorVisible = _isVirtualCursorVisible.asStateFlow()

    // Dialog Visibility States
    private val _showUpdateConfirmDialog = MutableStateFlow(false)
    val showUpdateConfirmDialog = _showUpdateConfirmDialog.asStateFlow()

    private val _showExitDialog = MutableStateFlow(false)
    val showExitDialog = _showExitDialog.asStateFlow()

    private val _showShortcutsDialog = MutableStateFlow(false)
    val showShortcutsDialog = _showShortcutsDialog.asStateFlow()


    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog = _showSettingsDialog.asStateFlow()

    private val _scrollToVariables = MutableStateFlow(false)
    val scrollToVariables = _scrollToVariables.asStateFlow()

    private val _scrollToSecurity = MutableStateFlow(false)
    val scrollToSecurity = _scrollToSecurity.asStateFlow()

    private val _showMarketplace = MutableStateFlow(false)
    val showMarketplace = _showMarketplace.asStateFlow()

    private val _showNewEventDialog = MutableStateFlow(false)
    val showNewEventDialog = _showNewEventDialog.asStateFlow()

    private val _showRecordDialog = MutableStateFlow(false)
    val showRecordDialog = _showRecordDialog.asStateFlow()

    private val _showLoggingWarning = MutableStateFlow(false)
    val showLoggingWarning = _showLoggingWarning.asStateFlow()

    private val _showAppInfo = MutableStateFlow(false)
    val showAppInfo = _showAppInfo.asStateFlow()

    fun setMainTab(index: Int) {
        _mainTab.value = index
    }

    fun setVirtualCursorVisible(visible: Boolean) {
        _isVirtualCursorVisible.value = visible
    }

    fun setShowUpdateConfirmDialog(show: Boolean) {
        _showUpdateConfirmDialog.value = show
    }

    fun setShowExitDialog(show: Boolean) {
        _showExitDialog.value = show
    }

    fun setShowShortcutsDialog(show: Boolean) {
        _showShortcutsDialog.value = show
    }


    fun setShowSettingsDialog(show: Boolean, scrollToVariables: Boolean = false, scrollToSecurity: Boolean = false) {
        _scrollToVariables.value = scrollToVariables
        _scrollToSecurity.value = scrollToSecurity
        _showSettingsDialog.value = show
    }

    fun setShowMarketplace(show: Boolean) {
        _showMarketplace.value = show
    }

    fun setShowNewEventDialog(show: Boolean) {
        _showNewEventDialog.value = show
    }

    fun setShowRecordDialog(show: Boolean) {
        _showRecordDialog.value = show
    }

    fun setShowLoggingWarning(show: Boolean) {
        _showLoggingWarning.value = show
    }

    fun setShowAppInfo(show: Boolean) {
        _showAppInfo.value = show
    }

    fun updateVirtualCursor(x: Float, y: Float) {
        _virtualCursorPosition.value = androidx.compose.ui.geometry.Offset(x, y)
    }

    fun getSplitterPosition(name: String, default: Float): Float {
        return settingsViewModel.getSplitterPosition(name, default)
    }

    fun setSplitterPosition(name: String, position: Float) {
        settingsViewModel.setSplitterPosition(name, position)
    }
}
