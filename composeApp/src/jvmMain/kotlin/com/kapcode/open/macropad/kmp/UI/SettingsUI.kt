import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class SettingsUI(parent: JFrame) : JDialog(parent, "Settings", true) {
    private val contentPanel: JPanel
    private val portField: JTextField
    private val encryptionComboBox: JComboBox<String>

    companion object {
        const val DEFAULT_PORT = 8080
        const val DEFAULT_ENCRYPTION = "TLS/SSL"
    }

    init {
        // Main content panel
        contentPanel = JPanel()
        contentPanel.layout = GridBagLayout()
        contentPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL

        // Port field
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        contentPanel.add(JLabel("Port:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        portField = JTextField(DEFAULT_PORT.toString(), 10)
        contentPanel.add(portField, gbc)

        gbc.gridx = 2
        gbc.weightx = 0.0
        val resetPortButton = JButton("Reset to Default")
        resetPortButton.addActionListener {
            portField.text = DEFAULT_PORT.toString()
        }
        contentPanel.add(resetPortButton, gbc)

        // Encryption settings
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        contentPanel.add(JLabel("Encryption:"), gbc)

        gbc.gridx = 1
        gbc.gridwidth = 2
        gbc.weightx = 1.0
        encryptionComboBox = JComboBox(arrayOf("None", "TLS/SSL"))
        encryptionComboBox.selectedItem = DEFAULT_ENCRYPTION
        contentPanel.add(encryptionComboBox, gbc)

        // Button panel at the bottom
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))

        val okButton = JButton("OK")
        okButton.addActionListener {
            saveSettings()
            dispose()
        }

        val cancelButton = JButton("Cancel")
        cancelButton.addActionListener {
            dispose()
        }

        buttonPanel.add(okButton)
        buttonPanel.add(cancelButton)

        // Add panels to the dialog
        add(contentPanel, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)

        // Dialog properties
        setSize(500, 200)
        setLocationRelativeTo(parent)
    }

    private fun saveSettings() {
        val port = portField.text.toIntOrNull()
        val encryption = encryptionComboBox.selectedItem as String

        if (port == null || port < 1 || port > 65535) {
            JOptionPane.showMessageDialog(
                this,
                "Please enter a valid port number (1-65535)",
                "Invalid Port",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        // TODO: Save settings to configuration
        println("Saving settings - Port: $port, Encryption: $encryption")
    }
}