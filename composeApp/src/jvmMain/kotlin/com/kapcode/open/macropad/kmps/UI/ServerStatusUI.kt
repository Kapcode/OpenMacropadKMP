import UI.Theme
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class ServerStatusUI : JPanel() {
    private val statusLabel: JLabel
    private val portLabel: JLabel
    private val ipLabel: JLabel?

    init {
        val theme = Theme()
        layout = BorderLayout()
        border = EmptyBorder(10, 10, 10, 10)
        background = theme.SecondaryBackgroundColor

        // Title panel
        val titlePanel = JPanel(BorderLayout())
        titlePanel.background = theme.BackgroundColor
        val titleLabel = JLabel("Server Status")
        titleLabel.horizontalAlignment = SwingConstants.CENTER
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 16f)
        titleLabel.foreground = theme.FontColor
        titlePanel.add(titleLabel, BorderLayout.CENTER)
        titlePanel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        add(titlePanel, BorderLayout.NORTH)

        // Main content panel now uses GridBagLayout for precise control
        val contentPanel = JPanel(GridBagLayout())
        contentPanel.background = theme.SecondaryBackgroundColor
        val gbc = GridBagConstraints()

        // --- Status Details Panel (Centered) ---
        val statusDetailsPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        statusDetailsPanel.background = theme.SecondaryBackgroundColor
        statusLabel = JLabel("Status: Stopped")
        portLabel = JLabel("Port: N/A")
        ipLabel = JLabel("IP: " + Utility().getSystemIP())
        statusLabel.foreground = theme.SecondaryFontColor
        portLabel.foreground = theme.SecondaryFontColor
        ipLabel.foreground = theme.SecondaryFontColor
        statusDetailsPanel.add(statusLabel)
        statusDetailsPanel.add(Box.createHorizontalStrut(10))
        statusDetailsPanel.add(portLabel)
        statusDetailsPanel.add(Box.createHorizontalStrut(10))
        statusDetailsPanel.add(ipLabel)

        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 3 // Span all 3 columns
        gbc.fill = GridBagConstraints.HORIZONTAL
        contentPanel.add(statusDetailsPanel, gbc)

        // --- Vertical Spacer ---
        gbc.gridy = 1
        gbc.gridwidth = 1 // Reset gridwidth
        gbc.fill = GridBagConstraints.NONE // Reset fill
        contentPanel.add(Box.createVerticalStrut(15), gbc)

        // --- Vertical Glue to push everything up ---
        gbc.gridy = 2 // Next row
        gbc.weighty = 1.0 // Takes up all available vertical space
        contentPanel.add(Box.createVerticalGlue(), gbc)

        // Add a scroll pane for the content
        val scrollPane = JScrollPane(contentPanel)
        scrollPane.border = BorderFactory.createEmptyBorder()
        add(scrollPane, BorderLayout.CENTER)
    }

    fun updateStatus(isRunning: Boolean, port: Int) {
        SwingUtilities.invokeLater {
            statusLabel.text = "Status: " + (if (isRunning) "Running" else "Stopped")
            portLabel.text = if (isRunning) "Port: $port" else "Port: N/A"
        }
    }
}