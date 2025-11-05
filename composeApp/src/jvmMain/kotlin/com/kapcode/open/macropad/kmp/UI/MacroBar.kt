package UI

import MacroRecorder
import org.json.JSONArray
import org.json.JSONObject
import java.awt.* 
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.beans.PropertyChangeListener
import javax.swing.*
import java.awt.image.BufferedImage
import java.awt.event.ActionListener

class MacroBar(private val frame: JFrame, private val tabbedUI: TabbedUI) : JPanel() {

    private val toolbar = ToolBarUI()
    private var dropLineX: Int? = null
    private val dropZoneHeight = 30
    private var isDragInProgress = false
    private val newTriggerButton: JButton
    private var macroRecorder: MacroRecorder? = null

    val macroItemsPanel = object : JPanel() {
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            if (isDragInProgress) {
                g.color = Theme().SecondaryBorderColor
                g.fillRect(0, 0, width, dropZoneHeight)
            }
            dropLineX?.let {
                g.color = Color.RED
                g.fillRect(it - 1, 0, 2, dropZoneHeight)
            }
        }
    }.apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        isOpaque = true
        background = Theme().SecondaryBackgroundColor
        border = BorderFactory.createEmptyBorder(dropZoneHeight, 0, 0, 0)
    }

    private val triggerSlot = JPanel(BorderLayout())
    private val transferHandler = MacroItemTransferHandler()

    init {
        val theme = Theme()
        layout = BorderLayout()
        background = theme.SecondaryBackgroundColor
        minimumSize = Dimension(0, 100)
        preferredSize = Dimension(0, 150)

        val newEventAction = ActionListener { 
            val selectedComponent = tabbedUI.selectedComponent
            if (selectedComponent is MacroJsonEditorUI) {
                val dialog = NewEventDialog(frame)
                dialog.isVisible = true
                dialog.createdEvent?.let { event ->
                    selectedComponent.insertNewEvent(event, dialog.isTriggerEvent, selectedComponent.hasTextFocus())
                }
            }
        }

        val newTriggerAction = ActionListener { 
            val selectedComponent = tabbedUI.selectedComponent
            if (selectedComponent is MacroJsonEditorUI) {
                val dialog = NewEventDialog(frame, isTriggerDefault = true)
                dialog.isVisible = true
                dialog.createdEvent?.let { event ->
                    selectedComponent.insertNewEvent(event, dialog.isTriggerEvent, selectedComponent.hasTextFocus())
                }
            }
        }

        triggerSlot.border = BorderFactory.createTitledBorder("Trigger")
        triggerSlot.transferHandler = transferHandler
        triggerSlot.preferredSize = Dimension(200, 0)
        triggerSlot.maximumSize = Dimension(200, Integer.MAX_VALUE)
        newTriggerButton = JButton("New Trigger")
        newTriggerButton.addActionListener(newTriggerAction)
        triggerSlot.add(newTriggerButton, BorderLayout.CENTER)
        add(triggerSlot, BorderLayout.WEST)

        val itemsPanelWithToolbar = JPanel(BorderLayout())
        
        val newEventIcon = SvgIconRenderer.getIcon("/add-file-icon.svg", 24, 24)
        if (newEventIcon != null) {
            toolbar.addButton(newEventIcon, "New Event", newEventAction)
        }

        val recordIcon = SvgIconRenderer.getIcon("/green-circle-shape-icon.svg", 24, 24)
        val stopIcon = SvgIconRenderer.getIcon("/red-circle-shape-icon.svg", 24, 24)
        val recordButton = if (recordIcon != null) {
            ToolBarButton(recordIcon, "Start Recording") {}
        } else {
            ToolBarButton("Record", "Start Recording") {}
        }
        var isRecording = false
        recordButton.addActionListener { 
            if (isRecording) {
                macroRecorder?.stop()
                isRecording = false
                if (recordIcon != null) recordButton.setIcon(recordIcon)
                recordButton.setToolTipText("Start Recording")
            } else {
                val settingsDialog = RecordingSettingsDialog(frame)
                settingsDialog.isVisible = true

                if (settingsDialog.shouldStartRecording) {
                    isRecording = true
                    if (stopIcon != null) recordButton.setIcon(stopIcon)
                    recordButton.setToolTipText("Stop Recording")
                    
                    macroRecorder = MacroRecorder(settingsDialog) { recordedEvents ->
                        SwingUtilities.invokeLater { 
                            val selectedEditor = tabbedUI.selectedComponent as? MacroJsonEditorUI
                            if (selectedEditor != null) {
                                val newEventsArray = JSONArray(recordedEvents)
                                val currentJson = JSONObject(selectedEditor.getText())
                                currentJson.put("events", newEventsArray)
                                selectedEditor.setText(currentJson.toString(4), selectedEditor.getCurrentFile())
                            }
                            println("Recording Stopped")
                            // Reset button state after recording is processed
                            isRecording = false
                            if (recordIcon != null) recordButton.setIcon(recordIcon)
                            recordButton.setToolTipText("Start Recording")
                        }
                    }
                    macroRecorder?.start()
                }
            }
        }
        toolbar.add(recordButton)

        val undoIcon = SvgIconRenderer.getIcon("/undo-circle-outline-icon.svg", 24, 24)
        if (undoIcon != null) {
            toolbar.addButton(undoIcon, "Undo last action") {}
        }

        val redoIcon = SvgIconRenderer.getIcon("/redo-circle-outline-icon.svg", 24, 24)
        if (redoIcon != null) {
            toolbar.addButton(redoIcon, "Redo last action") {}
        }

        itemsPanelWithToolbar.add(toolbar, BorderLayout.NORTH)

        val scrollPane = JScrollPane(macroItemsPanel).apply {
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_NEVER
            border = null
            horizontalScrollBar.unitIncrement = 20
        }
        
        val macroItemsContainer = JPanel(BorderLayout())
        macroItemsContainer.add(scrollPane, BorderLayout.CENTER)

        val addMacroItemButton = JButton(SvgIconRenderer.getIcon("/add-file-icon.svg", 24, 24))
        addMacroItemButton.toolTipText = "Add New Macro Event"
        addMacroItemButton.addActionListener(newEventAction)
        macroItemsContainer.add(addMacroItemButton, BorderLayout.EAST)

        itemsPanelWithToolbar.add(macroItemsContainer, BorderLayout.CENTER)
        add(itemsPanelWithToolbar, BorderLayout.CENTER)

        macroItemsPanel.transferHandler = transferHandler
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        super.addPropertyChangeListener(listener)
    }

    fun addMacroItem(item: MacroItem) {
        item.transferHandler = transferHandler
        macroItemsPanel.add(item)
    }

    fun setTriggerItem(item: MacroItem) {
        triggerSlot.removeAll()
        item.transferHandler = transferHandler
        triggerSlot.add(item, BorderLayout.CENTER)
        newTriggerButton.isVisible = false
        triggerSlot.revalidate()
        triggerSlot.repaint()
    }

    fun getTriggerItem(): MacroItem? {
        return if (triggerSlot.componentCount > 0 && triggerSlot.getComponent(0) is MacroItem) triggerSlot.getComponent(0) as? MacroItem else null
    }

    fun clear() {
        macroItemsPanel.removeAll()
        triggerSlot.removeAll()
        triggerSlot.add(newTriggerButton, BorderLayout.CENTER)
        newTriggerButton.isVisible = true
        revalidate()
        repaint()
    }

    fun notifyItemsReordered() {
        firePropertyChange("component.reordered", null, null)
    }

    inner class MacroItemTransferHandler : TransferHandler() {

        private val indexFlavor = DataFlavor(Int::class.java, "java/lang/Integer")
        private var sourceComponent: JComponent? = null

        private fun getTargetIndex(support: TransferSupport): Int {
            val dropPoint = support.dropLocation.dropPoint
            var targetIndex = 0
            for (i in 0 until macroItemsPanel.componentCount) {
                val component = macroItemsPanel.getComponent(i)
                if (dropPoint.x > component.x + component.width / 2) {
                    targetIndex = i + 1
                }
            }
            return targetIndex
        }

        override fun getSourceActions(c: JComponent): Int {
            sourceComponent = c
            isDragInProgress = true
            macroItemsPanel.repaint()
            return MOVE
        }

        override fun createTransferable(c: JComponent): Transferable? {
            val index = macroItemsPanel.components.indexOf(c)
            if (index != -1) {
                return object : Transferable {
                    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(indexFlavor)
                    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor.equals(indexFlavor)
                    override fun getTransferData(flavor: DataFlavor): Any = index
                }
            }
            return null
        }

        override fun getDragImage(): Image? {
            val c = sourceComponent ?: return null
            val image = BufferedImage(c.width, c.height, BufferedImage.TYPE_INT_ARGB)
            val g2 = image.createGraphics()
            c.paint(g2)
            g2.dispose()
            return image
        }

        override fun canImport(support: TransferSupport): Boolean {
            if (!support.isDrop || !support.isDataFlavorSupported(indexFlavor)) {
                return false
            }

            val dropPoint = support.dropLocation.dropPoint

            if (dropPoint.y > dropZoneHeight) {
                dropLineX = null
                macroItemsPanel.repaint()
                return false
            }

            val targetIndex = getTargetIndex(support)
            
            if (targetIndex < macroItemsPanel.componentCount) {
                dropLineX = macroItemsPanel.getComponent(targetIndex).x
            } else {
                if (macroItemsPanel.componentCount > 0) {
                    val lastComp = macroItemsPanel.getComponent(macroItemsPanel.componentCount - 1)
                    dropLineX = lastComp.x + lastComp.width
                } else {
                    dropLineX = 0
                }
            }

            macroItemsPanel.repaint()
            return true
        }

        override fun importData(support: TransferSupport): Boolean {
            if (!canImport(support)) {
                return false
            }

            val sourceIndex = try {
                support.transferable.getTransferData(indexFlavor) as Int
            } catch (e: Exception) {
                return false
            }

            val comp = macroItemsPanel.getComponent(sourceIndex) as? DraggableMacroItem ?: return false
            val targetIndex = getTargetIndex(support)

            if (sourceIndex == targetIndex) {
                return false
            }

            macroItemsPanel.remove(comp)
            val newIndex = if (sourceIndex < targetIndex) targetIndex - 1 else targetIndex
            macroItemsPanel.add(comp, newIndex)

            macroItemsPanel.revalidate()
            macroItemsPanel.repaint()
            notifyItemsReordered()
            return true
        }

        override fun exportDone(source: JComponent, data: Transferable, action: Int) {
            dropLineX = null
            isDragInProgress = false
            macroItemsPanel.repaint()
            sourceComponent = null
        }
    }
}