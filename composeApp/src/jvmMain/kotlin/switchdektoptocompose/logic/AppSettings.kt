package switchdektoptocompose.logic

import switchdektoptocompose.utils.ProjectPaths
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

object AppSettings {
    private val configDir = ProjectPaths.configDir
    private val configFile = File(configDir, "config.properties")
    private val properties = Properties()

    private const val WINDOW_PLACEMENT_MODE_KEY = "window.placement.mode"
    private const val WINDOW_MONITOR_INDEX_KEY = "window.monitor.index"
    private const val WINDOW_POLLING_RATE_KEY = "window.polling.rate"
    private const val MOUSE_POLLING_RATE_KEY = "mouse.polling.rate"
    private const val SYSTEM_POLLING_RATE_KEY = "system.polling.rate"
    private const val CONTROLLER_NAVIGATION_MODE_KEY = "controller.navigation.mode"

    private const val MACRO_DIR_KEY = "macroDirectory"
    private const val SERVER_PORT_KEY = "serverPort"
    private const val SECURE_SERVER_PORT_KEY = "secureServerPort"
    private const val ESTOP_KEY_KEY = "eStopKey"
    private const val EXIT_BEHAVIOR_KEY = "exitBehavior"
    private const val CLICK_TRAY_TO_TOGGLE_KEY = "clickTrayToToggle"
    private const val ANIMATE_TO_TRAY_KEY = "animateToTray"
    private const val HARD_ESTOP_KEY = "hardEstop"
    private const val ALLOW_NEW_CONNECTIONS_KEY = "allowNewConnections"
    private const val ALLOW_ONCE_ONLY_KEY = "allowOnceOnly"
    private const val FLEET_MODE_ENABLED_KEY = "fleetModeEnabled"
    private const val ENABLE_WEBSOCKET_PINGS_KEY = "enableWebsocketPings"
    private const val FLEET_GRID_VISIBILITY_KEY = "fleetGridVisibility"
    private const val DEFAULT_PAIRING_MODE_QR_KEY = "defaultPairingModeQr"
    private const val PAIRING_BAN_DURATION_MINUTES_KEY = "pairingBanDurationMinutes"
    private const val PAIRING_STRIKE_LIMIT_KEY = "pairingStrikeLimit"
    private const val TOTAL_CURRENCY_SPENT_KEY = "totalCurrencySpent"
    private const val GLOBAL_PRO_EXPIRY_KEY = "globalProExpiry"

    // Connected Clients Settings
    private const val CLIENT_THEME_KEY = "clientTheme"
    private const val CLIENT_ANALYTICS_ENABLED_KEY = "clientAnalyticsEnabled"
    private const val CLIENT_SLAM_FIRE_ENABLED_KEY = "clientSlamFireEnabled"
    private const val CLIENT_SLAM_FIRE_ACTION_KEY = "clientSlamFireAction"

    // Shortcuts
    private const val COPY_CONSOLE_OUTPUT_SHORTCUT_KEY = "copyConsoleOutputShortcut"
    private const val STOP_KEY_SHORTCUT_KEY = "stopKeyShortcut"
    private const val INSPECT_KEY_SHORTCUT_KEY = "inspectKeyShortcut"
    private const val SPLITTER_SWAP_MODIFIER_KEY = "splitterSwapModifier"
    private const val SPLITTER_INFO_MODIFIER_KEY = "splitterInfoModifier"

    private const val SPLITTER_POS_PREFIX = "splitter_pos_"
    private const val SPLITTER_SWAPPED_PREFIX = "splitter_swapped_"

    private const val TOOLTIP_X_OFFSET_KEY = "tooltipXOffset"
    private const val TOOLTIP_Y_OFFSET_KEY = "tooltipYOffset"
    private const val ENABLE_PACK_SWITCH_NOTIFICATIONS_KEY = "enablePackSwitchNotifications"
    private const val ENABLE_TOASTS_KEY = "enableToasts"
    private const val ENABLE_BACKGROUND_TOASTS_KEY = "enableBackgroundToasts"
    private const val ENABLE_NETWORK_TOASTS_KEY = "enableNetworkToasts"
    private const val INCLUDE_WINDOW_NAMES_IN_TOASTS_KEY = "includeWindowNamesInToasts"
    private const val TOAST_DURATION_MS_KEY = "toastDurationMs"
    private const val TOAST_TARGET_KEY = "toastTarget"
    private const val NOTIFICATION_CLIENT_IDS_KEY = "notificationClientIds"

