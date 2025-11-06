package UI

import ActiveMacroManager
import AppSettings
import MacroPlayer
import org.json.JSONObject
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService
import javax.swing.*

class MacroManagerUI(private val tabbedUI: TabbedUI, private val activeMacroManager: ActiveMacroManager, private val macroPlayer: MacroPlayer) : JPanel() {

    private var macroFolder = File(AppSettings.macroDirectory)
    private val macrosPanel: JPanel

    private var watcher: WatchService? = null
    private var watchThread: Thread? = null
    var isSelectionMode = false
        private set

    init {
        val theme = Theme()
        layout = BorderLayout()
        background = theme.SecondaryBackgroundColor

        val headerPanel = JPanel(BorderLayout())
        headerPanel.background = theme.SecondaryBackgroundColor
        val titleLabel = JLabel("Macro Manager")
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 14f)
        titleLabel.foreground = theme.SecondaryFontColor
        titleLabel.horizontalAlignment = SwingConstants.CENTER // Center the text
        headerPanel.add(titleLabel, BorderLayout.CENTER) // Center the label in the panel
        add(headerPanel, BorderLayout.NORTH)

        macrosPanel = JPanel()
        macrosPanel.layout = BoxLayout(macrosPanel, BoxLayout.Y_AXIS)
        macrosPanel.background = theme.SecondaryBackgroundColor
        add(JScrollPane(macrosPanel), BorderLayout.CENTER)

