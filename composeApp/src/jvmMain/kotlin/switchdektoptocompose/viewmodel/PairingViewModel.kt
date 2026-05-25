package switchdektoptocompose.viewmodel

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import switchdektoptocompose.model.ClientInfo
import switchdektoptocompose.utils.QrCodeGenerator

class PairingViewModel(
    private val settingsViewModel: SettingsViewModel,
    private val pendingPairingRequests: StateFlow<List<ClientInfo>>
) {
    private val _gridRows = MutableStateFlow(1)
    val gridRows = _gridRows.asStateFlow()

    private val _gridCols = MutableStateFlow(1)
    val gridCols = _gridCols.asStateFlow()

    private val _qrBitmaps = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val qrBitmaps = _qrBitmaps.asStateFlow()

    val fleetModeEnabled = settingsViewModel.fleetModeEnabled
    val fleetGridVisibility = settingsViewModel.fleetGridVisibility

    private val viewModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        pendingPairingRequests
            .onEach { updateQrBitmaps(it) }
            .launchIn(viewModelScope)
    }

    /**
     * Updates QR bitmaps for the given requests.
     * Uses a lock-free approach to ensure we don't regenerate existing bitmaps.
     */
    fun updateQrBitmaps(requests: List<ClientInfo>) {
        if (requests.isEmpty()) return
        
        viewModelScope.launch {
            val current = _qrBitmaps.value
            val needsUpdate = requests.any { !current.containsKey(it.id) }
            
            if (needsUpdate) {
                val updatedMap = current.toMutableMap()
                requests.forEach { request ->
                    if (!updatedMap.containsKey(request.id)) {
                        val qr = QrCodeGenerator.generateQrCode(request.verificationCode ?: "", 400)
                        updatedMap[request.id] = qr
                    }
                }
                _qrBitmaps.value = updatedMap
            }
        }
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
