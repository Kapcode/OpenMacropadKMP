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

    init {
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        if (configFile.exists()) {
            FileInputStream(configFile).use { properties.load(it) }
        }
    }

    var macroDirectory: String
        get() = properties.getProperty(MACRO_DIR_KEY, configDir.absolutePath + File.separator + "Macros")
        set(value) {
            properties.setProperty(MACRO_DIR_KEY, value)
            save()
        }

    private fun save() {
        FileOutputStream(configFile).use { properties.store(it, "OpenMacropadServer Settings") }
    }
}