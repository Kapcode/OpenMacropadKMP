package UI

import java.awt.BorderLayout
import java.awt.event.ActionListener
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JPanel

class ToolBarButton : JPanel {

    private lateinit var button: JButton

    constructor(icon: Icon, toolTipText: String, actionListener: ActionListener) : super(BorderLayout()) {
        button = JButton(icon)
        setup(toolTipText, actionListener)
    }

    constructor(text: String, toolTipText: String, actionListener: ActionListener) : super(BorderLayout()) {
        button = JButton(text)
        setup(toolTipText, actionListener)
    }

    private fun setup(toolTipText: String, actionListener: ActionListener) {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(2, 5, 2, 5)

        button.toolTipText = toolTipText
        button.addActionListener(actionListener)

        button.isFocusable = false
        button.background = Theme().PrimaryButtonColor
        button.foreground = Theme().PrimaryButtonFont
        button.border = BorderFactory.createLineBorder(Theme().PrimaryButtonBorder)

        add(button, BorderLayout.CENTER)
    }

    fun addActionListener(listener: ActionListener) {
        button.addActionListener(listener)
    }

    fun setIcon(icon: Icon) {
        button.icon = icon
    }

    override fun setToolTipText(text: String) {
        button.toolTipText = text
    }
}