package switchdektoptocompose.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import switchdektoptocompose.logic.AppSettings
import java.io.File
import javax.swing.JFileChooser

class SettingsViewModel {
    // --- StateFlows for UI ---
    private val _macroDirectory = MutableStateFlow(AppSettings.macroDirectory)
    val macroDirectory = _macroDirectory.asStateFlow()

    private val _serverPort = MutableStateFlow(AppSettings.serverPort)
    val serverPort = _serverPort.asStateFlow()

    private val _secureServerPort = MutableStateFlow(AppSettings.secureServerPort)
    val secureServerPort = _secureServerPort.asStateFlow()

    private val _eStopKey = MutableStateFlow(AppSettings.eStopKey)
    val eStopKey = _eStopKey.asStateFlow()
    
    private val _exitBehavior = MutableStateFlow(AppSettings.exitBehavior)
    val exitBehavior = _exitBehavior.asStateFlow()

    private val _clickTrayToToggle = MutableStateFlow(AppSettings.clickTrayToToggle)
    val clickTrayToToggle = _clickTrayToToggle.asStateFlow()

    private val _animateToTray = MutableStateFlow(AppSettings.animateToTray)
    val animateToTray = _animateToTray.asStateFlow()

    private val _hardEstop = MutableStateFlow(AppSettings.hardEstop)
    val hardEstop = _hardEstop.asStateFlow()

    private val _allowNewConnections = MutableStateFlow(AppSettings.allowNewConnections)
    val allowNewConnections = _allowNewConnections.asStateFlow()

    private val _allowOnceOnly = MutableStateFlow(AppSettings.allowOnceOnly)
    val allowOnceOnly = _allowOnceOnly.asStateFlow()

    private val _fleetModeEnabled = MutableStateFlow(AppSettings.fleetModeEnabled)
    val fleetModeEnabled = _fleetModeEnabled.asStateFlow()

    private val _enableWebsocketPings = MutableStateFlow(AppSettings.enableWebsocketPings)
    val enableWebsocketPings = _enableWebsocketPings.asStateFlow()

    private val _multiQrEnabled = MutableStateFlow(false) // Default to false
    val multiQrEnabled = _multiQrEnabled.asStateFlow()

    private val _fleetGridVisibility = MutableStateFlow(AppSettings.fleetGridVisibility.split(",").map { it == "1" })
    val fleetGridVisibility = _fleetGridVisibility.asStateFlow()

    private val _defaultPairingModeQr = MutableStateFlow(AppSettings.defaultPairingModeQr)
    val defaultPairingModeQr = _defaultPairingModeQr.asStateFlow()

    // Connected Clients Settings
    private val _clientTheme = MutableStateFlow(AppSettings.clientTheme)
    val clientTheme = _clientTheme.asStateFlow()

    private val _clientAnalyticsEnabled = MutableStateFlow(AppSettings.clientAnalyticsEnabled)
    val clientAnalyticsEnabled = _clientAnalyticsEnabled.asStateFlow()

    private val _clientSlamFireEnabled = MutableStateFlow(AppSettings.clientSlamFireEnabled)
    val clientSlamFireEnabled = _clientSlamFireEnabled.asStateFlow()

    private val _clientSlamFireAction = MutableStateFlow(AppSettings.clientSlamFireAction)
    val clientSlamFireAction = _clientSlamFireAction.asStateFlow()

    // Shortcuts
    private val _copyConsoleOutputShortcut = MutableStateFlow(AppSettings.copyConsoleOutputShortcut)
    val copyConsoleOutputShortcut = _copyConsoleOutputShortcut.asStateFlow()

    private val _stopKeyShortcut = MutableStateFlow(AppSettings.stopKeyShortcut)
    val stopKeyShortcut = _stopKeyShortcut.asStateFlow()

    private val _inspectKeyShortcut = MutableStateFlow(AppSettings.inspectKeyShortcut)
    val inspectKeyShortcut = _inspectKeyShortcut.asStateFlow()

