package UI

import AppSettings
import KeyMap
import org.json.JSONObject
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.FlowLayout
import java.awt.GridLayout
import java.io.File
import javax.swing.*
import java.util.LinkedHashMap

class NewEventDialog(parent: JFrame, private val isTriggerDefault: Boolean = false) : JDialog(parent, "Create New Macro Event", true) {

    private val eventTypeComboBox: JComboBox<String>
    private val cardsPanel: JPanel
    private val cardLayout = CardLayout()

    private val keyEventCard = "Key Event"
    private val mouseEventCard = "Mouse Event"
    private val runMacroCard = "Run Macro"
    private val setAutoWaitCard = "Set Auto Wait"

    private lateinit var keyTextField: JTextField
    private lateinit var keyCommandComboBox: JComboBox<String>
    private lateinit var isTriggerCheckBox: JCheckBox
    private lateinit var allowedClientsField: JTextField
    private lateinit var allowedClientsLabel: JLabel

    private lateinit var mouseCommandComboBox: JComboBox<String>
    private lateinit var xCoordinateField: JTextField
    private lateinit var yCoordinateField: JTextField

    private lateinit var macroNameComboBox: JComboBox<String>
    private lateinit var autoWaitField: JTextField

    var createdEvent: JSONObject? = null
        private set
    var isTriggerEvent: Boolean = false
        private set

    init {
        layout = BorderLayout()

        val topPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        topPanel.add(JLabel("Event Type:"))
        eventTypeComboBox = JComboBox(arrayOf(keyEventCard, mouseEventCard, runMacroCard, setAutoWaitCard))
        topPanel.add(eventTypeComboBox)
        add(topPanel, BorderLayout.NORTH)

        cardsPanel = JPanel(cardLayout)
        createKeyEventPanel()
        createMouseEventPanel()
        createRunMacroPanel()
        createSetAutoWaitPanel()
        add(cardsPanel, BorderLayout.CENTER)

        eventTypeComboBox.addActionListener { 
            cardLayout.show(cardsPanel, eventTypeComboBox.selectedItem as String)
        }

        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        val createButton = JButton("Create")
        val cancelButton = JButton("Cancel")
        buttonPanel.add(createButton)
        buttonPanel.add(cancelButton)
        add(buttonPanel, BorderLayout.SOUTH)

        createButton.addActionListener { 
            if (createEvent()) {
                dispose()
            }
        }
        cancelButton.addActionListener { dispose() }

        pack()
        setLocationRelativeTo(parent)
    }

    private fun createKeyEventPanel() {
        val panel = JPanel(GridLayout(0, 2, 5, 5))
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        panel.add(JLabel("Key(s) (spaces or commas):"))
        keyTextField = JTextField()
        panel.add(keyTextField)

        panel.add(JLabel("Command:"))
        keyCommandComboBox = JComboBox(arrayOf("PRESS", "RELEASE", "ON-RELEASE"))
        panel.add(keyCommandComboBox)

        panel.add(JLabel("Is Trigger:"))
        isTriggerCheckBox = JCheckBox()
        isTriggerCheckBox.isSelected = isTriggerDefault // Set initial state
        panel.add(isTriggerCheckBox)

        allowedClientsLabel = JLabel("Allowed Clients (comma-separated):")
        allowedClientsField = JTextField()
        allowedClientsLabel.isVisible = isTriggerDefault
        allowedClientsField.isVisible = isTriggerDefault
        panel.add(allowedClientsLabel)
        panel.add(allowedClientsField)

        // Set command to ON-RELEASE if it's a default trigger
        if (isTriggerDefault) {
            keyCommandComboBox.selectedItem = "ON-RELEASE"
        }

        isTriggerCheckBox.addActionListener { 
            val isTrigger = isTriggerCheckBox.isSelected
            allowedClientsLabel.isVisible = isTrigger
            allowedClientsField.isVisible = isTrigger
            keyCommandComboBox.selectedItem = if (isTrigger) "ON-RELEASE" else "PRESS"
            pack() // Resize dialog to fit new fields
        }

        cardsPanel.add(panel, keyEventCard)
    }

