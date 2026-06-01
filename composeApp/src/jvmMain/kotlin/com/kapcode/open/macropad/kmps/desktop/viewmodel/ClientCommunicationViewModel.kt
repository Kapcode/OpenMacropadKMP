package com.kapcode.open.macropad.kmps.desktop.viewmodel

import com.kapcode.open.macropad.kmps.network.sockets.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.kapcode.open.macropad.kmps.desktop.logic.AppSettings
import com.kapcode.open.macropad.kmps.desktop.logic.ConnectionHistoryManager
import com.kapcode.open.macropad.kmps.desktop.logic.TrustedDeviceManager
import com.kapcode.open.macropad.kmps.desktop.logic.PairingBanManager
import com.kapcode.open.macropad.kmps.desktop.logic.ProAccessManager
import com.kapcode.open.macropad.kmps.desktop.model.ClientInfo
import com.kapcode.open.macropad.kmps.desktop.model.LogLevel
import java.io.File

class ClientCommunicationViewModel(
    private val settingsViewModel: SettingsViewModel,
    private val consoleViewModel: ConsoleViewModel
) {
    lateinit var macroManagerViewModel: MacroManagerViewModel
    lateinit var serverViewModel: ServerViewModel
    lateinit var layoutViewModel: LayoutViewModel
    lateinit var marketplaceViewModel: MarketplaceViewModel

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _connectedDevices = MutableStateFlow<List<ClientInfo>>(emptyList())
    val connectedDevices = _connectedDevices.asStateFlow()

    private val _pendingPairingRequests = MutableStateFlow<List<ClientInfo>>(emptyList())
    val pendingPairingRequests = _pendingPairingRequests.asStateFlow()

    private val _trustedDevices = MutableStateFlow(TrustedDeviceManager.getTrustedDevices())
    val trustedDevices = _trustedDevices.asStateFlow()

    private val _bannedDevices = MutableStateFlow(TrustedDeviceManager.getBannedDevices())
    val bannedDevices = _bannedDevices.asStateFlow()

    private val _connectionHistory = MutableStateFlow<List<ConnectionHistoryManager.ConnectionEvent>>(emptyList())
    val connectionHistory = _connectionHistory.asStateFlow()

    private val _totalCurrencySpent = MutableStateFlow(AppSettings.totalCurrencySpent)
    val totalCurrencySpent = _totalCurrencySpent.asStateFlow()

    private val _isMacroExecutionEnabled = MutableStateFlow(true)
    val isMacroExecutionEnabled = _isMacroExecutionEnabled.asStateFlow()

    // Update Request State
    data class PendingUpdate(
        val clientId: String,
        val clientName: String,
        val jarBytes: ByteArray,
        val hash: String,
        val isSimulation: Boolean
    )
    private val _pendingUpdate = MutableStateFlow<PendingUpdate?>(null)
    val pendingUpdate = _pendingUpdate.asStateFlow()

    init {
        updateHistoryState()
        
        viewModelScope.launch {
            ProAccessManager.isProAccessActive.collect { active ->
                broadcastProStatus()
            }
        }
    }

    private fun broadcastProStatus() {
        val expiry = AppSettings.globalProExpiry
        val isActive = ProAccessManager.isProAccessActive.value
        viewModelScope.launch {
            serverViewModel.sendToAll(syncStateMessage(null, expiry, isActive))
        }
    }

    fun setMacroExecutionEnabled(enabled: Boolean) {
        _isMacroExecutionEnabled.value = enabled
        consoleViewModel.addLog(LogLevel.Info, "Macro execution ${if (enabled) "enabled" else "disabled"}")
    }

    private fun updateHistoryState() {
        _connectionHistory.value = ConnectionHistoryManager.getHistory()
    }

    fun onClientConnected(clientId: String, clientName: String) {
        val isTrusted = TrustedDeviceManager.isTrusted(clientId)
        _connectedDevices.update { devices ->
            if (devices.any { it.id == clientId }) {
                devices.map { if (it.id == clientId) it.copy(isTrusted = isTrusted) else it }
            } else {
                devices + ClientInfo(id = clientId, name = clientName, isTrusted = isTrusted)
            }
        }
        ConnectionHistoryManager.logEvent(clientId, clientName, "Connected")
        updateHistoryState()
        consoleViewModel.addLog(LogLevel.Info, "Client connected: $clientName ($clientId)")
        
        // Broadcast Pro status to the new client
        broadcastProStatus()
    }

    fun onClientDisconnected(clientId: String) {
        val client = _connectedDevices.value.find { it.id == clientId }
        _connectedDevices.update { it.filterNot { it.id == clientId } }
        _pendingPairingRequests.update { it.filterNot { it.id == clientId } }
        
        client?.let {
            ConnectionHistoryManager.logEvent(it.id, it.name, "Disconnected")
            updateHistoryState()
        }
        consoleViewModel.addLog(LogLevel.Info, "Client disconnected: ${client?.name ?: clientId}")
    }

    fun onPairingRequest(clientId: String, clientName: String) {
        val verificationCode = (100000..999999).random().toString()
        val metadata = serverViewModel.server.getMetadata(clientId)
        
        _pendingPairingRequests.update { requests ->
            if (requests.any { it.id == clientId }) {
                // If it exists but metadata was missing, update it
                requests.map { 
                    if (it.id == clientId && it.metadata == null && metadata != null) {
                        it.copy(metadata = metadata)
                    } else it 
                }
            } else {
                requests + ClientInfo(
                    id = clientId, 
                    name = clientName, 
                    verificationCode = verificationCode,
                    metadata = metadata
                )
            }
        }
        
        ConnectionHistoryManager.logEvent(clientId, clientName, "Pairing Request", metadata = metadata)
        updateHistoryState()
        
        consoleViewModel.addLog(LogLevel.Warn, "Pairing request from untrusted device: $clientName ($clientId). Displaying verification code and QR.")
    }

    fun approveDevice(clientId: String, clientName: String, persistent: Boolean = true) {
        val request = _pendingPairingRequests.value.find { it.id == clientId }
        if (request == null) {
            consoleViewModel.addLog(LogLevel.Error, "Attempted to approve $clientName ($clientId) but no pending request found.")
            return
        }
        
        if (!request.codeMatched) {
            consoleViewModel.addLog(LogLevel.Warn, "Attempted to approve $clientName ($clientId) before code was correctly entered.")
            return
        }

        val finalPersistent = if (settingsViewModel.allowOnceOnly.value) false else persistent
        if (finalPersistent) {
            val metadata = serverViewModel.server.getMetadata(clientId) ?: request.metadata
            TrustedDeviceManager.addTrustedDevice(clientId, clientName, metadata)
            _trustedDevices.value = TrustedDeviceManager.getTrustedDevices()
            ConnectionHistoryManager.logEvent(clientId, clientName, "Permanently Approved", metadata = metadata)
        } else {
            serverViewModel.server.approveTemporaryDevice(clientId)
            ConnectionHistoryManager.logEvent(clientId, clientName, "Temporarily Approved")
        }
        updateHistoryState()
        
        serverViewModel.server.authenticateClient(clientId)
        
        _pendingPairingRequests.update { it.filterNot { it.id == clientId } }
        _connectedDevices.update { devices ->
            devices.map { if (it.id == clientId) it.copy(isTrusted = finalPersistent) else it }
        }
        
        viewModelScope.launch {
            serverViewModel.server.sendToClient(clientId, pairingApprovedMessage())
            val macroNames = macroManagerViewModel.macroFiles.value.map { it.name }
            serverViewModel.server.sendToClient(clientId, macroListMessage(macroNames))
            serverViewModel.server.sendToClient(clientId, getCurrencyRequest())
            
            val packs = macroManagerViewModel.macroPacks.value
            if (packs.isNotEmpty()) {
                val json = Json { ignoreUnknownKeys = true }
                serverViewModel.server.sendToClient(clientId, dataMessage("installed_packs", json.encodeToString(packs).encodeToByteArray()))
            }
        }
        consoleViewModel.addLog(LogLevel.Info, "Approved device: $clientName ($clientId)")
    }

    fun denyDevice(clientId: String) {
        val request = _pendingPairingRequests.value.find { it.id == clientId }
        _pendingPairingRequests.update { it.filterNot { it.id == clientId } }
        
        viewModelScope.launch {
            serverViewModel.server.disconnectClient(clientId, "Pairing denied by user")
        }
        ConnectionHistoryManager.logEvent(clientId, request?.name ?: "Unknown", "Pairing Denied")
        updateHistoryState()
        consoleViewModel.addLog(LogLevel.Warn, "Denied pairing request from $clientId")
    }

    fun banDevice(clientId: String, clientName: String) {
        TrustedDeviceManager.banDevice(clientId, clientName)
        _bannedDevices.value = TrustedDeviceManager.getBannedDevices()
        _trustedDevices.value = TrustedDeviceManager.getTrustedDevices()
        
        viewModelScope.launch {
            serverViewModel.server.disconnectClient(clientId, "Device banned")
        }
        ConnectionHistoryManager.logEvent(clientId, clientName, "Banned")
        updateHistoryState()
        consoleViewModel.addLog(LogLevel.Error, "Banned device: $clientName ($clientId)")
    }

    fun unbanDevice(clientId: String) {
        TrustedDeviceManager.unbanDevice(clientId)
        PairingBanManager.unbanDevice(clientId)
        _bannedDevices.value = TrustedDeviceManager.getBannedDevices()
        consoleViewModel.addLog(LogLevel.Info, "Unbanned device: $clientId")
    }

    fun unbanAllDevices() {
        TrustedDeviceManager.unbanAllDevices()
        PairingBanManager.clearAllBans()
        _bannedDevices.value = TrustedDeviceManager.getBannedDevices()
        consoleViewModel.addLog(LogLevel.Info, "Unbanned all devices (including temporary bans)")
    }

    fun clearConnectionHistory() {
        ConnectionHistoryManager.clearHistory()
        updateHistoryState()
        consoleViewModel.addLog(LogLevel.Info, "Cleared connection history.")
    }

    fun removeTrustedDevice(clientId: String) {
        TrustedDeviceManager.removeTrustedDevice(clientId)
        _trustedDevices.value = TrustedDeviceManager.getTrustedDevices()
        
        // Security Fix: Explicitly disconnect and remove temporary trust to prevent session inheritance
        serverViewModel.server.removeTemporaryTrust(clientId)
        viewModelScope.launch {
            serverViewModel.server.disconnectClient(clientId, "Trust Revoked")
        }
        
        consoleViewModel.addLog(LogLevel.Info, "Removed trusted device: $clientId (Active session disconnected)")
    }

    fun onUpgradeRequest(clientId: String, jarBytes: ByteArray, hash: String, isSimulation: Boolean) {
        val client = _connectedDevices.value.find { it.id == clientId }
        val clientName = client?.name ?: "Unknown Device"

        _pendingUpdate.value = PendingUpdate(clientId, clientName, jarBytes, hash, isSimulation)
        layoutViewModel.setShowUpdateConfirmDialog(true)
        
        consoleViewModel.addLog(LogLevel.Warn, "Remote update request from $clientName ($clientId). Hash: $hash")
    }

    fun approveUpdate() {
        val update = _pendingUpdate.value ?: return
        _pendingUpdate.value = null
        layoutViewModel.setShowUpdateConfirmDialog(false)

        viewModelScope.launch(Dispatchers.IO) {
            val tempFile = File.createTempFile("upgrade", ".jar")
            tempFile.writeBytes(update.jarBytes)

            if (com.kapcode.open.macropad.kmps.desktop.logic.ServerUpdater.verifyHash(tempFile, update.hash)) {
                withContext(Dispatchers.Main) {
                    consoleViewModel.addLog(LogLevel.Info, "Update hash verified successfully.")
                }
                
                val result = com.kapcode.open.macropad.kmps.desktop.logic.ServerUpdater.applyUpdate(tempFile, update.isSimulation)
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        consoleViewModel.addLog(LogLevel.Info, result.getOrThrow())
                        serverViewModel.server.sendToClient(update.clientId, responseMessage(true, result.getOrThrow()))
                    } else {
                        consoleViewModel.addLog(LogLevel.Error, "Update failed: ${result.exceptionOrNull()?.message}")
                        serverViewModel.server.sendToClient(update.clientId, responseMessage(false, "Update failed: ${result.exceptionOrNull()?.message}"))
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    consoleViewModel.addLog(LogLevel.Error, "Update hash verification failed! Expected: ${update.hash}")
                    serverViewModel.server.sendToClient(update.clientId, responseMessage(false, "Hash mismatch. Update aborted."))
                }
            }
            tempFile.delete()
        }
    }

    fun rejectUpdate() {
        val update = _pendingUpdate.value ?: return
        _pendingUpdate.value = null
        layoutViewModel.setShowUpdateConfirmDialog(false)
        
        viewModelScope.launch {
            serverViewModel.server.sendToClient(update.clientId, responseMessage(false, "User rejected update."))
        }
        consoleViewModel.addLog(LogLevel.Warn, "Update request from ${update.clientName} was rejected by user.")
    }

    fun onDataReceived(clientId: String, dataModel: DataModel) {
        dataModel.handle(
            onData = { key, value ->
                if (key == "currency_update") {
                    try {
                        val amount = value.decodeToString().toLong()
                        _connectedDevices.update { devices ->
                            devices.map { if (it.id == clientId) it.copy(currency = amount) else it }
                        }
                        // Use Info level for currency sync to help verify it's working
                        consoleViewModel.addLog(LogLevel.Info, "Kap balance sync from $clientId: $amount")
                    } catch (e: Exception) {
                        consoleViewModel.addLog(LogLevel.Error, "Invalid currency update from $clientId")
                    }
                } else if (key == "currency_spent") {
                    try {
                        val amount = value.decodeToString().toLong()
                        AppSettings.totalCurrencySpent += amount
                        _totalCurrencySpent.value = AppSettings.totalCurrencySpent
                        consoleViewModel.addLog(LogLevel.Info, "Currency spent by $clientId: $amount (Total: ${_totalCurrencySpent.value})")
                    } catch (e: Exception) {
                        consoleViewModel.addLog(LogLevel.Error, "Invalid currency spent from $clientId")
                    }
                }
            },
            onSyncState = { sessionId, expiration, isPremium ->
                if (isPremium) {
                    consoleViewModel.addLog(LogLevel.Info, "Pro Access triggered by client $clientId")
                    ProAccessManager.triggerProAccess()
                }
            },
            onControl = { cmd, params ->
                consoleViewModel.addLog(LogLevel.Debug, "Control received from $clientId: $cmd")
                when (cmd) {
                    ControlCommand.PAIRING_RESPONSE -> {
                        val enteredCode = params["code"]
                        val pendingRequest = _pendingPairingRequests.value.find { it.id == clientId }
                        
                        if (pendingRequest != null) {
                            // 1. Log the attempt at INFO level for visibility
                            consoleViewModel.addLog(LogLevel.Info, "Pairing attempt from $clientId: enteredCode=$enteredCode, expected=${pendingRequest.verificationCode}")
                            
                            // 2. Process only if not already matched and code is valid
                            if (!pendingRequest.codeMatched && !enteredCode.isNullOrBlank()) {
                                if (pendingRequest.verificationCode == enteredCode) {
                                    consoleViewModel.addLog(LogLevel.Info, "Correct pairing code entered for $clientId. Waiting for manual approval.")
                                    _pendingPairingRequests.update { requests ->
                                        requests.map { if (it.id == clientId) it.copy(codeMatched = true) else it }
                                    }
                                    viewModelScope.launch {
                                        serverViewModel.server.sendToClient(clientId, controlMessage(ControlCommand.PAIRING_CODE_MATCHED))
                                    }
                                } else {
                                    // FLEET MODE PROTECTION: Check if the code belongs to ANY other pending device
                                    val otherPendingRequests = _pendingPairingRequests.value.filter { it.id != clientId }
                                    val matchedOther = otherPendingRequests.any { it.verificationCode == enteredCode }
                                    
                                    if (!matchedOther) {
                                        val newAttempts = pendingRequest.pairingAttempts + 1
                                        val strikeLimit = settingsViewModel.pairingStrikeLimit.value
                                        
                                        consoleViewModel.addLog(LogLevel.Warn, "Incorrect pairing code entered from $clientId (Attempt $newAttempts${if (strikeLimit > 0) "/$strikeLimit" else ""}).")
                                        
                                        if (strikeLimit > 0 && newAttempts >= strikeLimit) {
                                            val duration = settingsViewModel.pairingBanDurationMinutes.value
                                            consoleViewModel.addLog(LogLevel.Error, "Too many failed pairing attempts from $clientId. Banning device for $duration minutes.")
                                            PairingBanManager.banDevice(clientId, duration)
                                            _pendingPairingRequests.update { it.filterNot { req -> req.id == clientId } }
                                            
                                            viewModelScope.launch {
                                                serverViewModel.server.sendToClient(clientId, controlMessage(ControlCommand.BANNED, mapOf("reason" to "Temporary ban for brute-force protection ($duration min).")))
                                            }
                                        } else {
                                            _pendingPairingRequests.update { requests ->
                                                requests.map { if (it.id == clientId) it.copy(pairingAttempts = newAttempts) else it }
                                            }
                                        }
                                    } else {
                                        consoleViewModel.addLog(LogLevel.Warn, "Device $clientId submitted code for another pending device. Ignoring attempt without strike (Fleet Mode scan?).")
                                    }
                                }
                            }
                        }
                    }
                    else -> {}
                }
            },
            onText = { message ->
                consoleViewModel.addLog(LogLevel.Debug, "Text received from $clientId: $message")
                if (message == "getMacros") {
                    val isTrusted = serverViewModel.server.isDeviceTrusted(clientId)
                    if (isTrusted) {
                        val client = _connectedDevices.value.find { it.id == clientId }
                        val macroNames = macroManagerViewModel.getActiveMacrosForClient(client?.name ?: clientId, isTrusted).map { it.name }
                        viewModelScope.launch {
                            serverViewModel.server.sendToClient(clientId, macroListMessage(macroNames))
                            
                            val packs = macroManagerViewModel.macroPacks.value
                            if (packs.isNotEmpty()) {
                                val json = Json { ignoreUnknownKeys = true }
                                serverViewModel.server.sendToClient(clientId, dataMessage("installed_packs", json.encodeToString(packs).encodeToByteArray()))
                            }
                        }
                        consoleViewModel.addLog(LogLevel.Debug, "Sent macro list to $clientId")
                    } else {
                        consoleViewModel.addLog(LogLevel.Warn, "Untrusted device $clientId requested macro list. Ignored.")
                    }
                }
            },
            onCommand = { cmd, _ ->
                consoleViewModel.addLog(LogLevel.Debug, "Command received from $clientId: $cmd")
                if (cmd == "getMarketplace") {
                    // Send an empty list for now to show "Coming Soon" on Android
                    val items = emptyList<com.kapcode.open.macropad.kmps.models.MarketplaceItem>()
                    val json = Json { ignoreUnknownKeys = true }
                    viewModelScope.launch {
                        serverViewModel.server.sendToClient(clientId, dataMessage("marketplace_items", json.encodeToString(items).encodeToByteArray()))
                    }
                    consoleViewModel.addLog(LogLevel.Debug, "Sent (empty) marketplace items to $clientId to trigger 'Coming Soon' UI")
                } else if (cmd.startsWith("play:")) {
                    if (isMacroExecutionEnabled.value) {
                        val macroName = cmd.substringAfter("play:")
                        val isTrusted = serverViewModel.server.isDeviceTrusted(clientId)
                        val client = _connectedDevices.value.find { it.id == clientId }
                        val macroToPlay = macroManagerViewModel.getActiveMacrosForClient(client?.name ?: clientId, isTrusted)
                            .find { it.name.equals(macroName, ignoreCase = true) }

                        if (macroToPlay != null) {
                            macroManagerViewModel.onPlayMacro(
                                macro = macroToPlay,
                                onStart = {
                                    viewModelScope.launch {
                                        serverViewModel.server.sendToClient(clientId, DataModelBuilder().control(ControlCommand.EXECUTION_START, mapOf("macro" to macroName)).build())
                                    }
                                },
                                onComplete = {
                                    viewModelScope.launch {
                                        serverViewModel.server.sendToClient(clientId, DataModelBuilder().control(ControlCommand.EXECUTION_COMPLETE, mapOf("macro" to macroName)).build())
                                    }
                                }
                            )
                        } else {
                            consoleViewModel.addLog(LogLevel.Error, "Macro not found: $macroName")
                        }
                    } else {
                        consoleViewModel.addLog(LogLevel.Warn, "Macro execution is disabled. Ignored request for $cmd")
                    }
                }
            }
        )
    }
}
