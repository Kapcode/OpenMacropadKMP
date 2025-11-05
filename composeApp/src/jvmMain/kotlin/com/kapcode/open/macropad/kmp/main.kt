import UI.*
import com.formdev.flatlaf.FlatDarkLaf
import java.awt.AWTEvent
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import com.kapcode.open.macropad.kmp.WifiServer

fun main() {
    SwingUtilities.invokeLater {
        try {
            UIManager.setLookAndFeel(FlatDarkLaf())
        } catch (ex: Exception) {
            System.err.println("Failed to initialize LaF")
        }

        // Make tooltips appear and disappear instantly
        ToolTipManager.sharedInstance().initialDelay = 0
        ToolTipManager.sharedInstance().dismissDelay = 5000 // Dismiss after 5 seconds

        createAndShowGUI()
    }
}

fun createAndShowGUI() {
    val wifiServer = WifiServer()
    val frame = JFrame("Open Macropad Server")
    frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE // We will handle closing manually
    frame.extendedState = JFrame.MAXIMIZED_BOTH // Maximize the window
    frame.minimumSize = Dimension(200, 200) // Set minimum frame size
    frame.setLocationRelativeTo(null)

    val menuBar = JMenuBar()
    val serverMenu = JMenu("Server")
    val startItem = JMenuItem("Start")
    startItem.toolTipText = "Start the server"
    val stopItem = JMenuItem("Stop")
    stopItem.toolTipText = "Stop the server"
    val settingsItem = JMenuItem("Settings")
    settingsItem.toolTipText = "Open server settings"
    serverMenu.add(startItem)
    serverMenu.add(stopItem)
    serverMenu.addSeparator()
    serverMenu.add(settingsItem)
    menuBar.add(serverMenu)

    val macroMenu = JMenu("Macro Manager")
    val macroSettingsItem = JMenuItem("Settings")
    macroSettingsItem.toolTipText = "Open macro manager settings"
    macroMenu.add(macroSettingsItem)
    menuBar.add(macroMenu)

    frame.jMenuBar = menuBar

    val serverStatusUI = ServerStatusUI()
    val inspectorUI = InspectorUI()
    val consoleUI = ConsoleUI(wifiServer)
    val connectedDevicesUI = ConnectedDevicesUI()
    val tabbedUI = TabbedUI(frame) // Pass frame to TabbedUI
    val macroPlayer = MacroPlayer() // Create MacroPlayer instance
    val activeMacroManager = ActiveMacroManager(macroPlayer) // Create ActiveMacroManager
    val macroManagerUI = MacroManagerUI(tabbedUI, activeMacroManager, macroPlayer) // Pass MacroPlayer

    // Add a WindowListener to handle closing the application
    frame.addWindowListener(object : WindowAdapter() {
        override fun windowClosing(e: WindowEvent?) {
            for (i in 0 until tabbedUI.tabCount) {
                val editor = tabbedUI.getComponentAt(i) as? MacroJsonEditorUI
                if (editor?.hasUnsavedChanges == true) {
                    val choice = JOptionPane.showConfirmDialog(
                        frame,
                        "Save changes to ${tabbedUI.getTitleForComponent(editor)}?",
                        "Unsaved Changes",
                        JOptionPane.YES_NO_CANCEL_OPTION
                    )
                    when (choice) {
                        JOptionPane.YES_OPTION -> editor.save(tabbedUI.getTitleForComponent(editor))
                        JOptionPane.NO_OPTION -> { /* Do nothing, just proceed */ }
                        JOptionPane.CANCEL_OPTION -> return // Abort the close operation
                        JOptionPane.CLOSED_OPTION -> return // Abort the close operation
                    }
                }
            }
            // If we reach here, all tabs are handled, so we can safely exit
            activeMacroManager.shutdown()
            GlobalScreen.unregisterNativeHook() // Also unregister hook for inspector
            frame.dispose()
            System.exit(0)
        }
    })

    val macroManagerToolbar = ToolBarUI()
    val addIcon = SvgIconRenderer.getIcon("/add-file-icon.svg", 24, 24)
    if (addIcon != null) {
        macroManagerToolbar.addButton(addIcon, "Add Macro") { 
            val macroFolder = File(AppSettings.macroDirectory)
            var newMacroFile: File
            var i = 1
            do {
                newMacroFile = File(macroFolder, "New Macro $i.json")
                i++
            } while (newMacroFile.exists())

            newMacroFile.createNewFile()
            newMacroFile.writeText("{\n    \"events\": []\n}")

            val newEditor = MacroJsonEditorUI(frame, tabbedUI) // Pass tabbedUI
            newEditor.setText(newMacroFile.readText(), newMacroFile)
            tabbedUI.addTab(newMacroFile.name, newEditor)
            tabbedUI.setSelectedComponent(newEditor)
        }
    }

    val removeIcon = SvgIconRenderer.getIcon("/remove-file-icon.svg", 24, 24)
    val deleteIcon = SvgIconRenderer.getIcon("/trash-bin-icon.svg", 24, 24)
    val removeButton = if (removeIcon != null) {
        ToolBarButton(removeIcon, "Remove Macro") {}
    } else {
        ToolBarButton("Remove", "Remove Macro") {}
    }
    macroManagerToolbar.add(removeButton)

    removeButton.addActionListener { 
        if (macroManagerUI.isSelectionMode) {
            macroManagerUI.deleteSelectedMacros()
            if (removeIcon != null) removeButton.setIcon(removeIcon)
            removeButton.setToolTipText("Remove Macro")
        } else {
            macroManagerUI.setSelectionMode(true)
            if (deleteIcon != null) removeButton.setIcon(deleteIcon)
            removeButton.setToolTipText("Delete Selected")
        }
    }

    serverStatusUI.minimumSize = Dimension(0, 50)
    consoleUI.minimumSize = Dimension(200, 100)
    connectedDevicesUI.minimumSize = Dimension(200, 100)
    macroManagerUI.minimumSize = Dimension(200, 100)
    tabbedUI.minimumSize = Dimension(400, 100)

    val consoleAndDevicesSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, consoleUI, connectedDevicesUI)
    consoleAndDevicesSplit.resizeWeight = 0.5

    val tabbedUIToolbar = ToolBarUI()
    val newEventIcon = SvgIconRenderer.getIcon("/add-file-icon.svg", 24, 24)
    if (newEventIcon != null) {
        tabbedUIToolbar.addButton(newEventIcon, "New Event") { 
            val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
            val wasEditorInFocus = focusOwner is RSyntaxTextArea

            val selectedComponent = tabbedUI.selectedComponent
            if (selectedComponent is MacroJsonEditorUI) {
                val dialog = NewEventDialog(frame)
                dialog.isVisible = true
                dialog.createdEvent?.let { event ->
                    selectedComponent.insertNewEvent(event, dialog.isTriggerEvent, wasEditorInFocus)
                }
            }
        }
    }
    val saveIcon = SvgIconRenderer.getIcon("/save-file-icon.svg", 24, 24)
    if (saveIcon != null) {
        tabbedUIToolbar.addButton(saveIcon, "Save") {
            val selectedComponent = tabbedUI.selectedComponent
            if (selectedComponent is MacroJsonEditorUI) {
                selectedComponent.save(tabbedUI.getTitleForComponent(selectedComponent))
            }
        }
    }

    val saveAsIcon = SvgIconRenderer.getIcon("/save-as-icon.svg", 24, 24)
    if (saveAsIcon != null) {
        tabbedUIToolbar.addButton(saveAsIcon, "Save As") {
            val selectedComponent = tabbedUI.selectedComponent
            if (selectedComponent is MacroJsonEditorUI) {
                selectedComponent.saveAs(tabbedUI.getTitleForComponent(selectedComponent))
            }
        }
    }

    val undoIcon = SvgIconRenderer.getIcon("/undo-circle-outline-icon.svg", 24, 24)
    if (undoIcon != null) {
        tabbedUIToolbar.addButton(undoIcon, "Undo") {}
    }

    val redoIcon = SvgIconRenderer.getIcon("/redo-circle-outline-icon.svg", 24, 24)
    if (redoIcon != null) {
        tabbedUIToolbar.addButton(redoIcon, "Redo") {}
    }

    val macroManagerPanel = JPanel(BorderLayout())
    macroManagerPanel.add(macroManagerToolbar, BorderLayout.NORTH)
    macroManagerPanel.add(macroManagerUI, BorderLayout.CENTER)

    val tabbedUIPanel = JPanel(BorderLayout())
    tabbedUIPanel.add(tabbedUIToolbar, BorderLayout.NORTH)
    tabbedUIPanel.add(tabbedUI, BorderLayout.CENTER)

    val centerSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, macroManagerPanel, tabbedUIPanel)
    centerSplit.resizeWeight = 0.0 // Give macroManagerPanel its preferred width

    val bottomHorizontalSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, consoleAndDevicesSplit, centerSplit)
    bottomHorizontalSplit.resizeWeight = 0.15 // Give left side 15%

    val topSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, serverStatusUI, inspectorUI)
    topSplit.resizeWeight = 0.9 // Give serverStatusUI 90%

    val mainSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, bottomHorizontalSplit)
    mainSplitPane.resizeWeight = 0.1

    frame.add(mainSplitPane)

    // Add global key listener for the inspector hotkey
    GlobalScreen.addNativeKeyListener(object : NativeKeyListener {
        override fun nativeKeyPressed(e: NativeKeyEvent) {
            val selectedHotkey = inspectorUI.getSelectedHotkey()
            val hotkeyCode = KeyMap.stringToNativeKeyCodeMap[selectedHotkey]
            if (e.keyCode == hotkeyCode) {
                inspectorUI.toggleFreeze()
            }
        }
        override fun nativeKeyReleased(e: NativeKeyEvent) {}
        override fun nativeKeyTyped(e: NativeKeyEvent) {}
    })


    Toolkit.getDefaultToolkit().addAWTEventListener(AWTEventListener { event ->
        if (event is MouseEvent && event.id == MouseEvent.MOUSE_PRESSED) {
            if (macroManagerUI.isSelectionMode) {
                val source = event.source as? JComponent ?: return@AWTEventListener
                
                val isClickOnRemoveButton = SwingUtilities.isDescendingFrom(source, removeButton)
                val isClickInMacroManager = SwingUtilities.isDescendingFrom(source, macroManagerUI)

                if (!isClickOnRemoveButton && !isClickInMacroManager) {
                    macroManagerUI.cancelSelectionMode()
                    if (removeIcon != null) removeButton.setIcon(removeIcon)
                    removeButton.setToolTipText("Remove Macro")
                }
            }
        }
    }, AWTEvent.MOUSE_EVENT_MASK)

    startItem.addActionListener { wifiServer.startListening(); serverStatusUI.updateStatus(wifiServer.isListening(), 9999) }
    stopItem.addActionListener { wifiServer.stopListening(); serverStatusUI.updateStatus(wifiServer.isListening(), 9999) }
    settingsItem.addActionListener { SettingsUI(frame).isVisible = true }
    macroSettingsItem.addActionListener { MacroSettingsDialog(frame).isVisible = true }

    tabbedUI.addTab("Macro Editor", MacroJsonEditorUI(frame, tabbedUI)) // Pass tabbedUI
    frame.isVisible = true

    wifiServer.startListening()
    serverStatusUI.updateStatus(wifiServer.isListening(), 9999)
}
