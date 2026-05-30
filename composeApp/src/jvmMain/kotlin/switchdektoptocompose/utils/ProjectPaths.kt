package switchdektoptocompose.utils

import java.io.File

object ProjectPaths {
    val workingDir: File by lazy {
        val newDir = File(System.getProperty("user.home"), ".macrokap")
        val oldDir = File(System.getProperty("user.home"), ".openmacropad")
        
        if (!newDir.exists() && oldDir.exists()) {
            println("Migrating working directory from ${oldDir.absolutePath} to ${newDir.absolutePath}")
            oldDir.renameTo(newDir)
        }
        
        if (!newDir.exists()) {
            newDir.mkdirs()
        }
        newDir
    }

    val configDir: File by lazy {
        val newDir = File(System.getProperty("user.home"), "Documents/MacroKapServer")
        val oldDir = File(System.getProperty("user.home"), "Documents/OpenMacropadServer")

        if (!newDir.exists() && oldDir.exists()) {
            println("Migrating configuration directory from ${oldDir.absolutePath} to ${newDir.absolutePath}")
            oldDir.renameTo(newDir)
        }

        if (!newDir.exists()) {
            newDir.mkdirs()
        }
        newDir
    }
    
    val linuxMacroDir: File by lazy {
        val newDir = File(System.getProperty("user.home"), ".config/MacroKapKMP/macros")
        val oldDir = File(System.getProperty("user.home"), ".config/OpenMacropadKMP/macros")

        if (!newDir.exists() && oldDir.exists()) {
            println("Migrating Linux macro directory from ${oldDir.absolutePath} to ${newDir.absolutePath}")
            // We need to make sure the parent exists
            newDir.parentFile.mkdirs()
            oldDir.renameTo(newDir)
        }

        if (!newDir.exists()) {
            newDir.mkdirs()
        }
        newDir
    }
}
