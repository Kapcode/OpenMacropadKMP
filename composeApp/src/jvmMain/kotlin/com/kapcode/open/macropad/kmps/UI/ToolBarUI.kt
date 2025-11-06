package UI

import java.awt.event.ActionListener
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JToolBar

class ToolBarUI : JToolBar() {
    init {
        isFloatable = false
        isRollover = true

        background = Theme().PrimaryToolBarBackgroundColor
        border = BorderFactory.createLineBorder(Theme().PrimaryToolBarBorderColor) // Add border to the toolbar itself
    }

    // Method for adding a text-based button
    fun addButton(text: String, tooltip: String, actionListener: ActionListener) {
        val button = ToolBarButton(text, tooltip, actionListener)
        add(button)
    }

    // Method for adding an icon-based button
    fun addButton(icon: Icon, tooltip: String, actionListener: ActionListener) {
        val button = ToolBarButton(icon, tooltip, actionListener)
        add(button)
    }
}