    private val _splitterSwapModifier = MutableStateFlow(AppSettings.splitterSwapModifier)
    val splitterSwapModifier = _splitterSwapModifier.asStateFlow()

    private val _splitterInfoModifier = MutableStateFlow(AppSettings.splitterInfoModifier)
    val splitterInfoModifier = _splitterInfoModifier.asStateFlow()

    private val _tooltipXOffset = MutableStateFlow(AppSettings.tooltipXOffset)
    val tooltipXOffset = _tooltipXOffset.asStateFlow()

    private val _tooltipYOffset = MutableStateFlow(AppSettings.tooltipYOffset)
    val tooltipYOffset = _tooltipYOffset.asStateFlow()

    private val _enablePackSwitchNotifications = MutableStateFlow(AppSettings.enablePackSwitchNotifications)
    val enablePackSwitchNotifications = _enablePackSwitchNotifications.asStateFlow()

    private val _enableToasts = MutableStateFlow(AppSettings.enableToasts)
    val enableToasts = _enableToasts.asStateFlow()

    private val _enableBackgroundToasts = MutableStateFlow(AppSettings.enableBackgroundToasts)
    val enableBackgroundToasts = _enableBackgroundToasts.asStateFlow()

    private val _enableNetworkToasts = MutableStateFlow(AppSettings.enableNetworkToasts)
    val enableNetworkToasts = _enableNetworkToasts.asStateFlow()

    private val _includeWindowNamesInToasts = MutableStateFlow(AppSettings.includeWindowNamesInToasts)
    val includeWindowNamesInToasts = _includeWindowNamesInToasts.asStateFlow()

    private val _toastDurationMs = MutableStateFlow(AppSettings.toastDurationMs)
    val toastDurationMs = _toastDurationMs.asStateFlow()

    private val _toastTarget = MutableStateFlow(AppSettings.toastTarget)
    val toastTarget = _toastTarget.asStateFlow()

    private val _notificationClientIds = MutableStateFlow(AppSettings.notificationClientIds.split(",").filter { it.isNotBlank() }.toSet())
    val notificationClientIds = _notificationClientIds.asStateFlow()

    private val _windowPlacementMode = MutableStateFlow(AppSettings.windowPlacementMode)
    val windowPlacementMode = _windowPlacementMode.asStateFlow()

    private val _windowMonitorIndex = MutableStateFlow(AppSettings.windowMonitorIndex)
    val windowMonitorIndex = _windowMonitorIndex.asStateFlow()

    private val _windowPollingRate = MutableStateFlow(AppSettings.windowPollingRate)
    val windowPollingRate = _windowPollingRate.asStateFlow()

    private val _mousePollingRate = MutableStateFlow(AppSettings.mousePollingRate)
    val mousePollingRate = _mousePollingRate.asStateFlow()

    private val _systemPollingRate = MutableStateFlow(AppSettings.systemPollingRate)
    val systemPollingRate = _systemPollingRate.asStateFlow()

    private val _controllerNavigationMode = MutableStateFlow(AppSettings.controllerNavigationMode)
    val controllerNavigationMode = _controllerNavigationMode.asStateFlow()

    // For now, we'll keep theme settings separate as they are specific to the Compose UI.
    // In the future, this could also be moved to the properties file if desired.
    private val _selectedTheme = MutableStateFlow("Dark Blue") // Default value
    val selectedTheme = _selectedTheme.asStateFlow() // Corrected the typo from _selected_theme
    val availableThemes = listOf("Dark Blue", "Light Blue")

