package com.kapcode.open.macropad.kmps

import android.content.Context
import android.content.SharedPreferences
import com.kapcode.open.macropad.kmps.models.*
import com.kapcode.open.macropad.kmps.settings.AppTheme
import com.kapcode.open.macropad.kmps.settings.SlamFireTrigger
import com.kapcode.open.macropad.kmps.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

    fun saveToastsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("enable_toasts", enabled).apply()
    }

    fun getToastsEnabled(): Boolean {
        return prefs.getBoolean("enable_toasts", true)
    }

    fun saveBackgroundToastsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("enable_background_toasts", enabled).apply()
    }

    fun getBackgroundToastsEnabled(): Boolean {
        return prefs.getBoolean("enable_background_toasts", true)
    }

    fun saveDashboardMacros(widgets: List<GridWidget>) {
        val json = Json.encodeToString(widgets)
        prefs.edit().putString("dashboard_widgets", json).apply()
    }

    fun getDashboardMacros(): List<GridWidget> {
        val json = prefs.getString("dashboard_widgets", null)
        if (json != null) {
            return try { Json.decodeFromString<List<GridWidget>>(json) } catch (e: Exception) { emptyList() }
        }
        
        // Migration from old string-based dashboard
        val oldMacros = try {
            prefs.getString("dashboard_macros", "")
        } catch (e: Exception) { "" } ?: ""
        
        if (oldMacros.isNotEmpty()) {
            val list = oldMacros.split(",").mapIndexed { index, name ->
                GridWidget(
                    id = "migrated_$index",
                    macroId = name,
                    label = name,
                    color = 0xFF6200EE,
                    row = index / 2,
                    col = index % 2
                )
            }
            saveDashboardMacros(list)
            prefs.edit().remove("dashboard_macros").apply()
            return list
        }
        
        return emptyList()
    }

    fun saveServerHistory(history: List<TrustedServer>) {
        val json = Json.encodeToString(history)
        prefs.edit().putString("server_history", json).apply()
    }

    fun getServerHistory(): List<TrustedServer> {
        val json = prefs.getString("server_history", null) ?: return emptyList()
        return try {
            Json.decodeFromString<List<TrustedServer>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun bindViewModel(viewModel: SettingsViewModel, clientViewModel: ClientViewModel, scope: CoroutineScope) {
        // Load initial values
        viewModel.setTheme(getTheme())
        viewModel.setAnalyticsEnabled(getAnalyticsEnabled())
        viewModel.setMultiQrEnabled(getMultiQrEnabled())
        viewModel.setSlamFireEnabled(getSlamFireEnabled())
        viewModel.setSlamFireTrigger(getSlamFireTrigger())
        viewModel.setSlamFireSelectedMacro(getSlamFireSelectedMacro())
        viewModel.setSlamFireDoubleSelectedMacro(getSlamFireDoubleSelectedMacro())
        viewModel.setSlamFireDoubleThreshold(getSlamFireDoubleThreshold())
        viewModel.setEnableToasts(getToastsEnabled())
        viewModel.setEnableBackgroundToasts(getBackgroundToastsEnabled())
        viewModel.setServerHistory(getServerHistory())
        clientViewModel.setDashboardMacros(getDashboardMacros())

        // Sync changes back to storage
        viewModel.theme.onEach { saveTheme(it) }.launchIn(scope)
        viewModel.analyticsEnabled.onEach { saveAnalyticsEnabled(it) }.launchIn(scope)
        viewModel.multiQrEnabled.onEach { saveMultiQrEnabled(it) }.launchIn(scope)
        viewModel.slamFireEnabled.onEach { saveSlamFireEnabled(it) }.launchIn(scope)
        viewModel.slamFireTrigger.onEach { saveSlamFireTrigger(it) }.launchIn(scope)
        viewModel.slamFireSelectedMacro.onEach { saveSlamFireSelectedMacro(it) }.launchIn(scope)
        viewModel.slamFireDoubleSelectedMacro.onEach { saveSlamFireDoubleSelectedMacro(it) }.launchIn(scope)
        viewModel.slamFireDoubleThreshold.onEach { saveSlamFireDoubleThreshold(it) }.launchIn(scope)
        viewModel.enableToasts.onEach { saveToastsEnabled(it) }.launchIn(scope)
        viewModel.enableBackgroundToasts.onEach { saveBackgroundToastsEnabled(it) }.launchIn(scope)
        viewModel.serverHistory.onEach { 
            saveServerHistory(it)
            clientViewModel.setServerHistory(it)
        }.launchIn(scope)

        clientViewModel.uiState
            .map { it.dashboardMacros }
            .distinctUntilChanged()
            .onEach { saveDashboardMacros(it) }
            .launchIn(scope)
    }
}
