package UI

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class ConnectedDeviceLabel(deviceName: String) : JPanel() {
    private val nameLabel: JLabel
    private val popupMenu: JPopupMenu
    
    init {
        layout = BorderLayout()
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10),
            BorderFactory.createEtchedBorder()
        )
        
        // Keep panel background as default (don't set it)
        
        // Set min and max height to 80
        minimumSize = Dimension(0, 80)
        maximumSize = Dimension(Int.MAX_VALUE, 80)
        preferredSize = Dimension(preferredSize.width, 80)
        
        // Device name label in bold with light blue background
        nameLabel = JLabel("Device: $deviceName")
        nameLabel.font = nameLabel.font.deriveFont(Font.BOLD)
        nameLabel.background = Color(173, 216, 230) // Light blue
        nameLabel.isOpaque = true
        
        // Create popup menu
        popupMenu = JPopupMenu()
        
        val disconnectItem = JMenuItem("Disconnect")
        disconnectItem.addActionListener {
            // TODO: Handle disconnect
            println("Disconnecting device: $deviceName")
        }
        
        val viewDetailsItem = JMenuItem("View Details")
        viewDetailsItem.addActionListener {
            // TODO: Show device details
            println("Viewing details for: $deviceName")
        }
        
        val sendMessageItem = JMenuItem("Send Message")
        sendMessageItem.addActionListener {
            // TODO: Send message to device
            println("Sending message to: $deviceName")
        }
        
        popupMenu.add(disconnectItem)
        popupMenu.add(viewDetailsItem)
        popupMenu.add(sendMessageItem)
        
        // Add mouse listener for right-click
        val mouseListener = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    showPopup(e)
                }
            }
            
            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    showPopup(e)
                }
            }
            
            private fun showPopup(e: MouseEvent) {
                popupMenu.show(e.component, e.x, e.y)
            }
        }
        
        // Add listener to both the panel and the label
        addMouseListener(mouseListener)
        nameLabel.addMouseListener(mouseListener)
        
        add(nameLabel, BorderLayout.CENTER)
    }
    
    fun getDeviceName(): String {
        return nameLabel.text.removePrefix("Device: ")
    }
}