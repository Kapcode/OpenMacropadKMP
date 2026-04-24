package com.kapcode.open.macropad.kmps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kapcode.open.macropad.kmps.models.GridWidget
import com.kapcode.open.macropad.kmps.models.MacroPack
import com.kapcode.open.macropad.kmps.models.MarketplaceItem
import com.kapcode.open.macropad.kmps.models.TrustedServer
import com.kapcode.open.macropad.kmps.network.ClientRepository
import com.kapcode.open.macropad.kmps.settings.AppTheme
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val isScannerTimedOut: Boolean = false,
    val currency: Long = 0L,
    val isMacroExecutionEnabled: Boolean = true,
    val activeProcess: String? = null,
    val activePack: MacroPack? = null,
    val installedPacks: List<MacroPack> = emptyList(),
    val filteredPacks: List<MacroPack> = emptyList(),
    val dashboardMacros: List<GridWidget> = emptyList(),
    val marketplaceItems: List<MarketplaceItem> = emptyList(),
    val isMarketplaceLoading: Boolean = false,
    val searchQuery: String = "",
    val currentTab: Int = 0, // 0: My Dashboard, 1: Active Pack, 2: Marketplace
    val isEditMode: Boolean = false,
    val serverHistory: List<TrustedServer> = emptyList()
)

class ClientViewModel(private val repository: ClientRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ClientUiState())
    val uiState: StateFlow<ClientUiState> = _uiState.asStateFlow()

    fun connect(
        ipAddress: String,
        port: Int,
        deviceName: String,
        isSecure: Boolean,
        discoveryFingerprint: String?,
        serverName: String? = null,
        tokenManager: TokenManager,
        settingsViewModel: SettingsViewModel,
        onExecutionFailedToast: (String) -> Unit
    ) {
        repository.connect(
            ipAddress = ipAddress,
            port = port,
            deviceName = deviceName,
            isSecure = isSecure,
            discoveryFingerprint = discoveryFingerprint,
            serverName = serverName,
            onUpdate = { status, name, reason, code ->
                updateConnection(status, name, reason, code)
                if (status == "Connected") {
                    val server = TrustedServer(
                        serverId = discoveryFingerprint ?: "$ipAddress:$port", // Use fingerprint as ID if available
                        displayName = name ?: ipAddress,
                        lastIpAddress = ipAddress,
                        port = port,
                        isSecure = isSecure,
                        lastConnectedTimestamp = System.currentTimeMillis()
                    )
                    settingsViewModel.updateServerHistory(server)
                }
            },
            onMacrosReceived = { macros ->
                setMacros(macros)
            },
            onActiveProcessChanged = { process ->
                setActiveProcess(process)
            },
            onCurrencyUpdate = { balance ->
                updateCurrency(balance)
            },
            onExecutionStart = { macro ->
                onMacroExecutionStart(macro)
                if (tokenManager.spendTokens(BillingConstants.TOKENS_PER_MACRO_PRESS)) {
                    repository.sendData("currency_spent", BillingConstants.TOKENS_PER_MACRO_PRESS.toString())
                    repository.sendData("currency_update", tokenManager.tokenBalance.value.toString())
                }
            },
            onExecutionComplete = { macro ->
                onMacroExecutionComplete(macro)
            },
            onExecutionFailed = { macro, error ->
                onMacroExecutionFailed(macro)
                tokenManager.awardTokens(BillingConstants.TOKENS_PER_MACRO_PRESS)
                repository.sendData("currency_spent", (-BillingConstants.TOKENS_PER_MACRO_PRESS).toString())
                repository.sendData("currency_update", tokenManager.tokenBalance.value.toString())
                onExecutionFailedToast("Macro '$macro' failed: $error")
            },
            onSettingsPushed = { params ->
                params["theme"]?.let { themeStr ->
                    try {
                        settingsViewModel.setTheme(AppTheme.valueOf(themeStr))
                    } catch (e: Exception) {}
                }
                params["analyticsEnabled"]?.let { settingsViewModel.setAnalyticsEnabled(it.toBoolean()) }
                params["slamFireEnabled"]?.let { settingsViewModel.setSlamFireEnabled(it.toBoolean()) }
                params["macroExecutionEnabled"]?.let { setMacroExecutionEnabled(it.toBoolean()) }
            },
            onPacksReceived = { packs ->
                setInstalledPacks(packs)
            },
            onMarketplaceItemsReceived = { items ->
                setMarketplaceItems(items)
            }
        )
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun sendMacro(macroName: String) {
        repository.sendMacro(macroName)
    }

    fun submitPairingCode(code: String) {
        repository.submitPairingCode(code)
    }

    fun requestMacros() {
        repository.requestMacros()
    }

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
        _uiState.update { it.copy(
            showQrScanner = visible,
            isScannerTimedOut = false // Reset timeout when visibility changes
        ) }
    }

    fun setScannerTimedOut(timedOut: Boolean) {
        _uiState.update { it.copy(isScannerTimedOut = timedOut) }
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

    fun setDashboardMacros(widgets: List<GridWidget>) {
        _uiState.update { it.copy(dashboardMacros = widgets) }
    }

    fun setServerHistory(history: List<TrustedServer>) {
        _uiState.update { it.copy(serverHistory = history) }
    }

    fun connectToServer(
        server: TrustedServer,
        deviceName: String,
        tokenManager: TokenManager,
        settingsViewModel: SettingsViewModel,
        onExecutionFailedToast: (String) -> Unit
    ) {
        connect(
            ipAddress = server.lastIpAddress,
            port = server.port,
            deviceName = deviceName,
            isSecure = server.isSecure,
            discoveryFingerprint = if (server.serverId.contains(":")) null else server.serverId,
            serverName = server.displayName,
            tokenManager = tokenManager,
            settingsViewModel = settingsViewModel,
            onExecutionFailedToast = onExecutionFailedToast
        )
    }

    fun requestMarketplace() {
        _uiState.update { it.copy(isMarketplaceLoading = true) }
        repository.requestMarketplace()
    }

    fun setMarketplaceItems(items: List<MarketplaceItem>) {
        _uiState.update { it.copy(marketplaceItems = items, isMarketplaceLoading = false) }
    }

    fun downloadMarketplaceItem(item: MarketplaceItem) {
        repository.downloadMarketplaceItem(item.id)
    }

    fun addToDashboard(widget: GridWidget) {
        _uiState.update { state ->
            if (state.dashboardMacros.any { it.id == widget.id }) state
            else state.copy(dashboardMacros = state.dashboardMacros + widget)
        }
    }

    fun removeFromDashboard(widgetId: String) {
        _uiState.update { state ->
            state.copy(dashboardMacros = state.dashboardMacros.filter { it.id != widgetId })
        }
    }

    fun moveDashboardMacro(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val list = state.dashboardMacros.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                state.copy(dashboardMacros = list)
            } else {
                state
            }
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
