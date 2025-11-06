package com.kapcode.open.macropad.kmps.UI

import UI.Theme
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class ConnectedDevicesUI : JPanel() {

    private val devicesPanel: JPanel

    init {
        val theme = Theme()
        layout = BorderLayout()
        background = theme.SecondaryBackgroundColor

        val titleLabel = JLabel("Connected Devices")
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 14f)
        titleLabel.foreground = theme.SecondaryFontColor
        titleLabel.horizontalAlignment = SwingConstants.CENTER
        add(titleLabel, BorderLayout.NORTH)

        devicesPanel = JPanel()
        devicesPanel.layout = BoxLayout(devicesPanel, BoxLayout.Y_AXIS)
        devicesPanel.background = theme.SecondaryBackgroundColor
        add(JScrollPane(devicesPanel), BorderLayout.CENTER)
    }

    fun addDevice(clientId: String, clientName: String) {
        SwingUtilities.invokeLater {
            val deviceItem = ConnectedDeviceItem(clientId, clientName)
            devicesPanel.add(deviceItem)
            devicesPanel.revalidate()
            devicesPanel.repaint()
        }
    }

    fun removeDevice(clientId: String) {
        SwingUtilities.invokeLater {
            for (component in devicesPanel.components) {
                if (component is ConnectedDeviceItem && component.clientId == clientId) {
                    devicesPanel.remove(component)
                    devicesPanel.revalidate()
                    devicesPanel.repaint()
                    break
                }
            }
        }
    }
}