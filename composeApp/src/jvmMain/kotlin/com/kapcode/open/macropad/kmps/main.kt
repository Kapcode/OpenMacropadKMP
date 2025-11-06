package com.kapcode.open.macropad.kmps

import ActiveMacroManager
import MacroPlayer
import ServerStatusUI
import UI.ConsoleUI
import UI.InspectorUI
import UI.MacroManagerUI
import UI.TabbedUI
import com.github.kwhat.jnativehook.GlobalScreen
import com.formdev.flatlaf.FlatDarkLaf
import com.kapcode.open.macropad.kmps.UI.ConnectedDevicesUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

fun main() {
    // Apply the FlatLaf dark theme before creating any UI components
    try {
        UIManager.setLookAndFeel(FlatDarkLaf())
    } catch (ex: Exception) {
        System.err.println("Failed to initialize LaF")
    }

    SwingUtilities.invokeLater {
        ToolTipManager.sharedInstance().initialDelay = 0
        ToolTipManager.sharedInstance().dismissDelay = 5000
        createAndShowGUI()
    }
}

fun createAndShowGUI() {
    val wifiServer = WifiServer()
    val frame = JFrame("Open Macropad Server")
    frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
    frame.extendedState = JFrame.MAXIMIZED_BOTH
    frame.minimumSize = Dimension(200, 200)
    frame.setLocationRelativeTo(null)

    // --- Component Creation ---
    val serverStatusUI = ServerStatusUI()
    val connectedDevicesUI = ConnectedDevicesUI()
    val inspectorUI = InspectorUI()
    val consoleUI = ConsoleUI(wifiServer)
    val tabbedUI = TabbedUI(frame)
    val macroPlayer = MacroPlayer()
    val activeMacroManager = ActiveMacroManager(macroPlayer)
    val macroManagerUI = MacroManagerUI(tabbedUI, activeMacroManager, macroPlayer)

    // --- Connection Listener Setup ---
    val connectionListener = object : ConnectionListener {
        override fun onClientConnected(clientId: String, clientName: String) {
            connectedDevicesUI.addDevice(clientId, clientName)
        }
        override fun onClientDisconnected(clientId: String) {
            connectedDevicesUI.removeDevice(clientId)
        }
        override fun onDataReceived(clientId: String, data: ByteArray) {}
        override fun onError(error: String) {
            JOptionPane.showMessageDialog(frame, error, "Server Error", JOptionPane.ERROR_MESSAGE)
        }
    }
    wifiServer.setConnectionListener(connectionListener)

    // --- Layout Panels ---
    val bottomPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, connectedDevicesUI, consoleUI)
    bottomPane.setDividerLocation(250)
    
    val rightSidePane = JSplitPane(JSplitPane.VERTICAL_SPLIT, inspectorUI, bottomPane)
    rightSidePane.setDividerLocation(300)

    val topHorizontalPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, macroManagerUI, tabbedUI)
    topHorizontalPane.setDividerLocation(300)
    
    val mainSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, serverStatusUI, topHorizontalPane)
    
    // --- Root Layout Fix ---
    val rootSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainSplitPane, rightSidePane)
    rootSplitPane.setDividerLocation(frame.width - 500) // Adjust initial divider
    frame.add(rootSplitPane, BorderLayout.CENTER)
    // --- End Root Layout Fix ---

    // --- Window Closing Logic ---
    frame.addWindowListener(object : java.awt.event.WindowAdapter() {
        override fun windowClosing(e: java.awt.event.WindowEvent?) {
            activeMacroManager.shutdown()
            GlobalScreen.unregisterNativeHook()
            frame.dispose()
            System.exit(0)
        }
    })

    // --- Start Server & Show Frame ---
    wifiServer.startListening()
    serverStatusUI.updateStatus(wifiServer.isListening(), 9999)
    frame.isVisible = true
}