    init {
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        if (configFile.exists()) {
            FileInputStream(configFile).use { properties.load(it) }
        }

        // Set a Linux-specific default macro directory if none is set
        val os = System.getProperty("os.name").lowercase()
        if (os.contains("linux") && properties.getProperty(MACRO_DIR_KEY).isNullOrBlank()) {
            val linuxDefaultDir = ProjectPaths.linuxMacroDir
            properties.setProperty(MACRO_DIR_KEY, linuxDefaultDir.absolutePath)
            save()
        }
    }

    var macroDirectory: String
        get() = properties.getProperty(MACRO_DIR_KEY, configDir.absolutePath + File.separator + "Macros")
        set(value) {
            properties.setProperty(MACRO_DIR_KEY, value)
            save()
        }

    var serverPort: Int
        get() = properties.getProperty(SERVER_PORT_KEY, "8090").toIntOrNull() ?: 8090
        set(value) {
            properties.setProperty(SERVER_PORT_KEY, value.toString())
            save()
        }

    var secureServerPort: Int
        get() = properties.getProperty(SECURE_SERVER_PORT_KEY, "8449").toIntOrNull() ?: 8449
        set(value) {
            properties.setProperty(SECURE_SERVER_PORT_KEY, value.toString())
            save()
        }
    
    var eStopKey: String
        get() = properties.getProperty(ESTOP_KEY_KEY, "F12")
        set(value) {
            properties.setProperty(ESTOP_KEY_KEY, value)
            save()
        }

    var exitBehavior: String
        get() = properties.getProperty(EXIT_BEHAVIOR_KEY, "ASK")
        set(value) {
            properties.setProperty(EXIT_BEHAVIOR_KEY, value)
            save()
        }

    var clickTrayToToggle: Boolean
        get() = properties.getProperty(CLICK_TRAY_TO_TOGGLE_KEY, "false").toBoolean()
        set(value) {
            properties.setProperty(CLICK_TRAY_TO_TOGGLE_KEY, value.toString())
            save()
        }

    var animateToTray: Boolean
        get() = properties.getProperty(ANIMATE_TO_TRAY_KEY, "false").toBoolean()
        set(value) {
            properties.setProperty(ANIMATE_TO_TRAY_KEY, value.toString())
            save()
        }

    var hardEstop: Boolean
        get() = properties.getProperty(HARD_ESTOP_KEY, "true").toBoolean()
        set(value) {
            properties.setProperty(HARD_ESTOP_KEY, value.toString())
            save()
        }

    var allowNewConnections: Boolean
        get() = properties.getProperty(ALLOW_NEW_CONNECTIONS_KEY, "true").toBoolean()
        set(value) {
            properties.setProperty(ALLOW_NEW_CONNECTIONS_KEY, value.toString())
            save()
        }

    var allowOnceOnly: Boolean
        get() = properties.getProperty(ALLOW_ONCE_ONLY_KEY, "false").toBoolean()
        set(value) {
            properties.setProperty(ALLOW_ONCE_ONLY_KEY, value.toString())
            save()
        }

    var fleetModeEnabled: Boolean
        get() = properties.getProperty(FLEET_MODE_ENABLED_KEY, "false").toBoolean()
        set(value) {
            properties.setProperty(FLEET_MODE_ENABLED_KEY, value.toString())
            save()
        }

    var enableWebsocketPings: Boolean
        get() = properties.getProperty(ENABLE_WEBSOCKET_PINGS_KEY, "true").toBoolean()
        set(value) {
            properties.setProperty(ENABLE_WEBSOCKET_PINGS_KEY, value.toString())
            save()
        }

    var fleetGridVisibility: String
        get() = properties.getProperty(FLEET_GRID_VISIBILITY_KEY, "1,1,1,1,1,1")
        set(value) {
            properties.setProperty(FLEET_GRID_VISIBILITY_KEY, value)
            save()
        }

    var defaultPairingModeQr: Boolean
        get() = properties.getProperty(DEFAULT_PAIRING_MODE_QR_KEY, "true").toBoolean()
        set(value) {
            properties.setProperty(DEFAULT_PAIRING_MODE_QR_KEY, value.toString())
            save()
        }

    var pairingBanDurationMinutes: Int
        get() = properties.getProperty(PAIRING_BAN_DURATION_MINUTES_KEY, "15").toIntOrNull() ?: 15
        set(value) {
            properties.setProperty(PAIRING_BAN_DURATION_MINUTES_KEY, value.toString())
            save()
        }

