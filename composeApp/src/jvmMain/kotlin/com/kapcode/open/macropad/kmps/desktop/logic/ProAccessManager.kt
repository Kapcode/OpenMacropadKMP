package com.kapcode.open.macropad.kmps.desktop.logic

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages the global "Pro Access" state for the server.
 * When a Pro device connects, the server and all clients have Pro access for 12 hours.
 */
object ProAccessManager {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var updateJob: Job? = null

    private val _isProAccessActive = MutableStateFlow(false)
    val isProAccessActive = _isProAccessActive.asStateFlow()

    private val _proAccessTimeRemaining = MutableStateFlow(0L)
    val proAccessTimeRemaining = _proAccessTimeRemaining.asStateFlow()

    init {
        checkStatus()
        startUpdateLoop()
    }

    private fun checkStatus() {
        val expiry = AppSettings.globalProExpiry
        val now = System.currentTimeMillis()
        _isProAccessActive.value = expiry > now
        _proAccessTimeRemaining.value = (expiry - now).coerceAtLeast(0L)
    }

    private fun startUpdateLoop() {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (true) {
                checkStatus()
                delay(1000) // Update every second
            }
        }
    }

    fun triggerProAccess() {
        val newExpiry = System.currentTimeMillis() + (12 * 60 * 60 * 1000) // 12 Hours
        AppSettings.globalProExpiry = newExpiry
        checkStatus()
    }

    fun revokeProAccess() {
        AppSettings.globalProExpiry = 0
        checkStatus()
    }
}