        ensureMacroDirectoryExists()
        loadMacros()
        startWatchingMacroDirectory()
    }

    fun setSelectionMode(enabled: Boolean) {
        isSelectionMode = enabled
        loadMacros()
    }

    fun cancelSelectionMode() {
        if (isSelectionMode) {
            setSelectionMode(false)
        }
    }

    fun deleteSelectedMacros() {
        val selectedItems = macrosPanel.components.filterIsInstance<MacroManagerItem>().filter { it.isSelected() }

        if (selectedItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No macros selected for deletion.", "Delete Macros", JOptionPane.INFORMATION_MESSAGE)
            return
        }

        val fileNames = selectedItems.joinToString("\n") { "- ${it.getMacroFile().name}" }
        val confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete the following files?\n$fileNames",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION
        )

        if (confirm == JOptionPane.YES_OPTION) {
            selectedItems.forEach { item ->
                val macroFile = item.getMacroFile()
                // Remove from active macros if it was active
                activeMacroManager.removeActiveMacro(macroFile)

                for (i in 0 until tabbedUI.tabCount) {
                    val component = tabbedUI.getComponentAt(i) as? MacroJsonEditorUI
                    if (component != null && component.getCurrentFile()?.absolutePath == macroFile.absolutePath) {
                        tabbedUI.remove(i)
                        break
                    }
                }
                macroFile.delete()
            }
            setSelectionMode(false)
        }
    }

    private fun ensureMacroDirectoryExists() {
        if (!macroFolder.exists()) {
            macroFolder.mkdirs()
        }
    }

    private fun startWatchingMacroDirectory() {
        try {
            watcher = FileSystems.getDefault().newWatchService()
            macroFolder.toPath().register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY)

            watchThread = Thread { 
                while (!Thread.currentThread().isInterrupted) {
                    val key: WatchKey = try {
                        watcher!!.take()
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return@Thread
                    }

                    for (event in key.pollEvents()) {
                        if (event.kind() !== StandardWatchEventKinds.OVERFLOW) {
                            SwingUtilities.invokeLater { loadMacros() }
                        }
                    }

                    if (!key.reset()) {
                        break
                    }
                }
            }.apply { isDaemon = true; start() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun removeNotify() {
        super.removeNotify()
        watchThread?.interrupt()
        watcher?.close()
    }

    private fun loadMacros() {
        macrosPanel.removeAll()

        val macroFiles = macroFolder.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()

        if (macroFiles.isEmpty()) {
            val emptyLabel = JLabel("No macros found.")
            emptyLabel.foreground = Theme().SecondaryFontColor
            macrosPanel.add(emptyLabel)
        } else {
            macroFiles.forEach { file ->
                macrosPanel.add(MacroManagerItem(file))
            }
        }
        macrosPanel.revalidate()
        macrosPanel.repaint()
    }

    inner class MacroManagerItem(private val macroFile: File) : JPanel() {
        private val selectionCheckBox = JCheckBox() // For batch delete selection
        private val toggleSwitch = JCheckBox() // For on/off state

        init {
            val theme = Theme()
            layout = BorderLayout()
            background = theme.SecondaryBorderColor // Dark Gray background
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, theme.PrimaryToolBarTooltipFontColor), // White separator line
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            )

            // Left side: Selection Checkbox, On/Off Label, Toggle Switch, and Name
            val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            leftPanel.isOpaque = false
            selectionCheckBox.isVisible = isSelectionMode
            leftPanel.add(selectionCheckBox)

            val onOffLabel = JLabel("On/Off:")
            onOffLabel.font = onOffLabel.font.deriveFont(Font.PLAIN, 10f) // Smaller font
            onOffLabel.foreground = theme.SecondaryFontColor
            leftPanel.add(onOffLabel)

            leftPanel.add(toggleSwitch) // Add the toggle switch
            toggleSwitch.toolTipText = "Toggle macro on/off for global hotkey"
            toggleSwitch.isSelected = activeMacroManager.isMacroActive(macroFile)
            toggleSwitch.addActionListener { 
                if (toggleSwitch.isSelected) {
                    try {
                        val macroJson = JSONObject(macroFile.readText())
                        activeMacroManager.addActiveMacro(macroFile, macroJson)
                    } catch (e: Exception) {
                        JOptionPane.showMessageDialog(this, "Error activating macro: ${e.message}", "Activation Error", JOptionPane.ERROR_MESSAGE)
                        toggleSwitch.isSelected = false // Revert toggle state
                        e.printStackTrace()
                    }
                } else {
                    activeMacroManager.removeActiveMacro(macroFile)
                }
            }

            val nameLabel = JLabel(macroFile.nameWithoutExtension)
            nameLabel.foreground = theme.SecondaryFontColor
            leftPanel.add(nameLabel)
            add(leftPanel, BorderLayout.WEST)

            // Right side: Buttons
            val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
            buttonPanel.isOpaque = false

            val playIcon = SvgIconRenderer.getIcon("/play-button-outline-green-icon.svg", 16, 16)
            val playButton = if (playIcon != null) JButton(playIcon) else JButton("Play")
            playButton.toolTipText = "Play Macro"
            styleButton(playButton, theme.ThirdButtonColor)
            playButton.addActionListener { 
                Thread { // Run on a separate thread to avoid blocking the EDT
                    try {
                        macroPlayer.play(macroFile.readText()) // Use the shared macroPlayer instance
                    } catch (e: Exception) {
                        SwingUtilities.invokeLater { // Show error on EDT
                            JOptionPane.showMessageDialog(this, "Error playing macro: ${e.message}", "Macro Playback Error", JOptionPane.ERROR_MESSAGE)
                        }
                        e.printStackTrace()
                    }
                }.start()
            }

            val editIcon = SvgIconRenderer.getIcon("/pencil-icon.svg", 16, 16)
            val editButton = if (editIcon != null) JButton(editIcon) else JButton("Edit")
            editButton.toolTipText = "Edit Macro"
            styleButton(editButton, theme.ThirdButtonColor)
            editButton.addActionListener { 
                val frame = SwingUtilities.getWindowAncestor(this@MacroManagerUI) as? JFrame ?: return@addActionListener
                val newEditor = MacroJsonEditorUI(frame, tabbedUI) // Pass tabbedUI
                newEditor.setText(macroFile.readText(), macroFile)
                tabbedUI.addTab(macroFile.name, newEditor)
                tabbedUI.setSelectedComponent(newEditor)
            }

            val deleteIcon = SvgIconRenderer.getIcon("/trash-bin-icon.svg", 16, 16)
            val deleteButton = if (deleteIcon != null) JButton(deleteIcon) else JButton("Delete")
            deleteButton.toolTipText = "Delete Macro"
            styleButton(deleteButton, theme.ThirdButtonColor)
            deleteButton.addActionListener { 
                val confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete '${macroFile.name}'?", "Confirm Deletion", JOptionPane.YES_NO_OPTION)
                if (confirm == JOptionPane.YES_OPTION) {
                    // Remove from active macros if it was active
                    activeMacroManager.removeActiveMacro(macroFile)

                    for (i in 0 until tabbedUI.tabCount) {
                        val component = tabbedUI.getComponentAt(i) as? MacroJsonEditorUI
                        if (component != null && component.getCurrentFile()?.absolutePath == macroFile.absolutePath) {
                            tabbedUI.remove(i)
                            break
                        }
                    }
                    macroFile.delete()
                }
            }

            buttonPanel.add(editButton)
            buttonPanel.add(playButton)
            buttonPanel.add(deleteButton)
            add(buttonPanel, BorderLayout.EAST)

            val prefHeight = preferredSize.height
            maximumSize = Dimension(Short.MAX_VALUE.toInt(), prefHeight)
        }

        private fun styleButton(button: JButton, bgColor: java.awt.Color) {
            val theme = Theme()
            button.background = bgColor
            button.foreground = theme.SecondaryButtonFont
            button.border = BorderFactory.createLineBorder(theme.SecondaryButtonBorder)
            button.isFocusable = false
        }

        fun isSelected(): Boolean = selectionCheckBox.isSelected
        fun getMacroFile(): File = macroFile
    }
}