    private fun createMouseEventPanel() {
        val panel = JPanel(GridLayout(0, 2, 5, 5))
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        panel.add(JLabel("Command:"))
        mouseCommandComboBox = JComboBox(MacroMouseItem.MouseCommandType.values().map { it.name }.toTypedArray())
        panel.add(mouseCommandComboBox)

        panel.add(JLabel("X Coordinate:"))
        xCoordinateField = JTextField("0")
        panel.add(xCoordinateField)

        panel.add(JLabel("Y Coordinate:"))
        yCoordinateField = JTextField("0")
        panel.add(yCoordinateField)

        cardsPanel.add(panel, mouseEventCard)
    }

    private fun createRunMacroPanel() {
        val panel = JPanel(GridLayout(0, 2, 5, 5))
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        panel.add(JLabel("Macro Name:"))
        macroNameComboBox = JComboBox()
        val macroFolder = File(AppSettings.macroDirectory)
        macroFolder.listFiles { _, name -> name.endsWith(".json") }?.forEach { file ->
            macroNameComboBox.addItem(file.name)
        }
        panel.add(macroNameComboBox)

        cardsPanel.add(panel, runMacroCard)
    }

    private fun createSetAutoWaitPanel() {
        val panel = JPanel(GridLayout(0, 2, 5, 5))
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        panel.add(JLabel("Wait Value (ms):"))
        autoWaitField = JTextField("50")
        panel.add(autoWaitField)

        cardsPanel.add(panel, setAutoWaitCard)
    }

    private fun validateAndCleanKeys(keyInput: String): List<String>? {
        val withSpaces = keyInput.replace(',', ' ')
        val keys = withSpaces.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }

        if (keys.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Key(s) cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE)
            return null
        }

        val invalidKeys = keys.filter { !KeyMap.stringToNativeKeyCodeMap.containsKey(it.uppercase()) }
        if (invalidKeys.isNotEmpty()) {
            JOptionPane.showMessageDialog(this, "Invalid key(s) found: ${invalidKeys.joinToString()}", "Input Error", JOptionPane.ERROR_MESSAGE)
            return null
        }

        return keys
    }

    private fun createEvent(): Boolean {
        val selectedType = eventTypeComboBox.selectedItem as String
        val map = LinkedHashMap<String, Any>() // Use LinkedHashMap to preserve order
        isTriggerEvent = isTriggerCheckBox.isSelected

        return when (selectedType) {
            keyEventCard -> {
                val keys = validateAndCleanKeys(keyTextField.text)
                if (keys == null) {
                    return false // Validation failed
                }

                if (isTriggerEvent) {
                    map["command"] = "ON-RELEASE"
                    map["keys"] = keys

                    val clients = allowedClientsField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (clients.isNotEmpty()) {
                        map["allowed_clients"] = clients
                    }
                } else {
                    map["type"] = "key"
                    map["command"] = keyCommandComboBox.selectedItem as String
                    if (keys.size > 1) {
                        JOptionPane.showMessageDialog(this, "Only triggers can have multiple keys.", "Input Error", JOptionPane.ERROR_MESSAGE)
                        return false
                    }
                    map["key"] = keys.first()
                }
                createdEvent = JSONObject(map)
                true
            }
            mouseEventCard -> {
                try {
                    val x = xCoordinateField.text.toInt()
                    val y = yCoordinateField.text.toInt()
                    map["type"] = "mouse"
                    map["command"] = mouseCommandComboBox.selectedItem as String
                    map["x"] = x
                    map["y"] = y
                    createdEvent = JSONObject(map)
                    true
                } catch (e: NumberFormatException) {
                    JOptionPane.showMessageDialog(this, "Coordinates must be valid integers.", "Input Error", JOptionPane.ERROR_MESSAGE)
                    false
                }
            }
            runMacroCard -> {
                val selectedMacro = macroNameComboBox.selectedItem as? String
                if (selectedMacro == null) {
                    JOptionPane.showMessageDialog(this, "No macro selected.", "Input Error", JOptionPane.ERROR_MESSAGE)
                    return false
                }
                map["type"] = "run_macro"
                map["macro_name"] = selectedMacro
                createdEvent = JSONObject(map)
                true
            }
            setAutoWaitCard -> {
                try {
                    val waitValue = autoWaitField.text.toInt()
                    map["type"] = "set_auto_wait"
                    map["value"] = waitValue
                    createdEvent = JSONObject(map)
                    true
                } catch (e: NumberFormatException) {
                    JOptionPane.showMessageDialog(this, "Wait value must be a valid integer.", "Input Error", JOptionPane.ERROR_MESSAGE)
                    false
                }
            }
            else -> false
        }
    }
}