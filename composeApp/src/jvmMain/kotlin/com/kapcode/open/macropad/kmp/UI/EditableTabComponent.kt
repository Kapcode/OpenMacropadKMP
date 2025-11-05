package UI

import java.awt.CardLayout
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.JTabbedPane

class EditableTabComponent(private val tabbedPane: JTabbedPane, initialTitle: String) : JPanel() {

    private val cardLayout = CardLayout()
    private val label: JLabel
    private val textField: JTextField

    private val labelCard = "label"
    private val textFieldCard = "textField"

    init {
        layout = cardLayout
        isOpaque = false

        label = JLabel(initialTitle)
        label.border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 5)

        textField = JTextField(initialTitle)

        add(label, labelCard)
        add(textField, textFieldCard)

        cardLayout.show(this, labelCard)

        // Action listeners for when editing is finished
        textField.addActionListener { stopEditing() }
        textField.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                stopEditing()
            }
        })
    }

    fun startEditing() {
        textField.text = label.text
        cardLayout.show(this@EditableTabComponent, textFieldCard)
        textField.requestFocusInWindow()
        textField.selectAll()
    }

    private fun stopEditing() {
        val newTitle = textField.text
        label.text = newTitle

        val parentPanel = this.parent as? JPanel
        if (parentPanel != null) {
            val index = tabbedPane.indexOfTabComponent(parentPanel)
            if (index != -1) {
                tabbedPane.setTitleAt(index, newTitle)
            }
        }
        cardLayout.show(this, labelCard)
    }

    fun updateTitle(newTitle: String) {
        label.text = newTitle
        textField.text = newTitle
    }
}