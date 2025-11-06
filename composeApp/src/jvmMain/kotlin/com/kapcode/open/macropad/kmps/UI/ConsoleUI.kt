package UI


import com.kapcode.open.macropad.kmps.WifiServer
import java.awt.BorderLayout
import javax.swing.*

class ConsoleUI(private val wifiServer: WifiServer) : JPanel() {

    private val consoleTextArea: JTextArea
    private val inputTextField: JTextField

    init {
        layout = BorderLayout()
        val theme = Theme()

        // Console output area
        consoleTextArea = JTextArea()
        consoleTextArea.isEditable = false
        val scrollPane = JScrollPane(consoleTextArea)
        add(scrollPane, BorderLayout.CENTER)

        // Input panel with text field and send button
        val inputPanel = JPanel(BorderLayout())
        inputTextField = JTextField()
        
        val sendIcon = SvgIconRenderer.getIcon("/email-mail-sent-icon.svg", 24, 24)
        val sendButton = if (sendIcon != null) {
            JButton(sendIcon)
        } else {
            JButton("Send")
        }
        sendButton.toolTipText = "Send data to connected device"
        sendButton.background = theme.PrimaryButtonColor // Set blue background
        sendButton.isOpaque = true // Ensure background is visible
        sendButton.isContentAreaFilled = true

        inputPanel.add(inputTextField, BorderLayout.CENTER)
        inputPanel.add(sendButton, BorderLayout.EAST)

        add(inputPanel, BorderLayout.SOUTH)

        // Add action listener for the send button
        sendButton.addActionListener { sendData() }

        // Add action listener for the Enter key in the text field
        inputTextField.addActionListener { sendData() }
    }

    private fun sendData() {
        val dataToSend = inputTextField.text
        if (dataToSend.isNotEmpty()) {
            // wifiServer.sendToAll(dataToSend) // This line is commented out as the method is unresolved
            appendMessage("Sent: $dataToSend")
            inputTextField.text = "" // Clear the input field
        }
    }

    fun appendMessage(message: String) {
        SwingUtilities.invokeLater { // Ensure thread safety when updating UI
            consoleTextArea.append("$message\n")
            // Auto-scroll to the bottom
            consoleTextArea.caretPosition = consoleTextArea.document.length
        }
    }
}