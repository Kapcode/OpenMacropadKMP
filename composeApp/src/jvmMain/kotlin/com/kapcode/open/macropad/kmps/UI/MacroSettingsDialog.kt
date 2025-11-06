package UI

import AppSettings
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*

class MacroSettingsDialog(parent: JFrame) : JDialog(parent, "Macro Settings", true) {

    private val macroDirField: JTextField

    init {
        layout = BorderLayout()
        val panel = JPanel(BorderLayout(10, 10))
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        val topPanel = JPanel(BorderLayout())
        topPanel.add(JLabel("Macro Directory:"), BorderLayout.WEST)
        macroDirField = JTextField(AppSettings.macroDirectory, 30)
        topPanel.add(macroDirField, BorderLayout.CENTER)

        val browseButton = JButton("Browse")
        browseButton.addActionListener { 
            val chooser = JFileChooser()
            chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            chooser.dialogTitle = "Select Macro Directory"
            chooser.selectedFile = java.io.File(macroDirField.text)
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                macroDirField.text = chooser.selectedFile.absolutePath
            }
        }
        topPanel.add(browseButton, BorderLayout.EAST)

        panel.add(topPanel, BorderLayout.NORTH)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        val saveButton = JButton("Save")
        saveButton.addActionListener { 
            AppSettings.macroDirectory = macroDirField.text
            JOptionPane.showMessageDialog(this, "Settings saved. The application may need to be restarted for all changes to take effect.", "Settings Saved", JOptionPane.INFORMATION_MESSAGE)
            dispose()
        }
        val cancelButton = JButton("Cancel")
        cancelButton.addActionListener { dispose() }

        buttonPanel.add(saveButton)
        buttonPanel.add(cancelButton)
        panel.add(buttonPanel, BorderLayout.SOUTH)

        add(panel)
        pack()
        setLocationRelativeTo(parent)
    }
}