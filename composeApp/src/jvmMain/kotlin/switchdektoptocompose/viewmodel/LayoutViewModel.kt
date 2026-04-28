package switchdektoptocompose.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import switchdektoptocompose.logic.AppSettings

class LayoutViewModel(private val settingsViewModel: SettingsViewModel) {
    private val _showNewEventDialog = MutableStateFlow(false)
    val showNewEventDialog: StateFlow<Boolean> = _showNewEventDialog.asStateFlow()

    private val _showRecordDialog = MutableStateFlow(false)
    val showRecordDialog: StateFlow<Boolean> = _showRecordDialog.asStateFlow()

    private val _showExitDialogInternal = MutableStateFlow(false)
    val showExitDialogInternal: StateFlow<Boolean> = _showExitDialogInternal.asStateFlow()

    private val _showMarketplace = MutableStateFlow(false)
    val showMarketplace: StateFlow<Boolean> = _showMarketplace.asStateFlow()

    private val _showUpdateConfirmDialog = MutableStateFlow(false)
    val showUpdateConfirmDialog: StateFlow<Boolean> = _showUpdateConfirmDialog.asStateFlow()

    private val _mainTab = MutableStateFlow(0)
    val mainTab = _mainTab.asStateFlow()

    private val _virtualCursorPosition = MutableStateFlow(androidx.compose.ui.geometry.Offset.Zero)
    val virtualCursorPosition = _virtualCursorPosition.asStateFlow()

    private val _isVirtualCursorVisible = MutableStateFlow(false)
    val isVirtualCursorVisible = _isVirtualCursorVisible.asStateFlow()

    fun setShowNewEventDialog(show: Boolean) { _showNewEventDialog.value = show }
    fun setShowRecordDialog(show: Boolean) { _showRecordDialog.value = show }
    fun setShowExitDialogInternal(show: Boolean) { _showExitDialogInternal.value = show }
    fun setShowMarketplace(show: Boolean) { _showMarketplace.value = show }
    fun setShowUpdateConfirmDialog(show: Boolean) { _showUpdateConfirmDialog.value = show }

    fun setMainTab(index: Int) {
        _mainTab.value = index
    }

    fun setVirtualCursorVisible(visible: Boolean) {
        _isVirtualCursorVisible.value = visible
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
