package UI

class MacroRunMacroItem(val macroName: String) : DraggableMacroItem() {

    init {
        setText(macroName, "RUN_MACRO")
    }
}