    var pairingStrikeLimit: Int
        get() = properties.getProperty(PAIRING_STRIKE_LIMIT_KEY, "6").toIntOrNull() ?: 6
        set(value) {
            properties.setProperty(PAIRING_STRIKE_LIMIT_KEY, value.toString())
            save()
        }

    var totalCurrencySpent: Long
        get() = properties.getProperty(TOTAL_CURRENCY_SPENT_KEY, "0").toLongOrNull() ?: 0L
        set(value) {
            properties.setProperty(TOTAL_CURRENCY_SPENT_KEY, value.toString())
            save()
        }

    var globalProExpiry: Long
        get() = properties.getProperty(GLOBAL_PRO_EXPIRY_KEY, "0").toLongOrNull() ?: 0L
        set(value) {
            properties.setProperty(GLOBAL_PRO_EXPIRY_KEY, value.toString())
            save()
        }

    // Connected Clients Settings
    var clientTheme: String
        get() = properties.getProperty(CLIENT_THEME_KEY, "Dark Blue")
        set(value) {
            properties.setProperty(CLIENT_THEME_KEY, value)
            save()
        }

    var clientAnalyticsEnabled: Boolean
        get() = properties.getProperty(CLIENT_ANALYTICS_ENABLED_KEY, "true").toBoolean()
        set(value) {
            properties.setProperty(CLIENT_ANALYTICS_ENABLED_KEY, value.toString())
            save()
        }

    var clientSlamFireEnabled: Boolean
        get() = properties.getProperty(CLIENT_SLAM_FIRE_ENABLED_KEY, "false").toBoolean()
        set(value) {
            properties.setProperty(CLIENT_SLAM_FIRE_ENABLED_KEY, value.toString())
            save()
        }

    var clientSlamFireAction: String
        get() = properties.getProperty(CLIENT_SLAM_FIRE_ACTION_KEY, "None")
        set(value) {
            properties.setProperty(CLIENT_SLAM_FIRE_ACTION_KEY, value)
            save()
        }

    // Shortcuts
    var copyConsoleOutputShortcut: String
        get() = properties.getProperty(COPY_CONSOLE_OUTPUT_SHORTCUT_KEY, "Ctrl+Shift+C")
        set(value) {
            properties.setProperty(COPY_CONSOLE_OUTPUT_SHORTCUT_KEY, value)
            save()
        }

    var stopKeyShortcut: String
        get() = properties.getProperty(STOP_KEY_SHORTCUT_KEY, "Escape")
        set(value) {
            properties.setProperty(STOP_KEY_SHORTCUT_KEY, value)
            save()
        }

    var inspectKeyShortcut: String
        get() = properties.getProperty(INSPECT_KEY_SHORTCUT_KEY, "F10")
        set(value) {
            properties.setProperty(INSPECT_KEY_SHORTCUT_KEY, value)
            save()
        }

    var splitterSwapModifier: String
        get() = properties.getProperty(SPLITTER_SWAP_MODIFIER_KEY, "Shift")
        set(value) {
            properties.setProperty(SPLITTER_SWAP_MODIFIER_KEY, value)
            save()
        }

    var splitterInfoModifier: String
        get() = properties.getProperty(SPLITTER_INFO_MODIFIER_KEY, "Ctrl")
        set(value) {
            properties.setProperty(SPLITTER_INFO_MODIFIER_KEY, value)
            save()
        }

    var tooltipXOffset: Int
        get() = properties.getProperty(TOOLTIP_X_OFFSET_KEY, "0").toIntOrNull() ?: 0
        set(value) {
            properties.setProperty(TOOLTIP_X_OFFSET_KEY, value.toString())
            save()
        }

    var tooltipYOffset: Int
        get() = properties.getProperty(TOOLTIP_Y_OFFSET_KEY, "-24").toIntOrNull() ?: -24
        set(value) {
            properties.setProperty(TOOLTIP_Y_OFFSET_KEY, value.toString())
            save()
        }

    var enablePackSwitchNotifications: Boolean
        get() = properties.getProperty(ENABLE_PACK_SWITCH_NOTIFICATIONS_KEY, "true").toBoolean()
        set(value) {
            properties.setProperty(ENABLE_PACK_SWITCH_NOTIFICATIONS_KEY, value.toString())
            save()
        }

