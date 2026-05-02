package com.kapcode.open.macropad.kmps

import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun openFolder(path: String) {
    if (Desktop.isDesktopSupported()) {
        val desktop = Desktop.getDesktop()
        val folder = File(path)
        if (folder.exists()) {
            desktop.open(folder)
        }
    }
}

actual fun pickDirectory(onResult: (String?) -> Unit) {
    SwingUtilities.invokeLater {
        val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Select Directory"
        }
        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            onResult(fileChooser.selectedFile.absolutePath)
        } else {
            onResult(null)
        }
    }
}
