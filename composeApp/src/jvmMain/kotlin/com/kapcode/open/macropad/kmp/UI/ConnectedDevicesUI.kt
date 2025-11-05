package UI

import java.awt.BorderLayout
import java.awt.Font
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingConstants

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

        // Add a test item for development
        addDevice("Test-Device-01")
    }

    fun addDevice(deviceId: String) {
        val deviceItem = ConnectedDeviceItem(deviceId)
        devicesPanel.add(deviceItem)
        devicesPanel.revalidate()
        devicesPanel.repaint()
    }

    fun removeDevice(deviceId: String) {
        for (component in devicesPanel.components) {
            if (component is ConnectedDeviceItem && component.deviceId == deviceId) {
                devicesPanel.remove(component)
                devicesPanel.revalidate()
                devicesPanel.repaint()
                break
            }
        }
    }
}