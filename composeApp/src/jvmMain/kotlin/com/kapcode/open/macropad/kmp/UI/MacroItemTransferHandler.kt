package UI

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.TransferHandler
import javax.swing.SwingUtilities

class MacroItemTransferHandler(private val onDropSuccess: () -> Unit) : TransferHandler() {

    private val macroItemFlavor = DataFlavor(MacroItem::class.java, "Macro Item")

    override fun getSourceActions(c: JComponent?): Int {
        return MOVE
    }

    override fun createTransferable(c: JComponent?): Transferable? {
        return if (c is MacroItem) {
            object : Transferable {
                override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(macroItemFlavor)
                override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean = flavor == macroItemFlavor
                override fun getTransferData(flavor: DataFlavor?): Any = if (isDataFlavorSupported(flavor)) c else throw UnsupportedFlavorException(flavor)
            }
        } else {
            null
        }
    }

    override fun canImport(support: TransferSupport?): Boolean {
        return support?.isDataFlavorSupported(macroItemFlavor) == true
    }

    override fun importData(support: TransferSupport?): Boolean {
        if (!canImport(support) || support == null) {
            return false
        }

        val dropTargetComponent = support.component

        // Ensure we are always operating on the parent container (macroItemsPanel).
        // If the dropTargetComponent is a MacroItem, its parent is the container.
        // Otherwise, the dropTargetComponent itself is the container.
        val container = if (dropTargetComponent is MacroItem) {
            dropTargetComponent.parent as? JPanel
        } else {
            dropTargetComponent as? JPanel
        } ?: return false

        val transferable = support.transferable

        val draggedItem = try {
            transferable.getTransferData(macroItemFlavor) as MacroItem
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        // Adjust dropPoint to be relative to the container if it was originally relative to a child.
        val dropPoint = support.dropLocation.dropPoint
        val adjustedDropPoint = if (dropTargetComponent is MacroItem) {
            SwingUtilities.convertPoint(dropTargetComponent, dropPoint.x, dropPoint.y, container)
        } else {
            dropPoint
        }

        var targetIndex = container.componentCount // Default to end

        // Calculate the target index for insertion in a BoxLayout (X_AXIS)
        for (i in 0 until container.componentCount) {
            val component = container.getComponent(i)
            // If the adjusted drop point is before the middle of this component, insert here
            if (adjustedDropPoint.x < component.x + component.width / 2) {
                targetIndex = i
                break
            }
        }

        val sourceIndex = container.components.indexOf(draggedItem)

        // If the item is being dropped at its current position or just after it, do nothing.
        if (sourceIndex == targetIndex || (sourceIndex != -1 && targetIndex == sourceIndex + 1)) {
            return false
        }

        // Remove the item from its original position
        if (sourceIndex != -1) {
            container.remove(draggedItem)
            // Adjust targetIndex if removing an item from before the target
            if (sourceIndex < targetIndex) {
                targetIndex--
            }
        }

        // Add the item at the new target index
        container.add(draggedItem, targetIndex)

        container.revalidate()
        container.repaint()

        // Invoke the callback on successful drop
        onDropSuccess()

        return true
    }
}