package UI

import java.awt.Point

class MacroMouseItem(val commandType: MouseCommandType, val point: Point) : DraggableMacroItem() {

    enum class MouseCommandType {
        SNAP_TO,
        ANIMATE_TO,
        PRESS,
        RELEASE,
        CLICK,
        DRAG
    }

    init {
        // Use the parent's setText method to display the information.
        val commandText = "${commandType.name} (${point.x}, ${point.y})"
        setText("Mouse", commandText)
    }
}