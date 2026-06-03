package com.kapcode.open.macropad.kmps.desktop.viewmodel

import com.kapcode.open.macropad.kmps.desktop.network.MacroKtorServer
import com.kapcode.open.macropad.kmps.network.sockets.model.DataModel
import com.kapcode.open.macropad.kmps.network.sockets.model.activeProcessMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.kapcode.open.macropad.kmps.desktop.logic.AppSettings
import com.kapcode.open.macropad.kmps.desktop.logic.ProcessWatcher
import com.kapcode.open.macropad.kmps.desktop.logic.ProAccessManager
import com.kapcode.open.macropad.kmps.desktop.logic.ServerDiscoveryAnnouncer
import com.kapcode.open.macropad.kmps.desktop.logic.TrustedDeviceManager
import com.kapcode.open.macropad.kmps.desktop.model.LogLevel
import java.net.Inet4Address
import java.net.NetworkInterface

class ServerViewModel(
    private val settingsViewModel: SettingsViewModel,
    private val consoleViewModel: ConsoleViewModel,
    val processWatcher: ProcessWatcher,
    private val onMessageReceived: (String, DataModel) -> Unit,
    private val onClientConnected: (String, String) -> Unit,
    private val onClientDisconnected: (String) -> Unit,
    private val onPairingRequest: (String, String) -> Unit,
    private val onUpgradeRequest: (String, ByteArray, String, Boolean) -> Unit
) {
    var triggerListener: com.kapcode.open.macropad.kmps.desktop.logic.TriggerListener? = null
    var controllerManager: com.kapcode.open.macropad.kmps.desktop.logic.ControllerManager? = null
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    
    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning = _isServerRunning.asStateFlow()

    private val _serverIpAddress = MutableStateFlow("Determining IP...")
    val serverIpAddress = _serverIpAddress.asStateFlow()

    private val _serverError = MutableStateFlow<String?>(null)
    val serverError = _serverError.asStateFlow()

    private val _encryptionEnabled = MutableStateFlow(true)
    val encryptionEnabled = _encryptionEnabled.asStateFlow()

    val isProAccessActive = ProAccessManager.isProAccessActive
    val proAccessTimeRemaining = ProAccessManager.proAccessTimeRemaining

    val server = MacroKtorServer(
        appSettings = AppSettings,
        trustedDeviceManager = TrustedDeviceManager,
        onMessageReceived = onMessageReceived,
        onClientConnected = onClientConnected,
        onClientDisconnected = onClientDisconnected,
        onPairingRequest = onPairingRequest,
        onUpgradeRequest = onUpgradeRequest
    )

    private val discoveryAnnouncer = ServerDiscoveryAnnouncer()

    init {
        findLocalIpAddresses()
        
        viewModelScope.launch {
            processWatcher.activeProcess.collect { processName ->
                if (server.isRunning()) {
                    server.sendToAll(activeProcessMessage(processName))
                    consoleViewModel.addLog(LogLevel.Verbose, "Active process changed: ${processName ?: "None"}")
                }
            }
        }
    }

    fun setEncryption(enabled: Boolean) {
        if (!isServerRunning.value) {
            _encryptionEnabled.value = enabled
            consoleViewModel.addLog(LogLevel.Info, "Encryption ${if (enabled) "enabled" else "disabled"}")
        }
    }

    private fun findLocalIpAddresses() {
        val ips = try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.asSequence() }
                .filterIsInstance<Inet4Address>()
                .map { it.hostAddress }
                .joinToString(", ")
        } catch (_: Exception) {
            "Not Found"
        }
        _serverIpAddress.value = ips.ifBlank { "Not Found" }
        consoleViewModel.addLog(LogLevel.Verbose, "Found local IPs: $ips")
    }

    fun startServer(forceRecreateKeystore: Boolean = false) {
        if (server.isRunning() && !forceRecreateKeystore) return
        if (forceRecreateKeystore) stopServer()
        
        consoleViewModel.addLog(LogLevel.Info, if (forceRecreateKeystore) "Recreating keystore and starting server..." else "Starting server...")
        try {
            val port = if (encryptionEnabled.value) {
                settingsViewModel.secureServerPort.value
            } else {
                settingsViewModel.serverPort.value
            }
            
            server.start(port, encryptionEnabled.value)
            
            if (server.isRunning()) {
                _isServerRunning.value = true
                _serverError.value = null
                discoveryAnnouncer.start(AppSettings.serverPort.toString(), port, encryptionEnabled.value)
                processWatcher.startWatching()
                consoleViewModel.addLog(LogLevel.Info, "Server started on port $port")
            } else {
                consoleViewModel.addLog(LogLevel.Error, "Server failed to start")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isServerRunning.value = false
            _serverError.value = e.message
            consoleViewModel.addLog(LogLevel.Error, "Error starting server: ${e.message}")
        }
    }

    fun stopServer() {
        if (!server.isRunning()) return
        consoleViewModel.addLog(LogLevel.Info, "Stopping server...")
        discoveryAnnouncer.stop()
        processWatcher.stopWatching()
        viewModelScope.launch {
            server.stop()
            _isServerRunning.value = false
            consoleViewModel.addLog(LogLevel.Info, "Server stopped")
        }
    }

    fun clearServerError() {
        _serverError.value = null
    }

    fun sendToAll(dataModel: DataModel) {
        viewModelScope.launch {
            server.sendToAll(dataModel)
        }
    }

    fun sendToSelected(dataModel: DataModel, clientIds: Set<String>) {
        viewModelScope.launch {
            clientIds.forEach { clientId ->
                server.sendToClient(clientId, dataModel)
            }
        }
    }
}
