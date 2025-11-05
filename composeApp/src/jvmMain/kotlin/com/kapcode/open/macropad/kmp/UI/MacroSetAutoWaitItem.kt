package UI

class MacroSetAutoWaitItem(val waitValue: Int) : DraggableMacroItem() {

    init {
        setText("SET_AUTO_WAIT", "$waitValue ms")
    }
}