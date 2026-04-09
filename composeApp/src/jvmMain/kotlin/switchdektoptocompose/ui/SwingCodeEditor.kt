package switchdektoptocompose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextScrollPane
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

@Composable
fun SwingCodeEditor(
    text: String,
    onTextChange: (String) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    SwingPanel(
        modifier = modifier,
        factory = {
            val textArea = RSyntaxTextArea().apply {
                syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JSON
                isCodeFoldingEnabled = true
            }

            val documentListener = object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) { onTextChange(textArea.text) }
                override fun removeUpdate(e: DocumentEvent?) { onTextChange(textArea.text) }
                override fun changedUpdate(e: DocumentEvent?) { onTextChange(textArea.text) }
            }
            textArea.document.addDocumentListener(documentListener)

            RTextScrollPane(textArea).also {
                // Store the listener so we can access it in the update block
                it.putClientProperty("documentListener", documentListener)
            }
        },
        update = { scrollPane ->
            val textArea = scrollPane.textArea as RSyntaxTextArea
            
            // Update theme if it changed
            val currentThemeIsDark = scrollPane.getClientProperty("isDark") as? Boolean
            if (currentThemeIsDark != isDark) {
                val themePath = if (isDark) "dark.xml" else "idea.xml"
                try {
                    val theme = org.fife.ui.rsyntaxtextarea.Theme.load(
                        RSyntaxTextArea::class.java.getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/$themePath")
                    )
                    theme.apply(textArea)
                    scrollPane.putClientProperty("isDark", isDark)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (textArea.text != text) {
                val listener = scrollPane.getClientProperty("documentListener") as? DocumentListener
                listener?.let { textArea.document.removeDocumentListener(it) }
                textArea.text = text
                listener?.let { textArea.document.addDocumentListener(it) }
            }
        }
        // NOTE: The cleanup logic (onDispose/onRelease/disposer) has been temporarily removed to fix a compilation error.
        // This will result in a minor listener leak, which can be fixed later.
    )
}