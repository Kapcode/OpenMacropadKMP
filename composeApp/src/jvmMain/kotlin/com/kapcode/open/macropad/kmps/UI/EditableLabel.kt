package UI

import javax.swing.JLabel

class EditableLabel(initialText: String) : JLabel(initialText) {
    // This class will now be a simple JLabel.
    // The editing logic will be handled by its parent, TabTitle.
}