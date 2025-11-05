package UI

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class ConnectedDeviceItem(val deviceId: String) : JPanel() {

    private val fixedHeight = 40

    init {
        layout = BorderLayout()
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme().PrimaryToolBarTooltipFontColor),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        )

        val nameLabel = JLabel(deviceId)
        add(nameLabel, BorderLayout.WEST)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        buttonPanel.isOpaque = false

        val disconnectIcon = SvgIconRenderer.getIcon("/cross-icon.svg", 16, 16)
        val disconnectButton = if (disconnectIcon != null) JButton(disconnectIcon) else JButton("X")
        disconnectButton.toolTipText = "Disconnect this device"
        // disconnectButton.addActionListener { /* Add disconnect logic here */ }
        buttonPanel.add(disconnectButton)

        val messageIcon = SvgIconRenderer.getIcon("/email-mail-sent-icon.svg", 16, 16)
        val messageButton = if (messageIcon != null) JButton(messageIcon) else JButton("Send")
        messageButton.toolTipText = "Send a message to this device"
        // messageButton.addActionListener { /* Add send message logic here */ }
        buttonPanel.add(messageButton)

        add(buttonPanel, BorderLayout.EAST)

        // Set preferred and maximum size to enforce uniform height
        val prefSize = preferredSize
        preferredSize = Dimension(prefSize.width, fixedHeight)
        maximumSize = Dimension(Short.MAX_VALUE.toInt(), fixedHeight)
    }
}