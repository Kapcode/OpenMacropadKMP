package com.kapcode.open.macropad.kmps

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ClientUiState(
    val connectionStatus: String = "Disconnected",
    val serverName: String? = null,
    val disconnectReason: String? = null,
    val verificationCode: String? = null,
    val macros: List<String> = emptyList(),
    val executingMacros: Set<String> = emptySet(),
    val failedMacros: Set<String> = emptySet(),
    val showQrScanner: Boolean = false,
    val isAutoZoomEnabled: Boolean = false,
    val isAutoFocusEnabled: Boolean = true,
    val manualZoomRatio: Float = 1f,
    val manualFocusDistance: Float = 0f,
    val currentActualZoom: Float = 1f,
    val currentFocusState: String = "Idle",
    val currency: Long = 0L
)

class ClientViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ClientUiState())
    val uiState: StateFlow<ClientUiState> = _uiState.asStateFlow()

    fun updateConnection(status: String, server: String?, reason: String?, code: String?) {
        _uiState.update { it.copy(
            connectionStatus = status,
            serverName = server ?: it.serverName,
            disconnectReason = reason,
            verificationCode = code
        ) }
    }

    fun setMacros(macros: List<String>) {
        _uiState.update { it.copy(macros = macros.toList()) }
    }

    fun setQrScannerVisible(visible: Boolean) {
        _uiState.update { it.copy(showQrScanner = visible) }
    }

    fun setAutoZoomEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isAutoZoomEnabled = enabled) }
    }

    fun setAutoFocusEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isAutoFocusEnabled = enabled) }
    }

    fun setManualZoomRatio(ratio: Float) {
        _uiState.update { it.copy(manualZoomRatio = ratio) }
    }

    fun setManualFocusDistance(distance: Float) {
        _uiState.update { it.copy(manualFocusDistance = distance) }
    }
    
    fun updateActualZoom(ratio: Float) {
        _uiState.update { it.copy(currentActualZoom = ratio) }
    }
    
    fun updateFocusState(state: String) {
        _uiState.update { it.copy(currentFocusState = state) }
    }

    fun updateCurrency(amount: Long) {
        _uiState.update { it.copy(currency = amount) }
    }

    fun onMacroExecutionStart(macro: String) {
        _uiState.update { it.copy(
            executingMacros = it.executingMacros + macro,
            failedMacros = it.failedMacros - macro
        ) }
    }

    fun onMacroExecutionComplete(macro: String) {
        _uiState.update { it.copy(
            executingMacros = it.executingMacros - macro,
            failedMacros = it.failedMacros - macro
        ) }
    }

    fun onMacroExecutionFailed(macro: String) {
        _uiState.update { it.copy(
            executingMacros = it.executingMacros - macro,
            failedMacros = it.failedMacros + macro
        ) }
    }
}
