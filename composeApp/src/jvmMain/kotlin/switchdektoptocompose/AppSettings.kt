package switchdektoptocompose

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

object AppSettings {
    private val configDir = File(System.getProperty("user.home"), "Documents/OpenMacropadServer")
    private val configFile = File(configDir, "config.properties")
    private val properties = Properties()

    private const val MACRO_DIR_KEY = "macroDirectory"
    private const val SERVER_PORT_KEY = "serverPort"
    private const val SECURE_SERVER_PORT_KEY = "secureServerPort"
    private const val ESTOP_KEY_KEY = "eStopKey"

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
            val linuxDefaultDir = File(System.getProperty("user.home"), ".config/OpenMacropadKMP/macros")
            if (!linuxDefaultDir.exists()) {
                linuxDefaultDir.mkdirs()
            }
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

    private fun save() {
        FileOutputStream(configFile).use { properties.store(it, "OpenMacropadServer Settings") }
    }
}