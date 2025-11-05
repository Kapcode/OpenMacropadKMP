package UI

import KeyMap
import java.awt.*
import javax.swing.*

class RecordingSettingsDialog(parent: JFrame) : JDialog(parent, "Recording Settings", true) {

    enum class RecordingLevel {
        KEYS_ONLY,
        KEYS_AND_MOUSE_BUTTONS,
        KEYS_MOUSE_BUTTONS_AND_MOVES
    }

    var recordingLevel: RecordingLevel = RecordingLevel.KEYS_ONLY
        private set
    var animateMouseMoves: Boolean = false
        private set
    var endOnKeys: List<String>? = null
        private set
    var endOnMouseLocation: Point? = null
        private set
    var endAfterTime: Int? = null
        private set
    var shouldStartRecording = false
        private set

    private val animateCheckBox: JCheckBox
    private val endOnKeysCheckBox: JCheckBox
    private val keyTextField: JTextField
    private val endOnLocationCheckBox: JCheckBox
    private val xCoordinateField: JTextField
    private val yCoordinateField: JTextField
    private val endAfterTimeCheckBox: JCheckBox
    private val timeField: JTextField

    init {
        layout = BorderLayout()

        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        mainPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        // --- Recording Level ---
        mainPanel.add(JLabel("Record:"))
        val keysOnlyRadio = JRadioButton("Keys Only")
        keysOnlyRadio.isSelected = true
        val keysAndMouseRadio = JRadioButton("Keys and Mouse Buttons")
        val keysMouseAndMovesRadio = JRadioButton("Keys, Mouse Buttons, and Mouse Moves")

        val group = ButtonGroup()
        group.add(keysOnlyRadio)
        group.add(keysAndMouseRadio)
        group.add(keysMouseAndMovesRadio)

        animateCheckBox = JCheckBox("Animate Mouse Moves")
        animateCheckBox.isEnabled = false // Disabled by default

        keysOnlyRadio.addActionListener { animateCheckBox.isEnabled = false }
        keysAndMouseRadio.addActionListener { animateCheckBox.isEnabled = false }
        keysMouseAndMovesRadio.addActionListener { animateCheckBox.isEnabled = true }

        mainPanel.add(keysOnlyRadio)
        mainPanel.add(keysAndMouseRadio)
        mainPanel.add(keysMouseAndMovesRadio)
        mainPanel.add(animateCheckBox)

        mainPanel.add(JSeparator(SwingConstants.HORIZONTAL))
        mainPanel.add(Box.createVerticalStrut(10))

        // --- End Triggers ---
        mainPanel.add(JLabel("End Recording When:"))

        val keyPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        endOnKeysCheckBox = JCheckBox("Key(s) Pressed:", true) // Checked by default
        keyTextField = JTextField("ESCAPE", 15) // Default text and enabled
        keyTextField.isEnabled = true
        endOnKeysCheckBox.addActionListener { keyTextField.isEnabled = endOnKeysCheckBox.isSelected }
        keyPanel.add(endOnKeysCheckBox)
        keyPanel.add(keyTextField)
        mainPanel.add(keyPanel)

        val locationPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        endOnLocationCheckBox = JCheckBox("Mouse Enters (X, Y):")
        xCoordinateField = JTextField(5)
        yCoordinateField = JTextField(5)
        xCoordinateField.isEnabled = false
        yCoordinateField.isEnabled = false
        val getCurrentButton = JButton("Get Current")
        getCurrentButton.isEnabled = false
        endOnLocationCheckBox.addActionListener { 
            val selected = endOnLocationCheckBox.isSelected
            xCoordinateField.isEnabled = selected
            yCoordinateField.isEnabled = selected
            getCurrentButton.isEnabled = selected
        }
        getCurrentButton.addActionListener { 
            val p = MouseInfo.getPointerInfo().location
            xCoordinateField.text = p.x.toString()
            yCoordinateField.text = p.y.toString()
        }
        locationPanel.add(endOnLocationCheckBox)
        locationPanel.add(xCoordinateField)
        locationPanel.add(yCoordinateField)
        locationPanel.add(getCurrentButton)
        mainPanel.add(locationPanel)

        val timePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        endAfterTimeCheckBox = JCheckBox("Time Elapsed (seconds):")
        timeField = JTextField(5)
        timeField.isEnabled = false
        endAfterTimeCheckBox.addActionListener { timeField.isEnabled = endAfterTimeCheckBox.isSelected }
        timePanel.add(endAfterTimeCheckBox)
        timePanel.add(timeField)
        mainPanel.add(timePanel)

        add(mainPanel, BorderLayout.CENTER)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        val startButton = JButton("Start Recording")
        val cancelButton = JButton("Cancel")
        buttonPanel.add(startButton)
        buttonPanel.add(cancelButton)
        add(buttonPanel, BorderLayout.SOUTH)

        startButton.addActionListener { 
            // Set recording level
            recordingLevel = when {
                keysOnlyRadio.isSelected -> RecordingLevel.KEYS_ONLY
                keysAndMouseRadio.isSelected -> RecordingLevel.KEYS_AND_MOUSE_BUTTONS
                else -> RecordingLevel.KEYS_MOUSE_BUTTONS_AND_MOVES
            }
            animateMouseMoves = animateCheckBox.isSelected

            // Set end triggers
            if (endOnKeysCheckBox.isSelected) {
                val withSpaces = keyTextField.text.replace(',', ' ')
                val keys = withSpaces.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }
                val invalidKeys = keys.filter { !KeyMap.stringToNativeKeyCodeMap.containsKey(it.uppercase()) }
                if (invalidKeys.isNotEmpty()) {
                    JOptionPane.showMessageDialog(this, "Invalid stop key(s) found: ${invalidKeys.joinToString()}", "Input Error", JOptionPane.ERROR_MESSAGE)
                    return@addActionListener
                }
                endOnKeys = keys
            }
            if (endOnLocationCheckBox.isSelected) {
                try {
                    val x = xCoordinateField.text.toInt()
                    val y = yCoordinateField.text.toInt()
                    endOnMouseLocation = Point(x, y)
                } catch (e: NumberFormatException) {
                    JOptionPane.showMessageDialog(this, "Invalid mouse coordinates.", "Input Error", JOptionPane.ERROR_MESSAGE)
                    return@addActionListener
                }
            }
            if (endAfterTimeCheckBox.isSelected) {
                try {
                    endAfterTime = timeField.text.toInt()
                } catch (e: NumberFormatException) {
                    JOptionPane.showMessageDialog(this, "Invalid time value.", "Input Error", JOptionPane.ERROR_MESSAGE)
                    return@addActionListener
                }
            }

            shouldStartRecording = true
            dispose()
        }

        cancelButton.addActionListener { 
            shouldStartRecording = false
            dispose()
        }

        pack()
        setLocationRelativeTo(parent)
    }
}