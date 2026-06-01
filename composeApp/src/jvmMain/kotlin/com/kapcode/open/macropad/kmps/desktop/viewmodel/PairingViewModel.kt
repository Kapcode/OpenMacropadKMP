package com.kapcode.open.macropad.kmps.desktop.viewmodel

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.kapcode.open.macropad.kmps.desktop.model.ClientInfo
import com.kapcode.open.macropad.kmps.desktop.utils.QrCodeGenerator

class PairingViewModel(
    private val settingsViewModel: SettingsViewModel,
    private val pendingPairingRequests: StateFlow<List<ClientInfo>>,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val _gridRows = MutableStateFlow(1)
    val gridRows = _gridRows.asStateFlow()

    private val _gridCols = MutableStateFlow(1)
    val gridCols = _gridCols.asStateFlow()

    private val _qrBitmaps = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val qrBitmaps = _qrBitmaps.asStateFlow()

    val fleetModeEnabled = settingsViewModel.fleetModeEnabled
    val fleetGridVisibility = settingsViewModel.fleetGridVisibility

    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        pendingPairingRequests
            .onEach { updateQrBitmaps(it) }
            .launchIn(viewModelScope)
    }

    /**
     * Updates QR bitmaps for the given requests.
     * Ensures we regenerate if the verification code changes (e.g. on reconnect)
     * and cleans up stale bitmaps.
     */
    fun updateQrBitmaps(requests: List<ClientInfo>) {
        viewModelScope.launch {
            val current = _qrBitmaps.value
            val activeIds = requests.map { it.id }.toSet()
            
            // 1. Clean up stale bitmaps and start with active ones
            val updatedMap = current.filterKeys { it in activeIds }.toMutableMap()
            var changed = updatedMap.size != current.size
            
            // 2. Add or update bitmaps
            requests.forEach { request ->
                if (!updatedMap.containsKey(request.id)) {
                    val qr = withContext(backgroundDispatcher) {
                        QrCodeGenerator.generateQrCode(request.verificationCode ?: "", 400)
                    }
                    updatedMap[request.id] = qr
                    changed = true
                }
            }
            
            if (changed) {
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
