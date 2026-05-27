package com.kapcode.open.macropad.kmps

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.os.Build
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AnalyticsManager(private val context: Context, private val settingsViewModel: SettingsViewModel) {
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        settingsViewModel.analyticsEnabled.onEach { enabled ->
            if (enabled) {
                if (firebaseAnalytics == null) {
                    firebaseAnalytics = FirebaseAnalytics.getInstance(context)
                    setupUserProperties()
                }
                firebaseAnalytics?.setAnalyticsCollectionEnabled(true)
                Log.i("AnalyticsManager", "Firebase Analytics enabled")
            } else {
                firebaseAnalytics?.setAnalyticsCollectionEnabled(false)
                Log.i("AnalyticsManager", "Firebase Analytics disabled")
            }
        }.launchIn(scope)
        
        // Listen for Pro status changes
        settingsViewModel.isPro.onEach { updateProStatus() }.launchIn(scope)
        settingsViewModel.isServerProActive.onEach { updateProStatus() }.launchIn(scope)
    }

    private fun setupUserProperties() {
        firebaseAnalytics?.apply {
            setUserProperty("device_model", Build.MODEL)
            setUserProperty("device_manufacturer", Build.MANUFACTURER)
            setUserProperty("android_version", Build.VERSION.RELEASE)
            setUserProperty("cpu_abi", Build.SUPPORTED_ABIS.joinToString(","))
            setUserProperty("hardware", Build.HARDWARE)
            setUserProperty("board", Build.BOARD)
            
            // RAM info
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            val totalRamGb = "%.2f".format(memInfo.totalMem / (1024.0 * 1024.0 * 1024.0))
            setUserProperty("total_ram_gb", totalRamGb)
            
            // Initial Pro status
            updateProStatus()
        }
    }

    private fun updateProStatus() {
        val isLocalPro = settingsViewModel.isPro.value
        val isServerPro = settingsViewModel.isServerProActive.value
        val status = when {
            isLocalPro -> "Local Pro"
            isServerPro -> "Server Pro"
            else -> "Free"
        }
        firebaseAnalytics?.setUserProperty("max_pro_status", status)
        Log.d("AnalyticsManager", "Updated max_pro_status to: $status")
    }

    fun trackScreen(screenName: String, screenClass: String? = null) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            if (screenClass != null) {
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            }
        }
        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        Log.d("AnalyticsManager", "Tracked screen: $screenName")
    }

    fun trackEvent(name: String, params: Map<String, String> = emptyMap()) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                putString(key, value)
            }
        }
        firebaseAnalytics?.logEvent(name, bundle)
        Log.d("AnalyticsManager", "Tracked event: $name with params $params")
    }
}
