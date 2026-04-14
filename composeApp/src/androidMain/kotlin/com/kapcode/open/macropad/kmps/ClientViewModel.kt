package com.kapcode.open.macropad.kmps

import androidx.lifecycle.ViewModel
import com.kapcode.open.macropad.kmps.models.MacroPack
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
    val currency: Long = 0L,
    val isMacroExecutionEnabled: Boolean = true,
    val activeProcess: String? = null,
    val activePack: MacroPack? = null,
    val installedPacks: List<MacroPack> = emptyList(),
    val filteredPacks: List<MacroPack> = emptyList(),
    val dashboardMacros: List<String> = emptyList(),
    val searchQuery: String = "",
    val currentTab: Int = 0, // 0: Active Pack, 1: My Dashboard
    val isEditMode: Boolean = false
)

class ClientViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ClientUiState())
    val uiState: StateFlow<ClientUiState> = _uiState.asStateFlow()

    fun updateConnection(status: String, server: String?, reason: String?, code: String?) {
        _uiState.update { it.copy(
            connectionStatus = status,
            serverName = server ?: it.serverName,
            disconnectReason = reason,
            verificationCode = code,
            // If we're newly connected, reset currency to 0 until first update
            currency = if (status == "Connected") 0L else it.currency
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

    fun setMacroExecutionEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isMacroExecutionEnabled = enabled) }
    }

    fun setInstalledPacks(packs: List<MacroPack>) {
        _uiState.update { state ->
            state.copy(
                installedPacks = packs,
                filteredPacks = filterPacks(packs, state.searchQuery)
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredPacks = filterPacks(state.installedPacks, query)
            )
        }
    }

    private fun filterPacks(packs: List<MacroPack>, query: String): List<MacroPack> {
        if (query.isBlank()) return packs
        return packs.filter { 
            it.name.contains(query, ignoreCase = true) || 
            it.author.contains(query, ignoreCase = true) ||
            it.targetProcess?.contains(query, ignoreCase = true) == true
        }
    }

    fun setActiveProcess(process: String?) {
        _uiState.update { state ->
            val matchingPack = state.installedPacks.find { it.targetProcess?.equals(process, ignoreCase = true) == true }
            state.copy(
                activeProcess = process,
                activePack = matchingPack ?: state.activePack
            )
        }
    }

    fun setActivePack(pack: MacroPack?) {
        _uiState.update { it.copy(activePack = pack) }
    }

    fun setTab(index: Int) {
        _uiState.update { it.copy(currentTab = index) }
    }

    fun setEditMode(enabled: Boolean) {
        _uiState.update { it.copy(isEditMode = enabled) }
    }

    fun setDashboardMacros(macros: List<String>) {
        _uiState.update { it.copy(dashboardMacros = macros) }
    }

    fun addToDashboard(macro: String) {
        _uiState.update { state ->
            if (state.dashboardMacros.contains(macro)) state
            else state.copy(dashboardMacros = state.dashboardMacros + macro)
        }
    }

    fun removeFromDashboard(macro: String) {
        _uiState.update { state ->
            state.copy(dashboardMacros = state.dashboardMacros - macro)
        }
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
