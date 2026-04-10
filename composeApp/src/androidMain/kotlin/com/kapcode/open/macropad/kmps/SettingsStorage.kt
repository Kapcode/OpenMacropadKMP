package com.kapcode.open.macropad.kmps

import android.content.Context
import android.content.SharedPreferences
import com.kapcode.open.macropad.kmps.settings.AppTheme
import com.kapcode.open.macropad.kmps.settings.SlamFireTrigger
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SettingsStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun saveTheme(theme: AppTheme) {
        prefs.edit().putString("app_theme", theme.name).apply()
    }

    fun getTheme(): AppTheme {
        val name = prefs.getString("app_theme", AppTheme.DarkBlue.name)
        return try { AppTheme.valueOf(name!!) } catch (e: Exception) { AppTheme.DarkBlue }
    }

    fun saveAnalyticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("analytics_enabled", enabled).apply()
    }

    fun getAnalyticsEnabled(): Boolean {
        return prefs.getBoolean("analytics_enabled", false)
    }

    fun saveMultiQrEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("multi_qr_enabled", enabled).apply()
    }

    fun getMultiQrEnabled(): Boolean {
        return prefs.getBoolean("multi_qr_enabled", false)
    }

    fun saveSlamFireEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("slam_fire_enabled", enabled).apply()
    }

    fun getSlamFireEnabled(): Boolean {
        return prefs.getBoolean("slam_fire_enabled", true)
    }

    fun saveSlamFireTrigger(trigger: SlamFireTrigger) {
        prefs.edit().putString("slam_fire_trigger", trigger.name).apply()
    }

    fun getSlamFireTrigger(): SlamFireTrigger {
        val name = prefs.getString("slam_fire_trigger", SlamFireTrigger.VolumeDown.name)
        return try { SlamFireTrigger.valueOf(name!!) } catch (e: Exception) { SlamFireTrigger.VolumeDown }
    }

    fun saveSlamFireSelectedMacro(macro: String?) {
        prefs.edit().putString("slam_fire_macro", macro).apply()
    }

    fun getSlamFireSelectedMacro(): String? {
        return prefs.getString("slam_fire_macro", null)
    }

    fun saveSlamFireDoubleSelectedMacro(macro: String?) {
        prefs.edit().putString("slam_fire_double_macro", macro).apply()
    }

    fun getSlamFireDoubleSelectedMacro(): String? {
        return prefs.getString("slam_fire_double_macro", null)
    }

    fun saveSlamFireDoubleThreshold(threshold: Long) {
        prefs.edit().putLong("slam_fire_threshold", threshold).apply()
    }

    fun getSlamFireDoubleThreshold(): Long {
        return prefs.getLong("slam_fire_threshold", 300L)
    }

    fun bindViewModel(viewModel: SettingsViewModel, scope: CoroutineScope) {
        // Load initial values
        viewModel.setTheme(getTheme())
        viewModel.setAnalyticsEnabled(getAnalyticsEnabled())
        viewModel.setMultiQrEnabled(getMultiQrEnabled())
        viewModel.setSlamFireEnabled(getSlamFireEnabled())
        viewModel.setSlamFireTrigger(getSlamFireTrigger())
        viewModel.setSlamFireSelectedMacro(getSlamFireSelectedMacro())
        viewModel.setSlamFireDoubleSelectedMacro(getSlamFireDoubleSelectedMacro())
        viewModel.setSlamFireDoubleThreshold(getSlamFireDoubleThreshold())

        // Sync changes back to storage
        viewModel.theme.onEach { saveTheme(it) }.launchIn(scope)
        viewModel.analyticsEnabled.onEach { saveAnalyticsEnabled(it) }.launchIn(scope)
        viewModel.multiQrEnabled.onEach { saveMultiQrEnabled(it) }.launchIn(scope)
        viewModel.slamFireEnabled.onEach { saveSlamFireEnabled(it) }.launchIn(scope)
        viewModel.slamFireTrigger.onEach { saveSlamFireTrigger(it) }.launchIn(scope)
        viewModel.slamFireSelectedMacro.onEach { saveSlamFireSelectedMacro(it) }.launchIn(scope)
        viewModel.slamFireDoubleSelectedMacro.onEach { saveSlamFireDoubleSelectedMacro(it) }.launchIn(scope)
        viewModel.slamFireDoubleThreshold.onEach { saveSlamFireDoubleThreshold(it) }.launchIn(scope)
    }
}
