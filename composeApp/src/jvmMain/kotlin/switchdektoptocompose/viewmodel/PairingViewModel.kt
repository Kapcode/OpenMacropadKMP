package switchdektoptocompose.viewmodel

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.*
import switchdektoptocompose.model.ClientInfo
import switchdektoptocompose.utils.QrCodeGenerator

class PairingViewModel(
    private val settingsViewModel: SettingsViewModel
) {
    private val _gridRows = MutableStateFlow(1)
    val gridRows = _gridRows.asStateFlow()

    private val _gridCols = MutableStateFlow(1)
    val gridCols = _gridCols.asStateFlow()

    private val _qrBitmaps = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val qrBitmaps = _qrBitmaps.asStateFlow()

    val fleetModeEnabled = settingsViewModel.fleetModeEnabled
    val fleetGridVisibility = settingsViewModel.fleetGridVisibility

    fun updateQrBitmaps(requests: List<ClientInfo>) {
        val currentBitmaps = _qrBitmaps.value
        val newBitmaps = requests.associate { request ->
            request.id to (currentBitmaps[request.id] ?: QrCodeGenerator.generateQrCode(request.verificationCode ?: "", 400))
        }
        _qrBitmaps.value = newBitmaps
    }

    fun setGridRows(rows: Int) {
        _gridRows.value = rows
    }

    fun setGridCols(cols: Int) {
        _gridCols.value = cols
    }

    fun setFleetModeEnabled(enabled: Boolean) {
        settingsViewModel.setFleetModeEnabled(enabled)
    }

    fun setFleetGridVisibility(index: Int, visible: Boolean) {
        settingsViewModel.setFleetGridVisibility(index, visible)
    }

    fun calculateSmartBounds(maxWidth: Dp, maxHeight: Dp, sideGridsVisible: Boolean, centerGridsVisible: Boolean): Pair<Int, Int> {
        val smartMaxCols = if (sideGridsVisible) {
            val sideSpace = (maxWidth.value - 900) / 2 - 16
            ((sideSpace - 8) / 158).toInt().coerceIn(1, 4)
        } else {
            val fullSpace = maxWidth.value - 32
            ((fullSpace - 8) / 158).toInt().coerceIn(1, 10)
        }

        val smartMaxRows = if (centerGridsVisible) {
            ((maxHeight.value - 448) / 404).toInt().coerceIn(1, 4)
        } else {
            ((maxHeight.value - 48) / 364).toInt().coerceIn(1, 6)
        }
        
        return smartMaxRows to smartMaxCols
    }
}