    fun chooseMacroDirectory() {
        val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Select Macro Directory"
            currentDirectory = File(macroDirectory.value)
        }

        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedDirectory = fileChooser.selectedFile.absolutePath
            // Save the setting to the properties file via switchdektoptocompose.AppSettings
            AppSettings.macroDirectory = selectedDirectory
            // Update the UI by updating the StateFlow
            _macroDirectory.value = selectedDirectory
        }
    }

    fun onServerPortChange(port: String) {
        port.toIntOrNull()?.let {
            _serverPort.value = it
            AppSettings.serverPort = it
        }
    }

    fun onSecureServerPortChange(port: String) {
        port.toIntOrNull()?.let {
            _secureServerPort.value = it
            AppSettings.secureServerPort = it
        }
    }

    fun setEStopKey(key: String) {
        _eStopKey.value = key
        AppSettings.eStopKey = key
    }
    
    fun setExitBehavior(value: String) {
        _exitBehavior.value = value
        AppSettings.exitBehavior = value
    }

    fun setClickTrayToToggle(enabled: Boolean) {
        _clickTrayToToggle.value = enabled
        AppSettings.clickTrayToToggle = enabled
    }

    fun setAnimateToTray(enabled: Boolean) {
        _animateToTray.value = enabled
        AppSettings.animateToTray = enabled
    }

    fun setHardEstop(enabled: Boolean) {
        _hardEstop.value = enabled
        AppSettings.hardEstop = enabled
    }

    fun setAllowNewConnections(enabled: Boolean) {
        _allowNewConnections.value = enabled
        AppSettings.allowNewConnections = enabled
    }

    fun setAllowOnceOnly(enabled: Boolean) {
        _allowOnceOnly.value = enabled
        AppSettings.allowOnceOnly = enabled
    }

    fun setFleetModeEnabled(enabled: Boolean) {
        _fleetModeEnabled.value = enabled
        AppSettings.fleetModeEnabled = enabled
    }

    fun setEnableWebsocketPings(enabled: Boolean) {
        _enableWebsocketPings.value = enabled
        AppSettings.enableWebsocketPings = enabled
    }

    fun setMultiQrEnabled(enabled: Boolean) {
        _multiQrEnabled.value = enabled
    }

    fun setFleetGridVisibility(index: Int, visible: Boolean) {
        val current = _fleetGridVisibility.value.toMutableList()
        if (index in current.indices) {
            current[index] = visible
            _fleetGridVisibility.value = current
            AppSettings.fleetGridVisibility = current.map { if (it) "1" else "0" }.joinToString(",")
        }
    }

    fun setDefaultPairingModeQr(enabled: Boolean) {
        _defaultPairingModeQr.value = enabled
        AppSettings.defaultPairingModeQr = enabled
    }

    fun setClientTheme(theme: String) {
        _clientTheme.value = theme
        AppSettings.clientTheme = theme
        pushSettingsToClients()
    }

    fun setClientAnalyticsEnabled(enabled: Boolean) {
        _clientAnalyticsEnabled.value = enabled
        AppSettings.clientAnalyticsEnabled = enabled
        pushSettingsToClients()
    }

    fun setClientSlamFireEnabled(enabled: Boolean) {
        _clientSlamFireEnabled.value = enabled
        AppSettings.clientSlamFireEnabled = enabled
        pushSettingsToClients()
    }

    fun setClientSlamFireAction(action: String) {
        _clientSlamFireAction.value = action
        AppSettings.clientSlamFireAction = action
        pushSettingsToClients()
    }

    private var _pushSettingsCallback: (() -> Unit)? = null
    fun setPushSettingsCallback(callback: () -> Unit) {
        _pushSettingsCallback = callback
    }

    private fun pushSettingsToClients() {
        _pushSettingsCallback?.invoke()
    }

    fun setCopyConsoleOutputShortcut(shortcut: String) {
        _copyConsoleOutputShortcut.value = shortcut
        AppSettings.copyConsoleOutputShortcut = shortcut
    }

    fun setStopKeyShortcut(shortcut: String) {
        _stopKeyShortcut.value = shortcut
        AppSettings.stopKeyShortcut = shortcut
    }

    fun setInspectKeyShortcut(shortcut: String) {
        _inspectKeyShortcut.value = shortcut
        AppSettings.inspectKeyShortcut = shortcut
    }

    fun setSplitterSwapModifier(modifier: String) {
        _splitterSwapModifier.value = modifier
        AppSettings.splitterSwapModifier = modifier
    }

    fun setSplitterInfoModifier(modifier: String) {
        _splitterInfoModifier.value = modifier
        AppSettings.splitterInfoModifier = modifier
    }

    fun setTooltipXOffset(offset: Int) {
        _tooltipXOffset.value = offset
        AppSettings.tooltipXOffset = offset
    }

    fun setTooltipYOffset(offset: Int) {
        _tooltipYOffset.value = offset
        AppSettings.tooltipYOffset = offset
    }

    fun setEnablePackSwitchNotifications(enabled: Boolean) {
        _enablePackSwitchNotifications.value = enabled
        AppSettings.enablePackSwitchNotifications = enabled
    }

    fun setEnableToasts(enabled: Boolean) {
        _enableToasts.value = enabled
        AppSettings.enableToasts = enabled
    }

    fun setEnableBackgroundToasts(enabled: Boolean) {
        _enableBackgroundToasts.value = enabled
        AppSettings.enableBackgroundToasts = enabled
    }

    fun setEnableNetworkToasts(enabled: Boolean) {
        _enableNetworkToasts.value = enabled
        AppSettings.enableNetworkToasts = enabled
    }

    fun setIncludeWindowNamesInToasts(enabled: Boolean) {
        _includeWindowNamesInToasts.value = enabled
        AppSettings.includeWindowNamesInToasts = enabled
    }

    fun setToastDurationMs(duration: Long) {
        _toastDurationMs.value = duration
        AppSettings.toastDurationMs = duration
    }

    fun setToastTarget(target: String) {
        _toastTarget.value = target
        AppSettings.toastTarget = target
    }

    fun toggleNotificationClient(clientId: String) {
        val current = _notificationClientIds.value
        val newSet = if (current.contains(clientId)) current - clientId else current + clientId
        _notificationClientIds.value = newSet
        AppSettings.notificationClientIds = newSet.joinToString(",")
    }

    fun setAllNotificationClients(clientIds: Set<String>) {
        _notificationClientIds.value = clientIds
        AppSettings.notificationClientIds = clientIds.joinToString(",")
    }

    fun setWindowPlacementMode(mode: String) {
        _windowPlacementMode.value = mode
        AppSettings.windowPlacementMode = mode
    }

    fun setWindowMonitorIndex(index: Int) {
        _windowMonitorIndex.value = index
        AppSettings.windowMonitorIndex = index
    }

    fun setWindowPollingRate(rate: Long) {
        _windowPollingRate.value = rate
        AppSettings.windowPollingRate = rate
    }

    fun setMousePollingRate(rate: Long) {
        _mousePollingRate.value = rate
        AppSettings.mousePollingRate = rate
    }

    fun setSystemPollingRate(rate: Long) {
        _systemPollingRate.value = rate
        AppSettings.systemPollingRate = rate
    }

    fun setControllerNavigationMode(mode: String) {
        _controllerNavigationMode.value = mode
        AppSettings.controllerNavigationMode = mode
    }

    fun getSplitterPosition(name: String, default: Float): Float {
        return AppSettings.getSplitterPosition(name, default)
    }

    fun setSplitterPosition(name: String, position: Float) {
        AppSettings.setSplitterPosition(name, position)
    }

    fun getSplitterSwapped(name: String, default: Boolean): Boolean {
        return AppSettings.getSplitterSwapped(name, default)
    }

    fun setSplitterSwapped(name: String, swapped: Boolean) {
        AppSettings.setSplitterSwapped(name, swapped)
    }

    fun selectTheme(theme: String) {
        if (theme in availableThemes) {
            _selectedTheme.value = theme
        }
    }
}