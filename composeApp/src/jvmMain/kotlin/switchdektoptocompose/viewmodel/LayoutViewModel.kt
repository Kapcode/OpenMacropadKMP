package switchdektoptocompose.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

    fun setShowNewEventDialog(show: Boolean) { _showNewEventDialog.value = show }
    fun setShowRecordDialog(show: Boolean) { _showRecordDialog.value = show }
    fun setShowExitDialogInternal(show: Boolean) { _showExitDialogInternal.value = show }
    fun setShowMarketplace(show: Boolean) { _showMarketplace.value = show }
    fun setShowUpdateConfirmDialog(show: Boolean) { _showUpdateConfirmDialog.value = show }

    fun getSplitterPosition(name: String, default: Float): Float {
        return settingsViewModel.getSplitterPosition(name, default)
    }

    fun setSplitterPosition(name: String, position: Float) {
        settingsViewModel.setSplitterPosition(name, position)
    }
}