    var enableToasts: Boolean
        get() = properties.getProperty(ENABLE_TOASTS_KEY, "true").toBoolean()
        set(value) {
            properties.setProperty(ENABLE_TOASTS_KEY, value.toString())
            save()
        }

    var enableBackgroundToasts: Boolean
        get() = properties.getProperty(ENABLE_BACKGROUND_TOASTS_KEY, "true").toBoolean()
        set(value) {
            properties.setProperty(ENABLE_BACKGROUND_TOASTS_KEY, value.toString())
            save()
        }

    var enableNetworkToasts: Boolean
        get() = properties.getProperty(ENABLE_NETWORK_TOASTS_KEY, "true").toBoolean()
        set(value) {
            properties.setProperty(ENABLE_NETWORK_TOASTS_KEY, value.toString())
            save()
        }

    var includeWindowNamesInToasts: Boolean
        get() = properties.getProperty(INCLUDE_WINDOW_NAMES_IN_TOASTS_KEY, "true").toBoolean()
        set(value) {
            properties.setProperty(INCLUDE_WINDOW_NAMES_IN_TOASTS_KEY, value.toString())
            save()
        }

    var toastDurationMs: Long
        get() = properties.getProperty(TOAST_DURATION_MS_KEY, "3000").toLongOrNull() ?: 3000L
        set(value) {
            properties.setProperty(TOAST_DURATION_MS_KEY, value.toString())
            save()
        }

    var toastTarget: String
        get() = properties.getProperty(TOAST_TARGET_KEY, "BOTH")
        set(value) {
            properties.setProperty(TOAST_TARGET_KEY, value)
            save()
        }

    var notificationClientIds: String
        get() = properties.getProperty(NOTIFICATION_CLIENT_IDS_KEY, "")
        set(value) {
            properties.setProperty(NOTIFICATION_CLIENT_IDS_KEY, value)
            save()
        }

    fun getSplitterPosition(name: String, default: Float): Float {
        return properties.getProperty(SPLITTER_POS_PREFIX + name, default.toString()).toFloatOrNull() ?: default
    }

    fun setSplitterPosition(name: String, position: Float) {
        properties.setProperty(SPLITTER_POS_PREFIX + name, position.toString())
        save()
    }

    fun getSplitterSwapped(name: String, default: Boolean): Boolean {
        return properties.getProperty(SPLITTER_SWAPPED_PREFIX + name, default.toString()).toBoolean()
    }

    fun setSplitterSwapped(name: String, swapped: Boolean) {
        properties.setProperty(SPLITTER_SWAPPED_PREFIX + name, swapped.toString())
        save()
    }

    var windowPlacementMode: String
        get() = properties.getProperty(WINDOW_PLACEMENT_MODE_KEY, "CURSOR")
        set(value) {
            properties.setProperty(WINDOW_PLACEMENT_MODE_KEY, value)
            save()
        }

    var windowMonitorIndex: Int
        get() = properties.getProperty(WINDOW_MONITOR_INDEX_KEY, "0").toIntOrNull() ?: 0
        set(value) {
            properties.setProperty(WINDOW_MONITOR_INDEX_KEY, value.toString())
            save()
        }

    var windowPollingRate: Long
        get() = properties.getProperty(WINDOW_POLLING_RATE_KEY, "250").toLongOrNull() ?: 250L
        set(value) {
            properties.setProperty(WINDOW_POLLING_RATE_KEY, value.toString())
            save()
        }

    var mousePollingRate: Long
        get() = properties.getProperty(MOUSE_POLLING_RATE_KEY, "100").toLongOrNull() ?: 100L
        set(value) {
            properties.setProperty(MOUSE_POLLING_RATE_KEY, value.toString())
            save()
        }

    var systemPollingRate: Long
        get() = properties.getProperty(SYSTEM_POLLING_RATE_KEY, "1000").toLongOrNull() ?: 1000L
        set(value) {
            properties.setProperty(SYSTEM_POLLING_RATE_KEY, value.toString())
            save()
        }

    var controllerNavigationMode: String
        get() = properties.getProperty(CONTROLLER_NAVIGATION_MODE_KEY, "TRAVERSAL")
        set(value) {
            properties.setProperty(CONTROLLER_NAVIGATION_MODE_KEY, value)
            save()
        }

    fun clearAll() {
        properties.clear()
        save()
    }

    private fun save() {
        FileOutputStream(configFile).use { properties.store(it, "MacroKapServer Settings") }
    }
}