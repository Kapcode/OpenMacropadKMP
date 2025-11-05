package UI

import java.awt.CardLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.io.File

class TabbedUI(private val frame: JFrame) : JPanel(CardLayout()) {

    private val tabbedPane = JTabbedPane()
    private val newDocumentButton: JButton
    private val cardLayout = layout as CardLayout

    private val tabsCard = "Tabs"
    private val newButtonCard = "NewButton"

    init {
        // Create the "New Document" button panel
        val newButtonPanel = JPanel(GridBagLayout())
        newDocumentButton = JButton("New Document")
        newDocumentButton.addActionListener { 
            val macroFolder = File(System.getProperty("user.home") + File.separator + "Documents" + File.separator + "OpenMacropadServer" + File.separator + "Macros")
            var newMacroFile: File
            var i = 1
            do {
                newMacroFile = File(macroFolder, "New Macro $i.json")
                i++
            } while (newMacroFile.exists())

            newMacroFile.createNewFile()
            newMacroFile.writeText("{\n    \"events\": []\n}")

            val newEditor = MacroJsonEditorUI(frame, this) // Pass 'this' (TabbedUI instance)
            newEditor.setText(newMacroFile.readText(), newMacroFile)
            addTab(newMacroFile.name, newEditor)
            setSelectedComponent(newEditor)
        }
        newButtonPanel.add(newDocumentButton, GridBagConstraints())

        // Add cards to the main panel
        add(tabbedPane, tabsCard)
        add(newButtonPanel, newButtonCard)

        // Add a change listener to show/hide the button
        tabbedPane.addChangeListener { 
            checkShowNewDocumentButton()
        }

        // Initial check
        checkShowNewDocumentButton()
    }

    private fun checkShowNewDocumentButton() {
        if (tabbedPane.tabCount == 0) {
            cardLayout.show(this, newButtonCard)
        } else {
            cardLayout.show(this, tabsCard)
        }
    }

    fun addTab(title: String, component: Component) {
        tabbedPane.addTab(title, component)
        val index = tabbedPane.indexOfComponent(component)
        tabbedPane.setTabComponentAt(index, TabTitle(title))
        checkShowNewDocumentButton()
    }

    override fun remove(index: Int) {
        tabbedPane.remove(index)
        checkShowNewDocumentButton()
    }

    override fun add(title: String, component: Component): Component {
        addTab(title, component)
        return component
    }

    val tabCount: Int
        get() = tabbedPane.tabCount

    val selectedComponent: Component?
        get() = tabbedPane.selectedComponent

    fun getComponentAt(index: Int): Component {
        return tabbedPane.getComponentAt(index)
    }

    fun setSelectedComponent(component: Component) {
        tabbedPane.selectedComponent = component
    }

    fun setTitleForComponent(component: Component, title: String) {
        val index = tabbedPane.indexOfComponent(component)
        if (index != -1) {
            val tabTitle = tabbedPane.getTabComponentAt(index) as? TabTitle
            tabTitle?.editableLabel?.text = title
            tabbedPane.setTitleAt(index, title)
        }
    }

    fun getTitleForComponent(component: Component): String? {
        val index = tabbedPane.indexOfComponent(component)
        return if (index != -1) {
            (tabbedPane.getTabComponentAt(index) as? TabTitle)?.editableLabel?.text
        } else {
            null
        }
    }

    inner class TabTitle(initialTitle: String) : JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {
        val editableLabel: JLabel = JLabel(initialTitle)
        private val textField: JTextField = JTextField(initialTitle)

        init {
            isOpaque = false
            isFocusable = false // Make the TabTitle JPanel itself non-focusable

            editableLabel.isFocusable = false

            add(editableLabel)
            add(textField)

            val editIcon = SvgIconRenderer.getIcon("/pencil-icon.svg", 12, 12)
            val editButton = if (editIcon != null) JButton(editIcon) else JButton("Edit")
            editButton.toolTipText = "Edit Tab Title"
            editButton.isContentAreaFilled = false
            editButton.isFocusable = false
            editButton.border = BorderFactory.createEmptyBorder()
            editButton.addActionListener { startEditing() }

            add(Box.createHorizontalStrut(10))
            add(editButton)

            add(Box.createHorizontalStrut(10))
            add(TabButton())

            editableLabel.isVisible = true
            textField.isVisible = false

            textField.addFocusListener(object : FocusAdapter() {
                override fun focusLost(e: FocusEvent?) {
                    finishEditing()
                }
            })

            textField.addActionListener { finishEditing() }
        }

        fun startEditing() {
            editableLabel.isVisible = false
            textField.isVisible = true
            textField.text = editableLabel.text
            revalidate()
            repaint()
            textField.requestFocusInWindow()
            textField.selectAll()
        }

        private fun finishEditing() {
            val newTitle = textField.text
            editableLabel.text = newTitle
            editableLabel.isVisible = true
            textField.isVisible = false
            revalidate()
            repaint()

            val tabIndex = tabbedPane.indexOfTabComponent(this)
            if (tabIndex != -1) {
                tabbedPane.setTitleAt(tabIndex, newTitle)
            }
        }
    }

    inner class TabButton : JButton(SvgIconRenderer.getIcon("/close-icon.svg", 12, 12)), SwingConstants {
        init {
            isContentAreaFilled = false
            isFocusable = false
            border = BorderFactory.createEmptyBorder()
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val i = tabbedPane.indexOfTabComponent(this@TabButton.parent as Component)
                    if (i != -1) {
                        val editor = tabbedPane.getComponentAt(i) as? MacroJsonEditorUI
                        if (editor?.hasUnsavedChanges == true) {
                            val choice = JOptionPane.showConfirmDialog(
                                this@TabbedUI,
                                "Save changes to ${getTitleForComponent(editor)}?",
                                "Unsaved Changes",
                                JOptionPane.YES_NO_CANCEL_OPTION
                            )
                            when (choice) {
                                JOptionPane.YES_OPTION -> {
                                    editor.save(getTitleForComponent(editor))
                                    this@TabbedUI.remove(i) // Explicitly call TabbedUI's remove
                                }
                                JOptionPane.NO_OPTION -> this@TabbedUI.remove(i) // Explicitly call TabbedUI's remove
                            }
                        } else {
                            this@TabbedUI.remove(i) // Explicitly call TabbedUI's remove
                        }
                    }
                }
            })
        }
    }
}