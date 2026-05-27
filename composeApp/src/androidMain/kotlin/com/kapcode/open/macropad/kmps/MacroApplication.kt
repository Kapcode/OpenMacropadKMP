package com.kapcode.open.macropad.kmps

import android.app.Application
import android.util.Log
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel

class MacroApplication : Application() {
    companion object {
        const val TAG = "MacroPerf"
        val appStartTime = System.currentTimeMillis()
        lateinit var instance: MacroApplication
            private set
        
        val settingsViewModel by lazy { SettingsViewModel() }
        lateinit var analyticsManager: AnalyticsManager
            private set

        init {
            Log.i(TAG, "0ms: [Application] Static initialization started")
        }
    }

    override fun onCreate() {
        val start = System.currentTimeMillis() - appStartTime
        Log.i(TAG, "${start}ms: [Application] onCreate started")
        super.onCreate()
        instance = this
        
        analyticsManager = AnalyticsManager(this, settingsViewModel)

        // Warm up SharedPreferences on a background thread
        // This makes TokenManager and other pref-based logic "instant" later
        Thread {
            getSharedPreferences("token_prefs", MODE_PRIVATE)
        }.start()

        val end = System.currentTimeMillis() - appStartTime
        Log.i(TAG, "${end}ms: [Application] onCreate finished")
    }
}
